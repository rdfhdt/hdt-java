package org.rdfhdt.hdt.triples.impl;

import org.junit.Test;
import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.header.PlainHeader;
import org.rdfhdt.hdt.iterator.SuppliableIteratorTripleID;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.options.HDTOptionsKeys;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TriplesListLongTest {

	private static TriplesListLong newList() {
		return new TriplesListLong(new HDTSpecification());
	}

	private static TriplesListLong listOf(TripleID... triples) {
		TriplesListLong list = newList();
		for (TripleID t : triples) {
			list.insert(t.getSubject(), t.getPredicate(), t.getObject());
		}
		return list;
	}

	@Test
	public void emptyListHasDefaultOrderAndZeroElements() {
		TriplesListLong list = newList();
		assertEquals(0, list.getNumberOfElements());
		assertEquals(0, list.size());
		assertEquals(TripleComponentOrder.SPO, list.getOrder());
	}

	@Test
	public void constructorReadsOrderFromSpec() {
		HDTSpecification spec = new HDTSpecification();
		spec.set(HDTOptionsKeys.TRIPLE_ORDER_KEY, "OPS");
		assertEquals(TripleComponentOrder.OPS, new TriplesListLong(spec).getOrder());
	}

	@Test
	public void insertVarargsAddsAllTriples() {
		TriplesListLong list = newList();
		list.insert(new TripleID(1, 2, 3), new TripleID(4, 5, 6));
		assertEquals(2, list.getNumberOfElements());
	}

	@Test
	public void insertLongsAddsOneTriple() {
		TriplesListLong list = newList();
		list.insert(1L, 2L, 3L);
		assertEquals(1, list.getNumberOfElements());
	}

	@Test
	public void searchAllReturnsEveryTripleInOrder() {
		TriplesListLong list = listOf(new TripleID(1, 1, 1), new TripleID(2, 2, 2));
		IteratorTripleID it = list.searchAll();
		assertTrue(it.hasNext());
		assertEquals(new TripleID(1, 1, 1), it.next());
		assertEquals(new TripleID(2, 2, 2), it.next());
		assertFalse(it.hasNext());
	}

	@Test
	public void searchWithSpecificPatternFiltersResults() {
		TriplesListLong list = listOf(
				new TripleID(1, 2, 3),
				new TripleID(1, 2, 4),
				new TripleID(2, 2, 3));
		SuppliableIteratorTripleID it = list.search(new TripleID(1, 0, 0));
		int count = 0;
		while (it.hasNext()) {
			assertEquals(1L, it.next().getSubject());
			count++;
		}
		assertEquals(2, count);
	}

	@Test
	public void sortOrdersBySpoAndMakesRemoveDuplicatesEffective() {
		TriplesListLong list = listOf(
				new TripleID(3, 1, 1),
				new TripleID(1, 1, 1),
				new TripleID(1, 1, 1),
				new TripleID(2, 1, 1));
		list.sort(null);
		list.removeDuplicates(null);
		assertEquals(3, list.getNumberOfElements());
	}

	@Test
	public void removeDuplicatesIsNoOpWithoutSort() {
		TriplesListLong list = listOf(new TripleID(1, 1, 1), new TripleID(1, 1, 1));
		list.removeDuplicates(null);
		assertEquals(2, list.getNumberOfElements());
	}

	@Test
	public void removeClearsMatchingTriples() {
		TriplesListLong list = listOf(new TripleID(1, 2, 3), new TripleID(4, 5, 6));
		assertTrue(list.remove(new TripleID(1, 0, 0)));
		assertEquals(1, list.getNumberOfElements());
	}

	@Test
	public void removeReturnsFalseWhenNothingMatches() {
		TriplesListLong list = listOf(new TripleID(1, 2, 3));
		assertFalse(list.remove(new TripleID(99, 0, 0)));
	}

	@Test
	public void setOrderUpdatesOrder() {
		TriplesListLong list = newList();
		list.setOrder(TripleComponentOrder.OPS);
		assertEquals(TripleComponentOrder.OPS, list.getOrder());
	}

	@Test
	public void setOrderToSameOrderIsNoOp() {
		TriplesListLong list = newList();
		list.setOrder(TripleComponentOrder.SPO);
		assertEquals(TripleComponentOrder.SPO, list.getOrder());
	}

	@Test
	public void loadFromOtherTempTriplesCopiesAllTriples() {
		TriplesListLong src = listOf(new TripleID(1, 2, 3), new TripleID(4, 5, 6));
		TriplesListLong dst = newList();
		dst.load((org.rdfhdt.hdt.triples.TempTriples) src, null);
		assertEquals(2, dst.getNumberOfElements());
	}

	@Test
	public void loadFromTriplesClearsAndCopies() {
		TriplesListLong src = listOf(new TripleID(9, 9, 9));
		TriplesListLong dst = listOf(new TripleID(1, 1, 1));
		dst.load((org.rdfhdt.hdt.triples.Triples) src, null);
		assertEquals(1, dst.getNumberOfElements());
		assertEquals(new TripleID(9, 9, 9), dst.searchAll().next());
	}

	@Test
	public void clearResetsStateToEmpty() {
		TriplesListLong list = listOf(new TripleID(1, 1, 1));
		list.clear();
		assertEquals(0, list.getNumberOfElements());
		assertEquals(TripleComponentOrder.Unknown, list.getOrder());
	}

	@Test
	public void getTypeReturnsTripleslistVocabulary() {
		assertEquals(HDTVocabulary.TRIPLES_TYPE_TRIPLESLIST, newList().getType());
	}

	@Test
	public void findTripleReturnsTripleAtPosition() {
		TriplesListLong list = listOf(new TripleID(1, 2, 3), new TripleID(4, 5, 6));
		assertEquals(new TripleID(1, 2, 3), list.findTriple(0));
		assertEquals(new TripleID(4, 5, 6), list.findTriple(1));
	}

	@Test
	public void populateHeaderInsertsExpectedTriples() {
		TriplesListLong list = listOf(new TripleID(1, 1, 1));
		PlainHeader header = new PlainHeader();
		list.populateHeader(header, "_:root");
		assertTrue(header.getNumberOfElements() >= 3);
	}

	@Test
	public void reallocateIfEmptyReturnsTrueOnFreshList() {
		assertTrue(newList().reallocateIfEmpty(50));
	}

	@Test
	public void reallocateIfEmptyReturnsFalseWhenPopulated() {
		assertFalse(listOf(new TripleID(1, 1, 1)).reallocateIfEmpty(50));
	}

	@Test
	public void toStringIncludesOrder() {
		assertTrue(newList().toString().contains("SPO"));
	}

	@Test
	public void mapFromFileThrowsNotImplemented() {
		assertThrows(NotImplementedException.class,
				() -> newList().mapFromFile(null, null, null));
	}

	@Test
	public void closeIsANoOp() throws Exception {
		newList().close();
	}

	// --- Iterator ---

	@Test
	public void iteratorBidirectionalNavigation() {
		TriplesListLong list = listOf(new TripleID(1, 1, 1), new TripleID(2, 2, 2));
		SuppliableIteratorTripleID it = list.search(new TripleID(0, 0, 0));
		assertFalse(it.hasPrevious());
		it.next();
		it.next();
		assertTrue(it.hasPrevious());
		assertEquals(new TripleID(2, 2, 2), it.previous());
	}

	@Test
	public void iteratorGoToAndGoToStart() {
		TriplesListLong list = listOf(
				new TripleID(1, 1, 1), new TripleID(2, 2, 2), new TripleID(3, 3, 3));
		SuppliableIteratorTripleID it = list.search(new TripleID(0, 0, 0));
		assertTrue(it.canGoTo());
		it.goTo(2);
		assertEquals(new TripleID(3, 3, 3), it.next());
		it.goToStart();
		assertEquals(new TripleID(1, 1, 1), it.next());
	}

	@Test
	public void iteratorReportsExactEstimate() {
		TriplesListLong list = listOf(new TripleID(1, 1, 1), new TripleID(2, 2, 2));
		SuppliableIteratorTripleID it = list.search(new TripleID(0, 0, 0));
		assertEquals(2, it.estimatedNumResults());
		assertEquals(ResultEstimationType.EXACT, it.numResultEstimation());
		assertEquals(TripleComponentOrder.SPO, it.getOrder());
	}

	@Test
	public void iteratorTracksLastTriplePosition() {
		TriplesListLong list = listOf(new TripleID(1, 1, 1), new TripleID(2, 2, 2));
		SuppliableIteratorTripleID it = list.search(new TripleID(0, 0, 0));
		it.next();
		assertEquals(0, it.getLastTriplePosition());
		it.next();
		assertEquals(1, it.getLastTriplePosition());
	}

	@Test
	public void iteratorRemoveIsUnsupported() {
		SuppliableIteratorTripleID it = listOf(new TripleID(1, 1, 1))
				.search(new TripleID(0, 0, 0));
		assertThrows(UnsupportedOperationException.class, it::remove);
	}

	@Test
	public void saveLoadRoundTripPreservesTriples() throws Exception {
		TriplesListLong src = listOf(new TripleID(1L, 2L, 3L), new TripleID(4L, 5L, 6L));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		src.save(out, new ControlInformation(), null);

		TriplesListLong dst = newList();
		ControlInformation ci = new ControlInformation();
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		ci.load(in);
		dst.load(in, ci, null);

		assertEquals(2, dst.getNumberOfElements());
		IteratorTripleID it = dst.searchAll();
		assertEquals(new TripleID(1L, 2L, 3L), it.next());
		assertEquals(new TripleID(4L, 5L, 6L), it.next());
	}
}
