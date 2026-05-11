package org.rdfhdt.hdt.triples.impl;

import org.junit.Test;
import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.enums.TripleComponentOrder;

import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

public class EmptyTriplesIteratorTest {

	@Test
	public void iteratorHasNoElements() {
		EmptyTriplesIterator it = new EmptyTriplesIterator(TripleComponentOrder.SPO);
		assertFalse(it.hasNext());
		assertFalse(it.hasPrevious());
		assertThrows(NoSuchElementException.class, it::next);
		assertThrows(NoSuchElementException.class, it::previous);
	}

	@Test
	public void positioningIsUnsupported() {
		EmptyTriplesIterator it = new EmptyTriplesIterator(TripleComponentOrder.SPO);
		assertFalse(it.canGoTo());
		assertThrows(UnsupportedOperationException.class, () -> it.goTo(0));
		assertThrows(NoSuchElementException.class, it::getLastTriplePosition);
		assertThrows(UnsupportedOperationException.class, it::remove);
		// goToStart is a documented no-op
		it.goToStart();
	}

	@Test
	public void reportsExactZeroResults() {
		EmptyTriplesIterator it = new EmptyTriplesIterator(TripleComponentOrder.POS);
		assertEquals(0, it.estimatedNumResults());
		assertEquals(ResultEstimationType.EXACT, it.numResultEstimation());
	}

	@Test
	public void preservesOrderGivenAtConstruction() {
		for (TripleComponentOrder order : TripleComponentOrder.values()) {
			assertEquals(order, new EmptyTriplesIterator(order).getOrder());
		}
	}
}
