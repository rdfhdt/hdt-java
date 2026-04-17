package org.rdfhdt.hdt.triples;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class IndexedTripleTest {

	@Test
	public void defaultConstructorYieldsNullComponents() {
		IndexedTriple t = new IndexedTriple();
		assertNull(t.getSubject());
		assertNull(t.getPredicate());
		assertNull(t.getObject());
	}

	@Test
	public void threeArgConstructorStoresAllComponents() {
		IndexedNode s = new IndexedNode("s", 1);
		IndexedNode p = new IndexedNode("p", 2);
		IndexedNode o = new IndexedNode("o", 3);
		IndexedTriple t = new IndexedTriple(s, p, o);
		assertSame(s, t.getSubject());
		assertSame(p, t.getPredicate());
		assertSame(o, t.getObject());
	}

	@Test
	public void loadReplacesAllComponents() {
		IndexedTriple t = new IndexedTriple();
		IndexedNode s = new IndexedNode("s", 1);
		IndexedNode p = new IndexedNode("p", 2);
		IndexedNode o = new IndexedNode("o", 3);
		t.load(s, p, o);
		assertSame(s, t.getSubject());
		assertSame(p, t.getPredicate());
		assertSame(o, t.getObject());

		// Load a second set of components
		IndexedNode s2 = new IndexedNode("s2", 10);
		t.load(s2, p, o);
		assertSame(s2, t.getSubject());
		assertEquals(10L, t.getSubject().getIndex());
	}
}
