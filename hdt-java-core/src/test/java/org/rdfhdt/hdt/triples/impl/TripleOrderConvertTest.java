package org.rdfhdt.hdt.triples.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.triples.TripleID;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

@RunWith(Suite.class)
@Suite.SuiteClasses({
		TripleOrderConvertTest.PairwiseTests.class,
		TripleOrderConvertTest.OtherTests.class
})
public class TripleOrderConvertTest {

	private static final long S = 100, P = 200, O = 300;

	private static final TripleComponentOrder[] ORDERS = {
			TripleComponentOrder.SPO, TripleComponentOrder.SOP,
			TripleComponentOrder.PSO, TripleComponentOrder.POS,
			TripleComponentOrder.OSP, TripleComponentOrder.OPS};

	/**
	 * Build a TripleID whose three long slots are laid out per {@code order}.
	 * For every order, the S-term is {@link #S}, P-term is {@link #P}, O-term is {@link #O}.
	 */
	private static TripleID build(TripleComponentOrder order) {
		switch (order) {
			case SPO: return new TripleID(S, P, O);
			case SOP: return new TripleID(S, O, P);
			case PSO: return new TripleID(P, S, O);
			case POS: return new TripleID(P, O, S);
			case OSP: return new TripleID(O, S, P);
			case OPS: return new TripleID(O, P, S);
			default: throw new IllegalArgumentException();
		}
	}

	private static void assertLayout(TripleComponentOrder order, TripleID t) {
		TripleID expected = build(order);
		assertEquals("slot0 @ " + order, expected.getSubject(), t.getSubject());
		assertEquals("slot1 @ " + order, expected.getPredicate(), t.getPredicate());
		assertEquals("slot2 @ " + order, expected.getObject(), t.getObject());
	}

	/**
	 * One test case per (from, to) pair — 36 cases total. Each runs as its own JUnit
	 * test so a regression in any single cell of the swap table surfaces by name.
	 */
	@RunWith(Parameterized.class)
	public static class PairwiseTests {

		@Parameterized.Parameters(name = "{0} -> {1}")
		public static Collection<Object[]> pairs() {
			Collection<Object[]> cases = new ArrayList<>(ORDERS.length * ORDERS.length);
			for (TripleComponentOrder from : ORDERS) {
				for (TripleComponentOrder to : ORDERS) {
					cases.add(new Object[] {from, to});
				}
			}
			return cases;
		}

		private final TripleComponentOrder from;
		private final TripleComponentOrder to;

		public PairwiseTests(TripleComponentOrder from, TripleComponentOrder to) {
			this.from = from;
			this.to = to;
		}

		@Test
		public void producesCorrectTargetLayout() {
			TripleID t = build(from);
			TripleOrderConvert.swapComponentOrder(t, from, to);
			assertLayout(to, t);
		}

		@Test
		public void roundTripRestoresOriginal() {
			TripleID t = build(from);
			TripleOrderConvert.swapComponentOrder(t, from, to);
			TripleOrderConvert.swapComponentOrder(t, to, from);
			assertLayout(from, t);
		}
	}

	public static class OtherTests {

		@Test
		public void identityIsNoOp() {
			for (TripleComponentOrder order : ORDERS) {
				TripleID t = build(order);
				TripleOrderConvert.swapComponentOrder(t, order, order);
				assertLayout(order, t);
			}
		}

		@Test
		public void unknownFromThrows() {
			TripleID t = new TripleID(1, 2, 3);
			assertThrows(IllegalArgumentException.class,
					() -> TripleOrderConvert.swapComponentOrder(
							t, TripleComponentOrder.Unknown, TripleComponentOrder.SPO));
		}

		@Test
		public void unknownToThrows() {
			TripleID t = new TripleID(1, 2, 3);
			assertThrows(IllegalArgumentException.class,
					() -> TripleOrderConvert.swapComponentOrder(
							t, TripleComponentOrder.SPO, TripleComponentOrder.Unknown));
		}

		@Test
		public void spoToOpsIsSubjectObjectSwap() {
			TripleID t = new TripleID(1, 2, 3);
			TripleOrderConvert.swapComponentOrder(t, TripleComponentOrder.SPO, TripleComponentOrder.OPS);
			assertEquals(3, t.getSubject());
			assertEquals(2, t.getPredicate());
			assertEquals(1, t.getObject());
		}

		@Test
		public void spoToOspMovesSubjectToObjectAndObjectToSubjectAndPredicateToPredicate() {
			TripleID t = new TripleID(1, 2, 3);
			TripleOrderConvert.swapComponentOrder(t, TripleComponentOrder.SPO, TripleComponentOrder.OSP);
			assertEquals(3, t.getSubject());
			assertEquals(1, t.getPredicate());
			assertEquals(2, t.getObject());
		}
	}
}
