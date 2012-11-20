package org.rdfhdt.hdt.compact.bitmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

public class BitSequence375Test {
	final static int num=100000;
	
	Bitmap375 bitseq;
	BitSet bitset; 
	
	@Before
	public void setUp() throws Exception {
		Random r = new Random(1);
		
		bitseq = new Bitmap375(num);
		bitset = new BitSet();
				
		for(int i=0;i<num;i++) {
			boolean value =r.nextBoolean(); 
			bitset.set(i, value);
			bitseq.set(i, value);
		}	
	}

	@Test
	public void testLoadSave() {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			bitseq.save(out, null);
			
			ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
			
			Bitmap375 loaded = new Bitmap375();
			loaded.load(in, null);
			
			assertEquals("Save/Load different number of elements", bitseq.getNumBits(), loaded.getNumBits());
			for(int i=0;i<bitseq.getNumBits();i++) {
				assertEquals("Save/Load different value", bitseq.access(i), loaded.access(i));
			}
		} catch (IOException e) {
			fail("Exception thrown: "+e);
		}
		
	}
	
	@Test
	public void testNumbits() {
		assertEquals(0, Bitmap64.lastWordNumBits(0));
		assertEquals(1, Bitmap64.lastWordNumBits(1));
		assertEquals(64, Bitmap64.lastWordNumBits(64));
		assertEquals(1, Bitmap64.lastWordNumBits(65));
		assertEquals(64, Bitmap64.lastWordNumBits(128));
		assertEquals(1, Bitmap64.lastWordNumBits(129));
		
		assertEquals(0, Bitmap64.numWords(0));
		assertEquals(1, Bitmap64.numWords(1));
		assertEquals(1, Bitmap64.numWords(64));
		assertEquals(2, Bitmap64.numWords(65));
		assertEquals(5, Bitmap64.numWords(257));
		
		for(int i=0;i<1000;i++) {
			int words=Bitmap64.numWords(i);
			int bitsLast=Bitmap64.lastWordNumBits(i);
			int bytes = Bitmap64.numBytes(i);
			System.out.println(i+" bits  "+bytes+" bytes  "+words+" words / "+bitsLast+" bits last.");
		}	
	}
	
	@Test 
	public void testSize() {
		assertEquals(bitseq.getNumBits(), num);
	}
	
	@Test
	public void testAccess() {
		for(int i=0;i<bitset.size();i++) {
			assertEquals(bitset.get(i), bitseq.access(i));
		}
	}

	@Test
	public void testRank1() {
		int count = 0;
		for(int i=0;i<bitseq.getNumBits();i++) {
			if(bitseq.access(i)) {
				count++;
				assertEquals("Wrong rank1", count, bitseq.rank1(i));
			}
		}
	}

	@Test
	public void testSelect1() {
		for(long i=0;i<bitseq.getNumBits();i++) {
			
//			System.out.print("***"+i+"> "+(bitseq.access(i)?1:0)+ " \t  Rank1("+i+"): "+ bitseq.rank1(i) + " Select1("+bitseq.rank1(i)+"): " + bitseq.select1(bitseq.rank1(i))+ " SelectNext1("+i+"): "+ bitseq.selectNext1(i));
			if(bitseq.access(i)) {
				assertEquals("Select1 wrong",i,  (bitseq.select1(bitseq.rank1(i))));
			}
		}	
	}

	@Test
	public void testCountOnes() {
		int count = 0;
		for(int i=0;i<bitseq.getNumBits();i++) {
			if(bitseq.access(i)) {
				count++;
			}
		}
		assertEquals("Wrong count ones", count, bitseq.countOnes());
	}

	@Test
	public void testCountZeros() {
		int count = 0;
		for(int i=0;i<bitseq.getNumBits();i++) {
			if(!bitseq.access(i)) {
				count++;
			}
		}
		assertEquals("Wrong count zeros", count, bitseq.countZeros());
	}
}
