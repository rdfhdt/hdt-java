package org.rdfhdt.hdt.triples.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.hdt.writer.TripleWriterHDT;
import org.rdfhdt.hdt.iterator.SuppliableIteratorTripleID;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleString;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Edge-case tests for the BitmapTriples iterator family: backwards navigation,
 * {@code goTo}/{@code canGoTo} dispatch, boundary errors, and non-indexed iterator
 * paths (Y/Z rather than YFOQ/ZFOQ).
 */
public class BitmapTriplesIteratorEdgeTest {

	private static final String BASE = "http://ex.org/";
	private static final int SUBJECTS = 3;
	private static final int PREDICATES = 2;
	private static final int OBJECTS = 3;

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	private File hdtFile;
	private HDT hdtIndexed;
	private HDT hdtUnindexed;

	@Before
	public void buildHdt() throws IOException {
		hdtFile = tempDir.newFile("t.hdt");
		try (TripleWriterHDT wr = new TripleWriterHDT(BASE, new HDTSpecification(),
				hdtFile.getAbsolutePath(), false)) {
			for (int si = 1; si <= SUBJECTS; si++) {
				for (int pi = 1; pi <= PREDICATES; pi++) {
					for (int oi = 1; oi <= OBJECTS; oi++) {
						wr.addTriple(new TripleString(
								BASE + "S" + si,
								BASE + "P" + pi,
								BASE + "O" + oi));
					}
				}
			}
		}
		hdtIndexed = HDTManager.loadIndexedHDT(hdtFile.getAbsolutePath(), null, new HDTSpecification());
		hdtUnindexed = HDTManager.loadHDT(hdtFile.getAbsolutePath(), null, new HDTSpecification());
	}

	@After
	public void close() throws IOException {
		if (hdtIndexed != null) hdtIndexed.close();
		if (hdtUnindexed != null) hdtUnindexed.close();
	}

	private BitmapTriples indexed() {
		return (BitmapTriples) hdtIndexed.getTriples();
	}

	private BitmapTriples unindexed() {
		return (BitmapTriples) hdtUnindexed.getTriples();
	}

	private static List<TripleID> collect(SuppliableIteratorTripleID it) {
		List<TripleID> out = new ArrayList<>();
		while (it.hasNext()) out.add(new TripleID(it.next()));
		return out;
	}

	// -------- BitmapTriplesIterator (???, S??, SP?, SPO) --------

	@Test
	public void fullScanIteratorSupportsGoTo() {
		SuppliableIteratorTripleID it = indexed().search(new TripleID(0, 0, 0));
		assertTrue(it.canGoTo());
		// jump past the first bucket, consume the rest
		it.goTo(3);
		assertTrue(it.hasNext());
		TripleID t = it.next();
		assertNotNull(t);
	}

