package org.rdfhdt.hdt.triples.impl;

import org.junit.Test;
import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.header.PlainHeader;
import org.rdfhdt.hdt.iterator.SuppliableIteratorTripleID;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTOptionsKeys;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TempTriples;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.Triples;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Shared tests for the two in-memory TempTriples implementations
 * ({@link TriplesList} and {@link TriplesListLong}). Subclasses provide the
 * concrete factory.
 */
public abstract class AbstractTriplesListTest<L extends TempTriples> {

	protected abstract L newList(HDTOptions spec);

	protected abstract boolean reallocateIfEmpty(L list, int n);

	protected L newList() {
		return newList(new HDTSpecification());
	}

	protected L listOf(TripleID... triples) {
		L list = newList();
		for (TripleID t : triples) {
			list.insert(t.getSubject(), t.getPredicate(), t.getObject());
		}
		return list;
	}

	@Test
	public void emptyListHasDefaultOrderAndZeroElements() {
		L list = newList();
		assertEquals(0, list.getNumberOfElements());
		assertEquals(TripleComponentOrder.SPO, list.getOrder());
	}

	@Test
	public void constructorReadsOrderFromSpec() {
		HDTSpecification spec = new HDTSpecification();
		spec.set(HDTOptionsKeys.TRIPLE_ORDER_KEY, "OPS");
		assertEquals(TripleComponentOrder.OPS, newList(spec).getOrder());
	}

	@Test
	public void insertVarargsAddsAllTriples() {
		L list = newList();
		list.insert(new TripleID(1, 2, 3), new TripleID(4, 5, 6));
		assertEquals(2, list.getNumberOfElements());
	}

	@Test
	public void insertLongsAddsOneTriple() {
		L list = newList();
		list.insert(1L, 2L, 3L);
		assertEquals(1, list.getNumberOfElements());
	}

	@Test
	public void searchAllReturnsEveryTripleInOrder() {
		L list = listOf(new TripleID(1, 1, 1), new TripleID(2, 2, 2));
		IteratorTripleID it = list.searchAll();
		assertTrue(it.hasNext());
		assertEquals(new TripleID(1, 1, 1), it.next());
		assertEquals(new TripleID(2, 2, 2), it.next());
		assertFalse(it.hasNext());
	}

	@Test
	public void searchWithWildcardPatternReturnsAll() {
		L list = listOf(new TripleID(1, 2, 3));
		SuppliableIteratorTripleID it = list.search(new TripleID(0, 0, 0));
		assertTrue(it.hasNext());
		assertEquals(new TripleID(1, 2, 3), it.next());
	}

