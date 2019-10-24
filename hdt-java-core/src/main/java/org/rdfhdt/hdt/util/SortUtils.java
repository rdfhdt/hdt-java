package org.rdfhdt.hdt.util;

public class SortUtils {
	/**
	 * Searches a sorted int array for a specified value, using an optimized
	 * binary search algorithm (which tries to guess smart pivots). The result
	 * is unspecified if the array is not sorted. The method returns an index
	 * where key was found in the array. If the array contains duplicates, this
	 * might not be the first occurrence.
	 * 
	 * @see java.util.Arrays.sort(int[])
	 * @see java.util.Arrays.binarySearch(int[])
	 * @param array
	 *            sorted array of integers
	 * @param key
	 *            value to search for in the array
	 * @param offset
	 *            index of the first valid value in the array
	 * @param length
	 *            number of valid values in the array
	 * @return index of an occurrence of key in array, or -(insertionIndex + 1)
	 *         if key is not contained in array (&lt;i>insertionIndex&lt;/i> is
	 *         then the index at which key could be inserted).
	 */

	public static final int binarySearch(long[] array, long key) {
		return binarySearch(array, key, 0, array.length);
	}

	public static final int binarySearch(long[] array, long key, int offset, int length) {// min,
																							// int
																							// max)
																							// {
		if (length == 0) {
			return -1 - offset;
		}
		int min = offset, max = offset + length - 1;
		long minVal = array[min], maxVal = array[max];
		int nPreviousSteps = 0;
		// Uncomment these two lines to get statistics about the average number
		// of steps in the test report :
		// totalCalls++;
		for (;;) {
			// totalSteps++;
			// be careful not to compute key - minVal, for there might be an
			// integer overflow.
			if (key <= minVal)
				return key == minVal ? min : -1 - min;
			if (key >= maxVal)
				return key == maxVal ? max : -2 - max;
			assert min != max;
			int pivot;
			// A typical binarySearch algorithm uses pivot = (min + max) / 2.
			// The pivot we use here tries to be smarter and to choose a pivot
			// close to the expectable location of the key.
			// This reduces dramatically the number of steps needed to get to
			// the key.
			// However, it does not work well with a logaritmic distribution of
			// values, for instance.
			// When the key is not found quickly the smart way, we switch to the
			// standard pivot.
			if (nPreviousSteps > 2) {
				pivot = (min + max) >> 1;
				// stop increasing nPreviousSteps from now on
			} else {
				// NOTE: We cannot do the following operations in int precision,
				// because there might be overflows.
				// long operations are slower than float operations with the
				// hardware this was tested on (intel core duo 2, JVM 1.6.0).
				// Overall, using float proved to be the safest and fastest
				// approach.
				pivot = min + (int) ((key - (float) minVal) / (maxVal - (float) minVal) * (max - min));
				nPreviousSteps++;
			}
			long pivotVal = array[pivot];
			// NOTE: do not store key - pivotVal because of overflows
			if (key > pivotVal) {
				min = pivot + 1;
				max--;
			} else if (key == pivotVal) {
				return pivot;
			} else {
				min++;
				max = pivot - 1;
			}
			maxVal = array[max];
			minVal = array[min];
		}
	}

	public static final int binarySearch(int[] array, int key) {
		return binarySearch(array, key, 0, array.length);
	}

	/**
	 * Searches a sorted int array for a specified value, using an optimized
	 * binary search algorithm (which tries to guess smart pivots). The result
	 * is unspecified if the array is not sorted. The method returns an index
	 * where key was found in the array. If the array contains duplicates, this
	 * might not be the first occurrence.
	 * 
	 * @see java.util.Arrays.sort(int[])
	 * @see java.util.Arrays.binarySearch(int[])
	 * @param array
	 *            sorted array of integers
	 * @param key
	 *            value to search for in the array
	 * @param offset
	 *            index of the first valid value in the array
	 * @param length
	 *            number of valid values in the array
	 * @return index of an occurrence of key in array, or -(insertionIndex + 1)
	 *         if key is not contained in array (&lt;i>insertionIndex&lt;/i> is
	 *         then the index at which key could be inserted).
	 */
	public static final int binarySearch(int[] array, int key, int offset, int length) {// min,
																						// int
																						// max)
																						// {
		if (length == 0) {
			return -1 - offset;
		}
		int min = offset, max = offset + length - 1;
		int minVal = array[min], maxVal = array[max];
		int nPreviousSteps = 0;
		// Uncomment these two lines to get statistics about the average number
		// of steps in the test report :
		// totalCalls++;
		for (;;) {
			// totalSteps++;
			// be careful not to compute key - minVal, for there might be an
			// integer overflow.
			if (key <= minVal)
				return key == minVal ? min : -1 - min;
			if (key >= maxVal)
				return key == maxVal ? max : -2 - max;
			assert min != max;
			int pivot;
			// A typical binarySearch algorithm uses pivot = (min + max) / 2.
			// The pivot we use here tries to be smarter and to choose a pivot
			// close to the expectable location of the key.
			// This reduces dramatically the number of steps needed to get to
			// the key.
			// However, it does not work well with a logaritmic distribution of
			// values, for instance.
			// When the key is not found quickly the smart way, we switch to the
			// standard pivot.
			if (nPreviousSteps > 2) {
				pivot = (min + max) >> 1;
				// stop increasing nPreviousSteps from now on
			} else {
				// NOTE: We cannot do the following operations in int precision,
				// because there might be overflows.
				// long operations are slower than float operations with the
				// hardware this was tested on (intel core duo 2, JVM 1.6.0).
				// Overall, using float proved to be the safest and fastest
				// approach.
				pivot = min + (int) ((key - (float) minVal) / (maxVal - (float) minVal) * (max - min));
				nPreviousSteps++;
			}
			int pivotVal = array[pivot];
			// NOTE: do not store key - pivotVal because of overflows
			if (key > pivotVal) {
				min = pivot + 1;
				max--;
			} else if (key == pivotVal) {
				return pivot;
			} else {
				min++;
				max = pivot - 1;
			}
			maxVal = array[max];
			minVal = array[min];
		}
	}
}
