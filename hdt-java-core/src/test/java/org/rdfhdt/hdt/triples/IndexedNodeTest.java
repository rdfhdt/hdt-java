package org.rdfhdt.hdt.triples;

import org.junit.Test;
import org.rdfhdt.hdt.util.string.ByteString;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class IndexedNodeTest {

	@Test
	public void constructorAndGettersForByteString() {
		ByteString node = ByteString.of("hello");
		IndexedNode n = new IndexedNode(node, 42L);
		assertEquals(node, n.getNode());
		assertEquals(42L, n.getIndex());
	}

	@Test
	public void charSequenceConstructorWrapsInByteString() {
		IndexedNode n = new IndexedNode("hello", 7L);
		assertEquals(ByteString.of("hello"), n.getNode());
		assertEquals(7L, n.getIndex());
	}

	@Test
	public void defaultConstructorProducesEmpty() {
		IndexedNode n = new IndexedNode();
		// no exceptions — and setters work
		n.setNode(ByteString.of("set"));
		n.setIndex(99L);
		assertEquals(ByteString.of("set"), n.getNode());
		assertEquals(99L, n.getIndex());
	}

	@Test
	public void compareToIgnoresIndex() {
		IndexedNode a = new IndexedNode("aaa", 10L);
		IndexedNode b = new IndexedNode("bbb", 1L);
		assertTrue(a.compareTo(b) < 0);
		assertTrue(b.compareTo(a) > 0);
		assertEquals(0, a.compareTo(new IndexedNode("aaa", 999L)));
	}

	@Test
	public void cloneProducesIndependentCopy() {
		IndexedNode src = new IndexedNode("value", 5L);
		IndexedNode copy = src.clone();
		assertNotSame(src, copy);
		assertEquals(src.getIndex(), copy.getIndex());
		assertEquals(0, src.compareTo(copy));
		// Mutating the copy must not affect the original
		copy.setIndex(100L);
		assertEquals(5L, src.getIndex());
		assertEquals(100L, copy.getIndex());
	}
}
