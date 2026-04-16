package org.rdfhdt.hdt.iterator.utils;

import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ArrayIteratorsTest {

	// --- ArrayIterator (int) ---

	@Test
	public void arrayIteratorFullRange() {
		ArrayIterator it = new ArrayIterator(new int[] {1, 2, 3});
		assertEquals(Integer.valueOf(1), it.next());
		assertEquals(Integer.valueOf(2), it.next());
		assertEquals(Integer.valueOf(3), it.next());
		assertFalse(it.hasNext());
	}

	@Test
	public void arrayIteratorSubRangeClampsMax() {
		// max is clamped by Math.min(arr.length, max)
		ArrayIterator it = new ArrayIterator(new int[] {1, 2, 3, 4, 5}, 1, 100);
		assertEquals(Integer.valueOf(2), it.next());
		assertEquals(Integer.valueOf(3), it.next());
		assertEquals(Integer.valueOf(4), it.next());
		assertEquals(Integer.valueOf(5), it.next());
		assertFalse(it.hasNext());
	}

	@Test
	public void arrayIteratorRemoveIsNoOp() {
		new ArrayIterator(new int[] {1}).remove();
	}

	// --- ArrayIteratorG (generic) ---

	@Test
	public void arrayIteratorGFullRange() {
		ArrayIteratorG<String> it = new ArrayIteratorG<>(new String[] {"a", "b"});
		assertEquals("a", it.next());
		assertEquals("b", it.next());
		assertFalse(it.hasNext());
	}

	@Test
	public void arrayIteratorGSubRange() {
		ArrayIteratorG<String> it = new ArrayIteratorG<>(new String[] {"a", "b", "c"}, 1, 2);
		assertEquals("b", it.next());
		assertFalse(it.hasNext());
	}

	@Test
	public void arrayIteratorGRemoveIsNoOp() {
		new ArrayIteratorG<>(new String[] {"a"}).remove();
	}

	// --- ArrayIteratorLong ---

	@Test
	public void arrayIteratorLongFullRange() {
		ArrayIteratorLong it = new ArrayIteratorLong(new long[] {10, 20, 30});
		assertEquals(Long.valueOf(10), it.next());
		assertEquals(Long.valueOf(20), it.next());
		assertEquals(Long.valueOf(30), it.next());
		assertFalse(it.hasNext());
	}

	@Test
	public void arrayIteratorLongSubRange() {
		ArrayIteratorLong it = new ArrayIteratorLong(new long[] {10, 20, 30, 40}, 1L, 3L);
		assertEquals(Long.valueOf(20), it.next());
		assertEquals(Long.valueOf(30), it.next());
		assertFalse(it.hasNext());
	}

	@Test
	public void arrayIteratorLongRejectsTwoGigElementBoundary() {
		long[] arr = new long[1];
		long tooBig = (long) Integer.MAX_VALUE + 1;
		assertThrows(RuntimeException.class, () -> new ArrayIteratorLong(arr, tooBig, tooBig + 1));
		assertThrows(RuntimeException.class, () -> new ArrayIteratorLong(arr, 0L, tooBig));
	}

	@Test
	public void arrayIteratorLongRemoveIsNoOp() {
		new ArrayIteratorLong(new long[] {1}).remove();
	}

	// --- ArrayIteratorCharSequence ---

	@Test
	public void arrayIteratorCharSequenceFullRange() {
		ArrayIteratorCharSequence it = new ArrayIteratorCharSequence(
				new CharSequence[] {"x", "y"});
		assertEquals("x", it.next());
		assertEquals("y", it.next());
		assertFalse(it.hasNext());
	}

	@Test
	public void arrayIteratorCharSequenceSubRange() {
		ArrayIteratorCharSequence it = new ArrayIteratorCharSequence(
				new CharSequence[] {"x", "y", "z"}, 1, 3);
		assertEquals("y", it.next());
		assertEquals("z", it.next());
		assertFalse(it.hasNext());
	}

	@Test
	public void arrayIteratorCharSequenceRemoveIsNoOp() {
		new ArrayIteratorCharSequence(new CharSequence[] {"x"}).remove();
	}

	// --- SeveralIterator (via PrefetchIterator) ---

	@Test
	public void severalIteratorFlattensAllValuesInOrder() {
		Iterator<Integer> keys = Arrays.asList(1, 2, 3).iterator();
		SeveralIterator<Integer, String> it = new SeveralIterator<>(
				keys, k -> Arrays.asList("k" + k + "a", "k" + k + "b").iterator());
		List<String> collected = new java.util.ArrayList<>();
		while (it.hasNext()) collected.add(it.next());
		assertEquals(Arrays.asList("k1a", "k1b", "k2a", "k2b", "k3a", "k3b"), collected);
	}

	@Test
	public void severalIteratorSkipsKeysWhoseValueIteratorIsEmpty() {
		Iterator<Integer> keys = Arrays.asList(1, 2, 3).iterator();
		SeveralIterator<Integer, Integer> it = new SeveralIterator<>(
				keys, k -> (k == 2) ? java.util.Collections.emptyIterator()
						: java.util.Collections.singletonList(k).iterator());
		List<Integer> collected = new java.util.ArrayList<>();
		while (it.hasNext()) collected.add(it.next());
		assertEquals(Arrays.asList(1, 3), collected);
	}

	@Test
	public void severalIteratorEmptyWhenNoKeys() {
		SeveralIterator<Integer, String> it = new SeveralIterator<>(
				java.util.Collections.emptyIterator(), k -> null);
		assertFalse(it.hasNext());
	}

	// --- ListTripleIDIterator ---

	@Test
	public void listTripleIDIteratorBasicNavigation() {
		List<org.rdfhdt.hdt.triples.TripleID> triples = Arrays.asList(
				new org.rdfhdt.hdt.triples.TripleID(1, 2, 3),
				new org.rdfhdt.hdt.triples.TripleID(4, 5, 6));
		ListTripleIDIterator it = new ListTripleIDIterator(triples);
		assertTrue(it.canGoTo());
		assertEquals(2, it.estimatedNumResults());
		assertEquals(org.rdfhdt.hdt.enums.ResultEstimationType.EXACT, it.numResultEstimation());
		assertEquals(org.rdfhdt.hdt.enums.TripleComponentOrder.Unknown, it.getOrder());
		assertFalse(it.hasPrevious());
		assertEquals(new org.rdfhdt.hdt.triples.TripleID(1, 2, 3), it.next());
		assertEquals(0, it.getLastTriplePosition());
		assertEquals(new org.rdfhdt.hdt.triples.TripleID(4, 5, 6), it.next());
		assertFalse(it.hasNext());
		assertTrue(it.hasPrevious());
		assertEquals(new org.rdfhdt.hdt.triples.TripleID(4, 5, 6), it.previous());
	}

	@Test
	public void listTripleIDIteratorGoToAndGoToStart() {
		List<org.rdfhdt.hdt.triples.TripleID> triples = Arrays.asList(
				new org.rdfhdt.hdt.triples.TripleID(1, 1, 1),
				new org.rdfhdt.hdt.triples.TripleID(2, 2, 2),
				new org.rdfhdt.hdt.triples.TripleID(3, 3, 3));
		ListTripleIDIterator it = new ListTripleIDIterator(triples);
		it.goTo(2);
		assertEquals(new org.rdfhdt.hdt.triples.TripleID(3, 3, 3), it.next());
		it.goToStart();
		assertEquals(new org.rdfhdt.hdt.triples.TripleID(1, 1, 1), it.next());
	}

	@Test
	public void listTripleIDIteratorRemoveIsUnsupported() {
		ListTripleIDIterator it = new ListTripleIDIterator(java.util.Collections.emptyList());
		assertThrows(UnsupportedOperationException.class, it::remove);
	}
}
