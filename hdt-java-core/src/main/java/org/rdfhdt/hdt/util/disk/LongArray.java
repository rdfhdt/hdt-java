package org.rdfhdt.hdt.util.disk;

/**
 * Describe a large array of longs
 */
public interface LongArray {
	/**
	 * get an element at a particular index
	 * @param index the index
	 * @return the value
	 */
	long get(long index);
	/**
	 * Set a new value at the specified position.
	 * @param index the index
	 * @param value the value
	 */
	void set(long index, long value);

	/**
	 * @return the length of the array
	 */
	long length();
}
