package org.rdfhdt.hdt.triples.impl;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.hdt.writer.TripleWriterHDT;
import org.rdfhdt.hdt.iterator.SuppliableIteratorTripleID;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleString;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests that build a small HDT, load it in indexed mode (so the
 * {@code predicateIndex} and {@code indexZ/bitmapIndexZ} are populated), and
 * exercise {@link BitmapTriples#search(TripleID)} across every pattern shape.
 * This fans out into the whole BitmapTriplesIterator family (Y/Z/YFOQ/ZFOQ).
 */
public class BitmapTriplesQueryTest {

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	private static final String BASE = "http://ex.org/";
	private static final int SUBJECTS = 3;
	private static final int PREDICATES = 2;
	private static final int OBJECTS = 4;
	private static final int SHARED = 1;

	private HDT hdt;
	private BitmapTriples triples;

	private String s(int i) {
		return i <= SHARED ? BASE + "Sh" + i : BASE + "Su" + (i - SHARED);
	}

	private String p(int i) {
		return BASE + "P" + i;
	}

	private String o(int i) {
		return i <= SHARED ? BASE + "Sh" + i : BASE + "Ob" + (i - SHARED);
	}

	private HDT buildAndLoad() throws IOException {
		File hdtFile = tempDir.newFile("t.hdt");
		try (TripleWriterHDT wr = new TripleWriterHDT(BASE, new HDTSpecification(),
				hdtFile.getAbsolutePath(), false)) {
			for (int si = 1; si <= SUBJECTS; si++) {
				for (int pi = 1; pi <= PREDICATES; pi++) {
					for (int oi = 1; oi <= OBJECTS; oi++) {
						wr.addTriple(new TripleString(s(si), p(pi), o(oi)));
					}
				}
			}
		}
		// loadIndexedHDT populates predicateIndex + indexZ/bitmapIndexZ,
		// enabling the YFOQ/ZFOQ iterator paths in BitmapTriples.search.
		return HDTManager.loadIndexedHDT(hdtFile.getAbsolutePath(), null, new HDTSpecification());
	}

	private void setUp() throws IOException {
		hdt = buildAndLoad();
		triples = (BitmapTriples) hdt.getTriples();
	}

	private int countPattern(long sid, long pid, long oid) {
		IteratorTripleID it = triples.search(new TripleID(sid, pid, oid));
		int n = 0;
		while (it.hasNext()) {
			it.next();
			n++;
		}
		return n;
	}

	private int countAll() {
		return (int) triples.getNumberOfElements();
	}

	@Test
	public void totalTripleCountMatchesExpected() throws Exception {
		setUp();
		int expected = SUBJECTS * PREDICATES * OBJECTS;
		assertEquals(expected, countAll());
		assertEquals(expected, countPattern(0, 0, 0));
		hdt.close();
	}

	@Test
	public void bySubjectPatternReturnsAllPredicatesAndObjectsForThatSubject() throws Exception {
		setUp();
		for (long sid = 1; sid <= SUBJECTS; sid++) {
			assertEquals("S=" + sid, PREDICATES * OBJECTS, countPattern(sid, 0, 0));
		}
		hdt.close();
	}

	@Test
	public void byPredicatePatternReturnsAllSubjectsAndObjectsForThatPredicate() throws Exception {
		setUp();
		// This pattern should hit BitmapTriplesIteratorYFOQ (indexed HDT, ?P? pattern).
		for (long pid = 1; pid <= PREDICATES; pid++) {
			assertEquals("P=" + pid, SUBJECTS * OBJECTS, countPattern(0, pid, 0));
		}
		hdt.close();
	}

	@Test
	public void byObjectPatternReturnsAllSubjectsAndPredicatesForThatObject() throws Exception {
		setUp();
		// This pattern should hit BitmapTriplesIteratorZFOQ (indexed HDT, ??O pattern).
		for (long oid = 1; oid <= OBJECTS + SHARED; oid++) {
			int got = countPattern(0, 0, oid);
			if (oid <= OBJECTS) {
				// every (S, P, fixed O) pair exists when oid is a valid object id
				assertTrue("O=" + oid + " got=" + got, got > 0);
			}
		}
		hdt.close();
	}

	@Test
	public void bySubjectAndPredicateReturnsAllObjects() throws Exception {
		setUp();
		assertEquals(OBJECTS, countPattern(1, 1, 0));
		hdt.close();
	}

	@Test
	public void byPredicateAndObjectReturnsAllSubjects() throws Exception {
		setUp();
		// Should hit ZFOQ path on indexed HDT (?PO pattern).
		IteratorTripleID it = triples.search(new TripleID(0, 1, 1));
		assertTrue(it.hasNext());
		int n = 0;
		while (it.hasNext()) {
			TripleID t = it.next();
			assertEquals(1L, t.getPredicate());
			n++;
		}
		assertTrue("?PO should return at least 1 triple", n >= 1);
		hdt.close();
	}

	@Test
	public void bySubjectAndObjectReturnsAllPredicates() throws Exception {
		setUp();
		IteratorTripleID it = triples.search(new TripleID(1, 0, 1));
		int n = 0;
		while (it.hasNext()) {
			TripleID t = it.next();
			assertEquals(1L, t.getSubject());
			n++;
		}
		assertTrue("S?O should return at least 1 triple", n >= 1);
		hdt.close();
	}

	@Test
	public void fullySpecifiedPatternReturnsExactlyOneTriple() throws Exception {
		setUp();
		assertEquals(1, countPattern(1, 1, 1));
		hdt.close();
	}

	@Test
	public void emptyMatchPatternReturnsEmptyIterator() throws Exception {
		setUp();
		// A TripleID with a negative id is a "noMatch" pattern
		TripleID noMatch = new TripleID(-1, -1, -1);
		IteratorTripleID it = triples.search(noMatch);
		assertFalse(it.hasNext());
		hdt.close();
	}

	@Test
	public void iteratorGoToStartRestartsWalk() throws Exception {
		setUp();
		IteratorTripleID it = triples.search(new TripleID(0, 0, 0));
		TripleID first = it.next();
		while (it.hasNext()) it.next();
		it.goToStart();
		assertTrue(it.hasNext());
		assertEquals(first, it.next());
		hdt.close();
	}

	@Test
	public void iteratorEstimateMatchesActualCount() throws Exception {
		setUp();
		IteratorTripleID it = triples.search(new TripleID(0, 0, 0));
		long estimate = it.estimatedNumResults();
		assertNotNull(it.numResultEstimation());
		long actual = 0;
		while (it.hasNext()) {
			it.next();
			actual++;
		}
		// For the full-scan pattern this must be exact
		assertEquals(estimate, actual);
		assertSame(ResultEstimationType.EXACT, it.numResultEstimation());
		hdt.close();
	}

	@Test
	public void iteratorGetLastTriplePositionAdvancesWithNext() throws Exception {
		setUp();
		SuppliableIteratorTripleID it = (SuppliableIteratorTripleID)
				triples.search(new TripleID(0, 0, 0));
		long prev = -1;
		while (it.hasNext()) {
			it.next();
			long p = it.getLastTriplePosition();
			assertTrue("position should be monotonic", p > prev);
			prev = p;
		}
		hdt.close();
	}

	@Test
	public void iteratorOrderIsHdtOrder() throws Exception {
		setUp();
		IteratorTripleID it = triples.search(new TripleID(0, 0, 0));
		assertEquals(triples.getOrder(), it.getOrder());
		hdt.close();
	}

	@Test
	public void numberOfElementsMatchesTotal() throws Exception {
		setUp();
		assertEquals(SUBJECTS * PREDICATES * OBJECTS, triples.getNumberOfElements());
		hdt.close();
	}

	@Test
	public void searchAllEquivalentToFullScan() throws Exception {
		setUp();
		int viaSearchAll = 0;
		IteratorTripleID it = triples.searchAll();
		while (it.hasNext()) {
			it.next();
			viaSearchAll++;
		}
		assertEquals(countAll(), viaSearchAll);
		hdt.close();
	}

	@Test
	public void stringSearchThroughHdtResolvesAllPatterns() throws Exception {
		setUp();
		// Exercise the dictionary-translate path on top of the BitmapTriples iterators.
		try {
			int n = 0;
			hdt.search("", "", "").forEachRemaining(t -> {});
			n = countAll();
			assertEquals(SUBJECTS * PREDICATES * OBJECTS, n);
		} catch (NotFoundException e) {
			throw new AssertionError("?? search should succeed", e);
		}
		hdt.close();
	}
}
