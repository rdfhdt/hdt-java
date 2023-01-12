package org.rdfhdt.hdt.compact.sequence;

import org.junit.Test;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.visnow.jlargearrays.LargeArray;
import org.visnow.jlargearrays.LongLargeArray;

public class LargeArrayTest {

	@Test
	public void allocationTest() {
		int old = LargeArray.getMaxSizeOf32bitArray();
		try {
			LargeArray.setMaxSizeOf32bitArray(100);
			long size = LargeArray.getMaxSizeOf32bitArray() + 2L;
			IOUtil.createLargeArray(size, false);
		} finally {
			LargeArray.setMaxSizeOf32bitArray(old);
		}
	}
}
