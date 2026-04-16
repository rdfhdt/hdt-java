package org.rdfhdt.hdt.triples;

import org.junit.Test;
import org.rdfhdt.hdt.enums.TripleComponentOrder;

import java.util.Comparator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TripleIDComparatorTest {

	private static TripleID t(long s, long p, long o) {
		return new TripleID(s, p, o);
	}

	private static int sign(int x) {
		return Integer.compare(x, 0);
	}

	@Test
	public void spoFactoryReturnsSingleton() {
		Comparator<TripleID> c = TripleIDComparator.getComparator(TripleComponentOrder.SPO);
		assertNotNull(c);
		assertSame(c, TripleIDComparator.getComparator(TripleComponentOrder.SPO));
	}

	@Test
	public void nonSpoFactoryReturnsComparator() {
		for (TripleComponentOrder order : new TripleComponentOrder[] {
				TripleComponentOrder.SOP, TripleComponentOrder.PSO,
				TripleComponentOrder.POS, TripleComponentOrder.OSP,
				TripleComponentOrder.OPS}) {
			assertNotNull(TripleIDComparator.getComparator(order));
		}
	}

	@Test
	public void unknownOrderThrows() {
		TripleIDComparator c = new TripleIDComparator(TripleComponentOrder.Unknown);
		assertThrows(IllegalArgumentException.class,
				() -> c.compare(t(1, 2, 3), t(4, 5, 6)));
	}

	@Test
	public void equalTriplesReturnZeroInEveryOrder() {
		for (TripleComponentOrder order : nonUnknownOrders()) {
			Comparator<TripleID> c = new TripleIDComparator(order);
			assertEquals("order=" + order, 0, c.compare(t(7, 8, 9), t(7, 8, 9)));
		}
	}

	@Test
	public void compareIsAntisymmetric() {
		TripleID a = t(1, 2, 3);
		TripleID b = t(3, 2, 1);
		for (TripleComponentOrder order : nonUnknownOrders()) {
			Comparator<TripleID> c = new TripleIDComparator(order);
			assertEquals("order=" + order,
					-sign(c.compare(a, b)), sign(c.compare(b, a)));
		}
	}

	@Test
	public void spoPrimarySubjectSecondaryPredicateTertiaryObject() {
		Comparator<TripleID> c = new TripleIDComparator(TripleComponentOrder.SPO);
		assertTrue(c.compare(t(1, 9, 9), t(2, 0, 0)) < 0);
		assertTrue(c.compare(t(1, 1, 9), t(1, 2, 0)) < 0);
		assertTrue(c.compare(t(1, 1, 1), t(1, 1, 2)) < 0);
	}

	@Test
	public void sopPrimarySubjectSecondaryObjectTertiaryPredicate() {
		Comparator<TripleID> c = new TripleIDComparator(TripleComponentOrder.SOP);
		assertTrue(c.compare(t(1, 9, 9), t(2, 0, 0)) < 0);
		assertTrue(c.compare(t(1, 9, 1), t(1, 0, 2)) < 0);
		assertTrue(c.compare(t(1, 1, 1), t(1, 2, 1)) < 0);
	}

	@Test
	public void psoPrimaryPredicateSecondarySubjectTertiaryObject() {
		Comparator<TripleID> c = new TripleIDComparator(TripleComponentOrder.PSO);
		assertTrue(c.compare(t(9, 1, 9), t(0, 2, 0)) < 0);
		assertTrue(c.compare(t(1, 1, 9), t(2, 1, 0)) < 0);
		assertTrue(c.compare(t(1, 1, 1), t(1, 1, 2)) < 0);
	}

	@Test
	public void posPrimaryPredicateSecondaryObjectTertiarySubject() {
		Comparator<TripleID> c = new TripleIDComparator(TripleComponentOrder.POS);
		assertTrue(c.compare(t(9, 1, 9), t(0, 2, 0)) < 0);
		assertTrue(c.compare(t(9, 1, 1), t(0, 1, 2)) < 0);
		assertTrue(c.compare(t(1, 1, 1), t(2, 1, 1)) < 0);
	}

	@Test
	public void ospPrimaryObjectSecondarySubjectTertiaryPredicate() {
		Comparator<TripleID> c = new TripleIDComparator(TripleComponentOrder.OSP);
		assertTrue(c.compare(t(9, 9, 1), t(0, 0, 2)) < 0);
		assertTrue(c.compare(t(1, 9, 1), t(2, 0, 1)) < 0);
		assertTrue(c.compare(t(1, 1, 1), t(1, 2, 1)) < 0);
	}

	@Test
	public void opsPrimaryObjectSecondaryPredicateTertiarySubject() {
		Comparator<TripleID> c = new TripleIDComparator(TripleComponentOrder.OPS);
		assertTrue(c.compare(t(9, 9, 1), t(0, 0, 2)) < 0);
		assertTrue(c.compare(t(9, 1, 1), t(0, 2, 1)) < 0);
		assertTrue(c.compare(t(1, 1, 1), t(2, 1, 1)) < 0);
	}

	private static TripleComponentOrder[] nonUnknownOrders() {
		return new TripleComponentOrder[] {
				TripleComponentOrder.SPO, TripleComponentOrder.SOP,
				TripleComponentOrder.PSO, TripleComponentOrder.POS,
				TripleComponentOrder.OSP, TripleComponentOrder.OPS};
	}
}
