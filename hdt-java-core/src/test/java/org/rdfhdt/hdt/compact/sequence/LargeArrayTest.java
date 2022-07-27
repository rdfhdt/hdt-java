package org.rdfhdt.hdt.compact.sequence;

import org.junit.Test;
import org.visnow.jlargearrays.LargeArray;
import org.visnow.jlargearrays.LongLargeArray;

public class LargeArrayTest {

	@Test
	public void allocationTest() {
		int old = LargeArray.getMaxSizeOf32bitArray();
		try {
			LargeArray.setMaxSizeOf32bitArray(100);
			long size = LargeArray.getMaxSizeOf32bitArray() + 2L;
			new LongLargeArray(size);
		} finally {
			LargeArray.setMaxSizeOf32bitArray(old);
		}
	}
}
