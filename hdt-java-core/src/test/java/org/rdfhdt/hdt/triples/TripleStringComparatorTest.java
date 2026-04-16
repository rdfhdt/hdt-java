package org.rdfhdt.hdt.triples;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TripleStringComparatorTest {

	private final TripleStringComparator cmp = new TripleStringComparator();

	private static TripleString t(String s, String p, String o) {
		return new TripleString(s, p, o);
	}

	@Test
	public void equalTriplesReturnZero() {
		assertEquals(0, cmp.compare(t("s", "p", "o"), t("s", "p", "o")));
	}

	@Test
	public void subjectIsPrimaryKey() {
		assertTrue(cmp.compare(t("a", "z", "z"), t("b", "a", "a")) < 0);
		assertTrue(cmp.compare(t("b", "a", "a"), t("a", "z", "z")) > 0);
	}

	@Test
	public void predicateIsSecondaryKey() {
		assertTrue(cmp.compare(t("s", "a", "z"), t("s", "b", "a")) < 0);
		assertTrue(cmp.compare(t("s", "b", "a"), t("s", "a", "z")) > 0);
	}

	@Test
	public void objectIsTertiaryKey() {
		assertTrue(cmp.compare(t("s", "p", "a"), t("s", "p", "b")) < 0);
		assertTrue(cmp.compare(t("s", "p", "b"), t("s", "p", "a")) > 0);
	}
}