	@Test
	public void searchWithSpecificPatternFiltersResults() {
		L list = listOf(
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
		L list = listOf(
				new TripleID(3, 1, 1),
				new TripleID(1, 1, 1),
				new TripleID(1, 1, 1),
				new TripleID(2, 1, 1));
		list.sort(null);
		list.removeDuplicates(null);
		assertEquals(3, list.getNumberOfElements());
		IteratorTripleID it = list.searchAll();
		assertEquals(1L, it.next().getSubject());
		assertEquals(2L, it.next().getSubject());
		assertEquals(3L, it.next().getSubject());
	}

	@Test
	public void removeDuplicatesIsNoOpWithoutSort() {
		L list = listOf(new TripleID(1, 1, 1), new TripleID(1, 1, 1));
		list.removeDuplicates(null);
		assertEquals(2, list.getNumberOfElements());
	}

	@Test
	public void removeDuplicatesIsNoOpOnSingletonList() {
		L list = listOf(new TripleID(1, 1, 1));
		list.sort(null);
		list.removeDuplicates(null);
		assertEquals(1, list.getNumberOfElements());
	}

	@Test
	public void removeClearsMatchingTriples() {
		L list = listOf(new TripleID(1, 2, 3), new TripleID(4, 5, 6));
		assertTrue(list.remove(new TripleID(1, 0, 0)));
		assertEquals(1, list.getNumberOfElements());
	}

	@Test
	public void removeReturnsFalseWhenNothingMatches() {
		L list = listOf(new TripleID(1, 2, 3));
		assertFalse(list.remove(new TripleID(99, 0, 0)));
	}

	@Test
	public void setOrderUpdatesOrder() {
		L list = newList();
		list.setOrder(TripleComponentOrder.OPS);
		assertEquals(TripleComponentOrder.OPS, list.getOrder());
	}

	@Test
	public void setOrderToSameOrderIsNoOp() {
		L list = newList();
		list.setOrder(TripleComponentOrder.SPO);
		assertEquals(TripleComponentOrder.SPO, list.getOrder());
	}

	@Test
	public void saveLoadRoundTripPreservesTriples() throws Exception {
		L src = listOf(new TripleID(1, 2, 3), new TripleID(4, 5, 6));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		src.save(out, new ControlInformation(), null);

		L dst = newList();
		ControlInformation ci = new ControlInformation();
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		ci.load(in);
		dst.load(in, ci, null);

		assertEquals(2, dst.getNumberOfElements());
		IteratorTripleID it = dst.searchAll();
		assertEquals(new TripleID(1, 2, 3), it.next());
		assertEquals(new TripleID(4, 5, 6), it.next());
	}

	@Test
	public void loadFromOtherTempTriplesCopiesAllTriples() {
		L src = listOf(new TripleID(1, 2, 3), new TripleID(4, 5, 6));
		L dst = newList();
		dst.load((TempTriples) src, null);
		assertEquals(2, dst.getNumberOfElements());
	}

	@Test
	public void loadFromTriplesClearsAndCopies() {
		L src = listOf(new TripleID(9, 9, 9));
		L dst = listOf(new TripleID(1, 1, 1));
		dst.load((Triples) src, null);
		assertEquals(1, dst.getNumberOfElements());
		assertEquals(new TripleID(9, 9, 9), dst.searchAll().next());
	}

	@Test
	public void clearResetsStateToEmpty() {
		L list = listOf(new TripleID(1, 1, 1));
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
		L list = listOf(new TripleID(1, 2, 3), new TripleID(4, 5, 6));
		assertEquals(new TripleID(1, 2, 3), list.findTriple(0));
		assertEquals(new TripleID(4, 5, 6), list.findTriple(1));
	}

	@Test
	public void populateHeaderInsertsExpectedTriples() {
		L list = listOf(new TripleID(1, 1, 1));
		PlainHeader header = new PlainHeader();
		list.populateHeader(header, "_:root");
		assertTrue(header.getNumberOfElements() >= 3);
	}

	@Test
	public void reallocateIfEmptyReturnsTrueOnFreshList() {
		assertTrue(reallocateIfEmpty(newList(), 50));
	}

	@Test
	public void reallocateIfEmptyReturnsFalseWhenPopulated() {
		assertFalse(reallocateIfEmpty(listOf(new TripleID(1, 1, 1)), 50));
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
		L list = listOf(new TripleID(1, 1, 1), new TripleID(2, 2, 2), new TripleID(3, 3, 3));
		SuppliableIteratorTripleID it = list.search(new TripleID(0, 0, 0));
		assertFalse(it.hasPrevious());
		it.next();
		it.next();
		assertTrue(it.hasPrevious());
		assertEquals(new TripleID(2, 2, 2), it.previous());
	}

	@Test
	public void iteratorGoToAndGoToStart() {
		L list = listOf(new TripleID(1, 1, 1), new TripleID(2, 2, 2), new TripleID(3, 3, 3));
		SuppliableIteratorTripleID it = list.search(new TripleID(0, 0, 0));
		assertTrue(it.canGoTo());
		it.goTo(2);
		assertEquals(new TripleID(3, 3, 3), it.next());
		it.goToStart();
		assertEquals(new TripleID(1, 1, 1), it.next());
	}

	@Test
	public void iteratorReportsExactEstimate() {
		L list = listOf(new TripleID(1, 1, 1), new TripleID(2, 2, 2));
		SuppliableIteratorTripleID it = list.search(new TripleID(0, 0, 0));
		assertEquals(2, it.estimatedNumResults());
		assertEquals(ResultEstimationType.EXACT, it.numResultEstimation());
		assertEquals(TripleComponentOrder.SPO, it.getOrder());
	}

	@Test
	public void iteratorTracksLastTriplePosition() {
		L list = listOf(new TripleID(1, 1, 1), new TripleID(2, 2, 2));
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
}
