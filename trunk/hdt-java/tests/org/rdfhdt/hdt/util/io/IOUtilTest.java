package org.rdfhdt.hdt.util.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

public class IOUtilTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testWriteLong() {
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			
			IOUtil.writeLong(bout, 3);
			IOUtil.writeLong(bout, 4);
			IOUtil.writeLong(bout, 0xFF000000000000AAL);
			IOUtil.writeLong(bout, 0x33AABBCCDDEEFF11L);
						
			ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
			
			long a = IOUtil.readLong(bin);
			assertEquals(a, 3);
			
			long b = IOUtil.readLong(bin);
			assertEquals(b, 4);
			
			long c = IOUtil.readLong(bin);
			assertEquals(c, 0xFF000000000000AAL);
			
			long d = IOUtil.readLong(bin);
			assertEquals(d, 0x33AABBCCDDEEFF11L);
			
		} catch (IOException e) {
			fail("Exception thrown: "+e);
		}
	}

	@Test
	public void testWriteInt() {
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			
			IOUtil.writeInt(bout, 3);
			IOUtil.writeInt(bout, 4);
			IOUtil.writeInt(bout, 0xFF0000AA);
			IOUtil.writeInt(bout, 0xAABBCCDD);
			
			ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
			
			long a = IOUtil.readInt(bin);
			assertEquals(a, 3);
			
			long b = IOUtil.readInt(bin);
			assertEquals(b, 4);
			
			long c = IOUtil.readInt(bin);
			assertEquals(c, 0xFF0000AA);
			
			long d = IOUtil.readInt(bin);
			assertEquals(d, 0xAABBCCDD);
			
		} catch (IOException e) {
			fail("Exception thrown: "+e);
		}
	}
}
