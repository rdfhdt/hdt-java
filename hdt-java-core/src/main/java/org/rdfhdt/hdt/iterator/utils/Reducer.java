package org.rdfhdt.hdt.iterator.utils;

public interface Reducer <T> {
	/**
	 * Reduce a and b into a (new) objects. Returns null if cannot reduce (different)
	 * @param a
	 * @param b
	 * @return
	 */
	T reduce(T a, T b);
}
