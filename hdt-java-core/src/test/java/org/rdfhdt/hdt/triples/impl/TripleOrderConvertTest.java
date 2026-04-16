package org.rdfhdt.hdt.triples.impl;

import org.junit.Test;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.triples.TripleID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TripleOrderConvertTest {

	private static final long S = 100, P = 200, O = 300;

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

	private static TripleComponentOrder[] orders() {
		return new TripleComponentOrder[] {
				TripleComponentOrder.SPO, TripleComponentOrder.SOP,
				TripleComponentOrder.PSO, TripleComponentOrder.POS,
				TripleComponentOrder.OSP, TripleComponentOrder.OPS};
	}

	/**
	 * The permutation table must be correct for every (from, to) pair: starting from a
	 * triple laid out as {@code from}, after swap the slots should match {@code to}'s layout.
	 */
	@Test
	public void allPairsProduceCorrectTargetLayout() {
		for (TripleComponentOrder from : orders()) {
			for (TripleComponentOrder to : orders()) {
				TripleID t = build(from);
				TripleOrderConvert.swapComponentOrder(t, from, to);
				assertLayout(to, t);
			}
		}
	}

	@Test
	public void roundTripRestoresOriginal() {
		for (TripleComponentOrder from : orders()) {
			for (TripleComponentOrder to : orders()) {
				TripleID t = build(from);
				TripleOrderConvert.swapComponentOrder(t, from, to);
				TripleOrderConvert.swapComponentOrder(t, to, from);
				assertLayout(from, t);
			}
		}
	}

	@Test
	public void identityIsNoOp() {
		for (TripleComponentOrder order : orders()) {
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
