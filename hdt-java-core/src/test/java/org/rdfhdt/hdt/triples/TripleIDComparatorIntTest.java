package org.rdfhdt.hdt.triples;

import org.junit.Test;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.triples.impl.TripleIDInt;

import java.util.Comparator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TripleIDComparatorIntTest {

	private static TripleIDInt t(int s, int p, int o) {
		return new TripleIDInt(s, p, o);
	}

	private static int sign(int x) {
		return Integer.compare(x, 0);
	}

	private static TripleComponentOrder[] nonUnknownOrders() {
		return new TripleComponentOrder[] {
				TripleComponentOrder.SPO, TripleComponentOrder.SOP,
				TripleComponentOrder.PSO, TripleComponentOrder.POS,
				TripleComponentOrder.OSP, TripleComponentOrder.OPS};
	}

	@Test
	public void factoryReturnsNonNullForEveryOrder() {
		for (TripleComponentOrder order : nonUnknownOrders()) {
			assertNotNull(TripleIDComparatorInt.getComparator(order));
		}
	}

	@Test
	public void equalTriplesReturnZeroInEveryOrder() {
		for (TripleComponentOrder order : nonUnknownOrders()) {
			Comparator<TripleIDInt> c = TripleIDComparatorInt.getComparator(order);
			assertEquals("order=" + order, 0, c.compare(t(7, 8, 9), t(7, 8, 9)));
		}
	}

	@Test
	public void compareIsAntisymmetric() {
		TripleIDInt a = t(1, 2, 3);
		TripleIDInt b = t(3, 2, 1);
		for (TripleComponentOrder order : nonUnknownOrders()) {
			Comparator<TripleIDInt> c = TripleIDComparatorInt.getComparator(order);
			assertEquals("order=" + order,
					-sign(c.compare(a, b)), sign(c.compare(b, a)));
		}
	}

	@Test
	public void spoPrimarySubjectSecondaryPredicateTertiaryObject() {
		Comparator<TripleIDInt> c = TripleIDComparatorInt.getComparator(TripleComponentOrder.SPO);
		assertTrue(c.compare(t(1, 9, 9), t(2, 0, 0)) < 0);
		assertTrue(c.compare(t(1, 1, 9), t(1, 2, 0)) < 0);
		assertTrue(c.compare(t(1, 1, 1), t(1, 1, 2)) < 0);
	}

	@Test
	public void sopPrimarySubjectSecondaryObjectTertiaryPredicate() {
		Comparator<TripleIDInt> c = TripleIDComparatorInt.getComparator(TripleComponentOrder.SOP);
		assertTrue(c.compare(t(1, 9, 9), t(2, 0, 0)) < 0);
		assertTrue(c.compare(t(1, 9, 1), t(1, 0, 2)) < 0);
		assertTrue(c.compare(t(1, 1, 1), t(1, 2, 1)) < 0);
	}

	@Test
	public void psoPrimaryPredicateSecondarySubjectTertiaryObject() {
		Comparator<TripleIDInt> c = TripleIDComparatorInt.getComparator(TripleComponentOrder.PSO);
		assertTrue(c.compare(t(9, 1, 9), t(0, 2, 0)) < 0);
		assertTrue(c.compare(t(1, 1, 9), t(2, 1, 0)) < 0);
		assertTrue(c.compare(t(1, 1, 1), t(1, 1, 2)) < 0);
	}

	@Test
	public void posPrimaryPredicateSecondaryObjectTertiarySubject() {
		Comparator<TripleIDInt> c = TripleIDComparatorInt.getComparator(TripleComponentOrder.POS);
		assertTrue(c.compare(t(9, 1, 9), t(0, 2, 0)) < 0);
		assertTrue(c.compare(t(9, 1, 1), t(0, 1, 2)) < 0);
		assertTrue(c.compare(t(1, 1, 1), t(2, 1, 1)) < 0);
	}

	@Test
	public void ospPrimaryObjectSecondarySubjectTertiaryPredicate() {
		Comparator<TripleIDInt> c = TripleIDComparatorInt.getComparator(TripleComponentOrder.OSP);
		assertTrue(c.compare(t(9, 9, 1), t(0, 0, 2)) < 0);
		assertTrue(c.compare(t(1, 9, 1), t(2, 0, 1)) < 0);
		assertTrue(c.compare(t(1, 1, 1), t(1, 2, 1)) < 0);
	}

	@Test
	public void opsPrimaryObjectSecondaryPredicateTertiarySubject() {
		Comparator<TripleIDInt> c = TripleIDComparatorInt.getComparator(TripleComponentOrder.OPS);
		assertTrue(c.compare(t(9, 9, 1), t(0, 0, 2)) < 0);
		assertTrue(c.compare(t(9, 1, 1), t(0, 2, 1)) < 0);
		assertTrue(c.compare(t(1, 1, 1), t(2, 1, 1)) < 0);
	}
}
