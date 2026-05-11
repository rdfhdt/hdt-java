package org.rdfhdt.hdt.triples.impl;

import org.junit.Test;
import org.rdfhdt.hdt.triples.TripleID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TripleIDIntTest {

	@Test
	public void defaultConstructorIsAllZero() {
		TripleIDInt t = new TripleIDInt();
		assertEquals(0, t.getSubject());
		assertEquals(0, t.getPredicate());
		assertEquals(0, t.getObject());
		assertTrue(t.isEmpty());
		assertFalse(t.isValid());
	}

	@Test
	public void intConstructorStoresComponents() {
		TripleIDInt t = new TripleIDInt(1, 2, 3);
		assertEquals(1, t.getSubject());
		assertEquals(2, t.getPredicate());
		assertEquals(3, t.getObject());
	}

	@Test
	public void longConstructorNarrowsToInt() {
		TripleIDInt t = new TripleIDInt(10L, 20L, 30L);
		assertEquals(10, t.getSubject());
		assertEquals(20, t.getPredicate());
		assertEquals(30, t.getObject());
	}

	@Test
	public void copyConstructorCopiesComponents() {
		TripleIDInt src = new TripleIDInt(1, 2, 3);
		TripleIDInt dst = new TripleIDInt(src);
		assertTrue(dst.equals(src));
		// independence
		src.setSubject(99);
		assertEquals(1, dst.getSubject());
	}

	@Test
	public void tripleIdConstructorNarrowsLongsToInt() {
		TripleIDInt t = new TripleIDInt(new TripleID(4L, 5L, 6L));
		assertEquals(4, t.getSubject());
		assertEquals(5, t.getPredicate());
		assertEquals(6, t.getObject());
	}

	@Test
	public void settersUpdateComponents() {
		TripleIDInt t = new TripleIDInt();
		t.setSubject(1);
		t.setPredicate(2);
		t.setObject(3);
		assertEquals(1, t.getSubject());
		assertEquals(2, t.getPredicate());
		assertEquals(3, t.getObject());
	}

	@Test
	public void setAllReplacesAllComponents() {
		TripleIDInt t = new TripleIDInt(9, 9, 9);
		t.setAll(1, 2, 3);
		assertEquals(1, t.getSubject());
		assertEquals(2, t.getPredicate());
		assertEquals(3, t.getObject());
	}

	@Test
	public void assignCopiesFromAnother() {
		TripleIDInt dst = new TripleIDInt();
		dst.assign(new TripleIDInt(7, 8, 9));
		assertEquals(7, dst.getSubject());
		assertEquals(8, dst.getPredicate());
		assertEquals(9, dst.getObject());
	}

	@Test
	public void clearZeroesAllComponents() {
		TripleIDInt t = new TripleIDInt(1, 2, 3);
		t.clear();
		assertTrue(t.isEmpty());
		assertEquals(0, t.getSubject());
	}

	@Test
	public void equalsComparesAllThreeComponents() {
		TripleIDInt a = new TripleIDInt(1, 2, 3);
		assertTrue(a.equals(new TripleIDInt(1, 2, 3)));
		assertFalse(a.equals(new TripleIDInt(9, 2, 3)));
		assertFalse(a.equals(new TripleIDInt(1, 9, 3)));
		assertFalse(a.equals(new TripleIDInt(1, 2, 9)));
	}

	@Test
	public void compareToUsesSpoOrder() {
		TripleIDInt a = new TripleIDInt(1, 1, 1);
		assertTrue(a.compareTo(new TripleIDInt(2, 0, 0)) < 0);
		assertTrue(a.compareTo(new TripleIDInt(1, 2, 0)) < 0);
		assertTrue(a.compareTo(new TripleIDInt(1, 1, 2)) < 0);
		assertEquals(0, a.compareTo(new TripleIDInt(1, 1, 1)));
	}

	@Test
	public void matchTreatsZeroAsWildcard() {
		TripleIDInt t = new TripleIDInt(1, 2, 3);
		assertTrue(t.match(new TripleID(0, 0, 0)));
		assertTrue(t.match(new TripleID(1, 0, 0)));
		assertTrue(t.match(new TripleID(0, 2, 0)));
		assertTrue(t.match(new TripleID(0, 0, 3)));
		assertTrue(t.match(new TripleID(1, 2, 3)));
		assertFalse(t.match(new TripleID(9, 0, 0)));
		assertFalse(t.match(new TripleID(1, 9, 0)));
		assertFalse(t.match(new TripleID(1, 2, 9)));
	}

	@Test
	public void isValidRequiresAllPositive() {
		assertTrue(new TripleIDInt(1, 2, 3).isValid());
		assertFalse(new TripleIDInt(0, 2, 3).isValid());
		assertFalse(new TripleIDInt(1, 0, 3).isValid());
		assertFalse(new TripleIDInt(1, 2, 0).isValid());
	}

	@Test
	public void isEmptyRequiresAllZero() {
		assertTrue(new TripleIDInt().isEmpty());
		assertFalse(new TripleIDInt(1, 0, 0).isEmpty());
		assertFalse(new TripleIDInt(0, 1, 0).isEmpty());
		assertFalse(new TripleIDInt(0, 0, 1).isEmpty());
	}

	@Test
	public void getPatternStringMarksWildcards() {
		assertEquals("SPO", new TripleIDInt(1, 2, 3).getPatternString());
		assertEquals("???", new TripleIDInt().getPatternString());
		assertEquals("S?O", new TripleIDInt(1, 0, 3).getPatternString());
		assertEquals("?P?", new TripleIDInt(0, 1, 0).getPatternString());
	}

	@Test
	public void toStringIsSpaceSeparatedComponents() {
		assertEquals("1 2 3", new TripleIDInt(1, 2, 3).toString());
	}

	@Test
	public void asTripleIDProducesEquivalentLongTriple() {
		TripleID t = new TripleIDInt(1, 2, 3).asTripleID();
		assertEquals(1L, t.getSubject());
		assertEquals(2L, t.getPredicate());
		assertEquals(3L, t.getObject());
	}

	@Test
	public void sizeIsTwentyFourBytes() {
		assertEquals(24, TripleIDInt.size());
	}
}
