package org.rdfhdt.hdt.triples.impl;

import org.junit.Test;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TempTriples;
import org.rdfhdt.hdt.triples.TripleID;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class OneReadTempTriplesTest {

	private static OneReadTempTriples of(TripleComponentOrder order, TripleID... triples) {
		List<TripleID> list = Arrays.asList(triples);
		return new OneReadTempTriples(list.iterator(), order, list.size());
	}

	@Test
	public void getOrderReturnsOrderFromConstructor() {
		for (TripleComponentOrder order : TripleComponentOrder.values()) {
			assertEquals(order, of(order).getOrder());
		}
	}

	@Test
	public void getNumberOfElementsAndSizeReflectTripleCount() {
		OneReadTempTriples t = of(TripleComponentOrder.SPO,
				new TripleID(1, 2, 3), new TripleID(4, 5, 6));
		assertEquals(2, t.getNumberOfElements());
		assertEquals(2, t.size());
	}

	@Test
	public void searchAllReturnsNoDuplicateIterator() {
		OneReadTempTriples t = of(TripleComponentOrder.SPO,
				new TripleID(1, 2, 3), new TripleID(1, 2, 3), new TripleID(4, 5, 6));
		IteratorTripleID it = t.searchAll();
		assertTrue(it.hasNext());
		assertEquals(new TripleID(1, 2, 3), it.next());
		// Duplicate (1,2,3) should be filtered by NoDuplicateTripleIDIterator
		assertTrue(it.hasNext());
		assertEquals(new TripleID(4, 5, 6), it.next());
		assertFalse(it.hasNext());
	}

	@Test
	public void sortIsNoOp() {
		// documented to be already sorted
		of(TripleComponentOrder.SPO).sort(null);
	}

	@Test
	public void setOrderWithSameOrderSucceeds() {
		of(TripleComponentOrder.OPS).setOrder(TripleComponentOrder.OPS);
	}

	@Test
	public void setOrderWithDifferentOrderThrows() {
		OneReadTempTriples t = of(TripleComponentOrder.SPO);
		assertThrows(IllegalArgumentException.class, () -> t.setOrder(TripleComponentOrder.OPS));
	}

	@Test
	public void loadFromAnotherOneReadTempTriplesCopiesIteratorAndOrder() {
		OneReadTempTriples src = of(TripleComponentOrder.OPS,
				new TripleID(1, 2, 3), new TripleID(4, 5, 6));
		OneReadTempTriples dst = of(TripleComponentOrder.SPO); // different order on purpose
		dst.load((TempTriples) src, null);
		assertEquals(TripleComponentOrder.OPS, dst.getOrder());

		IteratorTripleID it = dst.searchAll();
		assertEquals(new TripleID(1, 2, 3), it.next());
		assertEquals(new TripleID(4, 5, 6), it.next());
	}

	@Test
	public void loadFromUnrelatedTempTriplesThrows() {
		OneReadTempTriples dst = of(TripleComponentOrder.SPO);
		TempTriples other = new TriplesList(new org.rdfhdt.hdt.options.HDTSpecification());
		assertThrows(NotImplementedException.class, () -> dst.load(other, null));
	}

	@Test
	public void closeIsANoOp() throws Exception {
		of(TripleComponentOrder.SPO).close();
	}

	@Test
	public void unsupportedMutatorsThrowNotImplemented() {
		OneReadTempTriples t = of(TripleComponentOrder.SPO);
		assertThrows(NotImplementedException.class, () -> t.insert(1L, 2L, 3L));
		assertThrows(NotImplementedException.class, () -> t.insert(new TripleID(1, 2, 3)));
		assertThrows(NotImplementedException.class, () -> t.remove(new TripleID(1, 2, 3)));
		assertThrows(NotImplementedException.class, () -> t.removeDuplicates(null));
		assertThrows(NotImplementedException.class, t::clear);
		assertThrows(NotImplementedException.class, () -> t.load((org.rdfhdt.hdt.triples.Triples) null, null));
		assertThrows(NotImplementedException.class, () -> t.replaceAllIds(null, null, null));
	}

	@Test
	public void unsupportedIoMethodsThrowNotImplemented() {
		OneReadTempTriples t = of(TripleComponentOrder.SPO);
		assertThrows(NotImplementedException.class, () -> t.save(null, null, null));
		assertThrows(NotImplementedException.class, () -> t.load(new java.io.ByteArrayInputStream(new byte[0]), null, null));
		assertThrows(NotImplementedException.class, () -> t.mapFromFile(null, null, null));
		assertThrows(NotImplementedException.class, () -> t.generateIndex(null, null, null));
		assertThrows(NotImplementedException.class, () -> t.loadIndex(null, null, null));
		assertThrows(NotImplementedException.class, () -> t.mapIndex(null, null, null, null));
		assertThrows(NotImplementedException.class, () -> t.saveIndex(null, null, null));
	}

	@Test
	public void unsupportedQueryMethodsThrowNotImplemented() {
		OneReadTempTriples t = of(TripleComponentOrder.SPO);
		assertThrows(NotImplementedException.class, () -> t.search(new TripleID(0, 0, 0)));
		assertThrows(NotImplementedException.class, () -> t.populateHeader(null, ""));
		assertThrows(NotImplementedException.class, t::getType);
		assertThrows(NotImplementedException.class, () -> t.findTriple(0));
	}

	// --- SimpleIteratorTripleID (inner class, reached via searchAll's underlying iterator) ---

	@Test
	public void simpleIteratorReportsOrderAndEstimates() throws Exception {
		OneReadTempTriples t = of(TripleComponentOrder.POS,
				new TripleID(1, 2, 3), new TripleID(4, 5, 6));
		// Reach SimpleIteratorTripleID via reflection on the inner "iterator" field
		java.lang.reflect.Field f = OneReadTempTriples.class.getDeclaredField("iterator");
		f.setAccessible(true);
		IteratorTripleID inner = (IteratorTripleID) f.get(t);

		assertEquals(TripleComponentOrder.POS, inner.getOrder());
		assertEquals(2, inner.estimatedNumResults());
		assertEquals(org.rdfhdt.hdt.enums.ResultEstimationType.UP_TO, inner.numResultEstimation());
		assertEquals(2, inner.getLastTriplePosition());
		assertTrue(inner.hasNext());
		assertEquals(new TripleID(1, 2, 3), inner.next());
	}

	@Test
	public void simpleIteratorUnsupportedMethodsThrow() throws Exception {
		OneReadTempTriples t = of(TripleComponentOrder.SPO);
		java.lang.reflect.Field f = OneReadTempTriples.class.getDeclaredField("iterator");
		f.setAccessible(true);
		IteratorTripleID inner = (IteratorTripleID) f.get(t);

		assertThrows(NotImplementedException.class, inner::hasPrevious);
		assertThrows(NotImplementedException.class, inner::previous);
		assertThrows(NotImplementedException.class, inner::goToStart);
		assertThrows(NotImplementedException.class, inner::canGoTo);
		assertThrows(NotImplementedException.class, () -> inner.goTo(0));
	}

	private static OneReadTempTriples of(TripleComponentOrder order) {
		return of(order, new TripleID[0]);
	}

	// Also exercises: Iterator<TripleID> arg to constructor comes straight through via searchAll.
	@Test
	public void constructorAcceptsAnyTripleIDIterator() {
		Iterator<TripleID> src = Arrays.asList(new TripleID(7, 8, 9)).iterator();
		OneReadTempTriples t = new OneReadTempTriples(src, TripleComponentOrder.SPO, 1);
		IteratorTripleID it = t.searchAll();
		assertTrue(it.hasNext());
		assertEquals(new TripleID(7, 8, 9), it.next());
		assertFalse(it.hasNext());
	}
}