	@Test
	public void fullScanIteratorGoToBeyondLastThrows() {
		SuppliableIteratorTripleID it = indexed().search(new TripleID(0, 0, 0));
		long total = indexed().getNumberOfElements();
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> it.goTo(total));
	}

	@Test
	public void nonWildcardPatternCannotGoTo() {
		SuppliableIteratorTripleID it = indexed().search(new TripleID(1, 0, 0));
		assertFalse(it.canGoTo());
		assertThrows(IllegalAccessError.class, () -> it.goTo(0));
	}

	@Test
	public void fullScanBackwardsWalkReturnsSameTriples() {
		SuppliableIteratorTripleID it = indexed().search(new TripleID(0, 0, 0));
		List<TripleID> forwards = collect(it);

		it.goToStart();
		// advance to the end, then walk back
		while (it.hasNext()) it.next();
		List<TripleID> backwards = new ArrayList<>();
		while (it.hasPrevious()) backwards.add(new TripleID(it.previous()));

		assertEquals(forwards.size(), backwards.size());
		for (int i = 0; i < forwards.size(); i++) {
			assertEquals(forwards.get(forwards.size() - 1 - i), backwards.get(i));
		}
	}

	@Test
	public void fullScanRemoveIsUnsupported() {
		SuppliableIteratorTripleID it = indexed().search(new TripleID(0, 0, 0));
		assertThrows(UnsupportedOperationException.class, it::remove);
	}

	@Test
	public void fullScanEstimateIsExact() {
		SuppliableIteratorTripleID it = indexed().search(new TripleID(0, 0, 0));
		assertEquals(ResultEstimationType.EXACT, it.numResultEstimation());
		assertEquals(indexed().getNumberOfElements(), it.estimatedNumResults());
	}

	@Test
	public void bySubjectAndObjectEstimateIsUpTo() {
		// S?O pattern → numResultEstimation is UP_TO
		SuppliableIteratorTripleID it = indexed().search(new TripleID(1, 0, 1));
		assertEquals(ResultEstimationType.UP_TO, it.numResultEstimation());
	}

	@Test
	public void bySubjectGetLastTriplePositionIsMonotonic() {
		SuppliableIteratorTripleID it = indexed().search(new TripleID(1, 0, 0));
		long prev = -1;
		while (it.hasNext()) {
			it.next();
			long p = it.getLastTriplePosition();
			assertTrue("positions must be monotonic, was " + prev + " now " + p, p > prev);
			prev = p;
		}
	}

	// -------- BitmapTriplesIteratorY (?P? on non-indexed HDT) --------

	@Test
	public void yIteratorByPredicateReturnsAllMatches() {
		SuppliableIteratorTripleID it = unindexed().search(new TripleID(0, 1, 0));
		// This should be a BitmapTriplesIteratorY.
		assertEquals(BitmapTriplesIteratorY.class, it.getClass());
		List<TripleID> triples = collect(it);
		assertEquals(SUBJECTS * OBJECTS, triples.size());
	}

	@Test
	public void yIteratorCannotGoTo() {
		SuppliableIteratorTripleID it = unindexed().search(new TripleID(0, 1, 0));
		assertFalse(it.canGoTo());
		assertThrows(IllegalAccessError.class, () -> it.goTo(0));
	}

	@Test
	public void yIteratorRemoveIsUnsupported() {
		SuppliableIteratorTripleID it = unindexed().search(new TripleID(0, 1, 0));
		assertThrows(UnsupportedOperationException.class, it::remove);
	}

	@Test
	public void yIteratorHasUnknownResultEstimation() {
		SuppliableIteratorTripleID it = unindexed().search(new TripleID(0, 1, 0));
		assertEquals(ResultEstimationType.UNKNOWN, it.numResultEstimation());
	}

	@Test
	public void yIteratorGoToStartRestartsWalk() {
		SuppliableIteratorTripleID it = unindexed().search(new TripleID(0, 1, 0));
		TripleID first = new TripleID(it.next());
		while (it.hasNext()) it.next();
		it.goToStart();
		assertTrue(it.hasNext());
		assertEquals(first, it.next());
	}

	@Test
	public void yIteratorBackwardsWalkYieldsForwardsReversed() {
		SuppliableIteratorTripleID it = unindexed().search(new TripleID(0, 1, 0));
		List<TripleID> forwards = collect(it);

		it.goToStart();
		while (it.hasNext()) it.next();
		List<TripleID> backwards = new ArrayList<>();
		while (it.hasPrevious()) backwards.add(new TripleID(it.previous()));

		assertEquals(forwards.size(), backwards.size());
		for (int i = 0; i < forwards.size(); i++) {
			assertEquals(forwards.get(forwards.size() - 1 - i), backwards.get(i));
		}
	}

	@Test
	public void yIteratorHasLastTriplePositionSupplier() {
		SuppliableIteratorTripleID it = unindexed().search(new TripleID(0, 1, 0));
		it.next();
		assertNotNull(it.getLastTriplePositionSupplier());
		assertTrue(it.getLastTriplePositionSupplier().compute() >= 0);
	}

	@Test
	public void yIteratorRejectsZeroPredicate() {
		// The Y iterator requires a fixed predicate; pattern with patY == 0 is illegal
		assertThrows(IllegalArgumentException.class,
				() -> new BitmapTriplesIteratorY(unindexed(), new TripleID(0, 0, 0)));
	}

	// -------- BitmapTriplesIteratorYFOQ (?P? on indexed HDT) --------

	@Test
	public void yfoqIteratorByPredicateReturnsAllMatches() {
		SuppliableIteratorTripleID it = indexed().search(new TripleID(0, 1, 0));
		assertEquals(BitmapTriplesIteratorYFOQ.class, it.getClass());
		assertEquals(SUBJECTS * OBJECTS, collect(it).size());
	}

	@Test
	public void yfoqIteratorCannotGoTo() {
		SuppliableIteratorTripleID it = indexed().search(new TripleID(0, 1, 0));
		assertFalse(it.canGoTo());
		assertThrows(IllegalAccessError.class, () -> it.goTo(0));
	}

	@Test
	public void yfoqIteratorRejectsZeroPredicate() {
		assertThrows(IllegalArgumentException.class,
				() -> new BitmapTriplesIteratorYFOQ(indexed(), new TripleID(0, 0, 0)));
	}

	@Test
	public void yfoqIteratorEstimateMatchesPredicateCount() {
		SuppliableIteratorTripleID it = indexed().search(new TripleID(0, 1, 0));
		assertTrue(it.estimatedNumResults() > 0);
	}

	@Test
	public void yfoqIteratorBackwardsWalkYieldsForwardsReversed() {
		SuppliableIteratorTripleID it = indexed().search(new TripleID(0, 1, 0));
		List<TripleID> forwards = collect(it);

		it.goToStart();
		while (it.hasNext()) it.next();
		List<TripleID> backwards = new ArrayList<>();
		while (it.hasPrevious()) backwards.add(new TripleID(it.previous()));

		assertEquals(forwards.size(), backwards.size());
		for (int i = 0; i < forwards.size(); i++) {
			assertEquals(forwards.get(forwards.size() - 1 - i), backwards.get(i));
		}
	}

	// -------- BitmapTriplesIteratorZ (??O on non-indexed HDT) --------

	@Test
	public void zIteratorByObjectReturnsAllMatches() {
		SuppliableIteratorTripleID it = unindexed().search(new TripleID(0, 0, 1));
		assertEquals(BitmapTriplesIteratorZ.class, it.getClass());
		// Every (s, p, o=1) triple should appear
		assertTrue(collect(it).size() > 0);
	}

	@Test
	public void zIteratorCannotGoTo() {
		SuppliableIteratorTripleID it = unindexed().search(new TripleID(0, 0, 1));
		assertFalse(it.canGoTo());
		assertThrows(IllegalAccessError.class, () -> it.goTo(0));
	}

	@Test
	public void zIteratorRemoveIsUnsupported() {
		SuppliableIteratorTripleID it = unindexed().search(new TripleID(0, 0, 1));
		assertThrows(UnsupportedOperationException.class, it::remove);
	}

	// -------- BitmapTriplesIteratorZFOQ (??O on indexed HDT) --------

	@Test
	public void zfoqIteratorByObjectReturnsAllMatches() {
		SuppliableIteratorTripleID it = indexed().search(new TripleID(0, 0, 1));
		assertEquals(BitmapTriplesIteratorZFOQ.class, it.getClass());
		assertTrue(collect(it).size() > 0);
	}

	@Test
	public void zfoqIteratorByPredicateAndObjectFiltersCorrectly() {
		// ?PO pattern under indexed mode → ZFOQ
		SuppliableIteratorTripleID it = indexed().search(new TripleID(0, 1, 1));
		assertEquals(BitmapTriplesIteratorZFOQ.class, it.getClass());
		List<TripleID> triples = collect(it);
		for (TripleID t : triples) {
			assertEquals(1L, t.getPredicate());
			assertEquals(1L, t.getObject());
		}
	}

	@Test
	public void zfoqIteratorCanGoToSupportsRandomAccess() {
		SuppliableIteratorTripleID it = indexed().search(new TripleID(0, 0, 1));
		assertTrue(it.canGoTo());
		// Random-access to position 0 should not throw.
		it.goTo(0);
		assertTrue(it.hasNext());
	}

	@Test
	public void zfoqIteratorGoToOutOfBoundsThrows() {
		SuppliableIteratorTripleID it = indexed().search(new TripleID(0, 0, 1));
		assertThrows(IndexOutOfBoundsException.class, () -> it.goTo(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> it.goTo(Long.MAX_VALUE));
	}

	@Test
	public void zfoqIteratorBackwardsWalk() {
		SuppliableIteratorTripleID it = indexed().search(new TripleID(0, 0, 1));
		int forwards = collect(it).size();
		it.goToStart();
		while (it.hasNext()) it.next();
		int backwards = 0;
		while (it.hasPrevious()) {
			it.previous();
			backwards++;
		}
		assertEquals(forwards, backwards);
	}

	@Test
	public void zfoqIteratorGoToStartAllowsRewalk() {
		SuppliableIteratorTripleID it = indexed().search(new TripleID(0, 0, 1));
		int first = collect(it).size();
		it.goToStart();
		int second = collect(it).size();
		assertEquals(first, second);
	}
}
