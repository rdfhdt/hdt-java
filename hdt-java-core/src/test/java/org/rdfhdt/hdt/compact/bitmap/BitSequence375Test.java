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
	final static int num=10000;
	
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
	

	// Naive impl of select0
//		public long select0b(long x) {
//			if(x<=0) {
//				return -1;
//			}
//			long numzeros=0;
//			for(long i=0;i<getNumBits();i++) {
//				if(!access(i)) {
//					numzeros++;
//				}
//				if(numzeros==x) {
//					return i;
//				}
//			}
//			return getNumBits();
//		}

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
		
//		assertEquals(0, Bitmap64.numWords(0));
		assertEquals(1, Bitmap64.numWords(1));
		assertEquals(1, Bitmap64.numWords(64));
		assertEquals(2, Bitmap64.numWords(65));
		assertEquals(5, Bitmap64.numWords(257));
		
//		for(int i=0;i<1000;i++) {
//			int words=(int) Bitmap64.numWords(i);
//			int bitsLast=Bitmap64.lastWordNumBits(i);
//			int bytes = (int) Bitmap64.numBytes(i);
//			System.out.println(i+" bits  "+bytes+" bytes  "+words+" words / "+bitsLast+" bits last.");
//		}	
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
		long count = 0;
		for(long i=0;i<bitseq.getNumBits();i++) {
			if(bitseq.access(i)) {
				count++;
//				assertEquals("Wrong rank1", count, bitseq.rank1(i));
//				assertEquals("Wrong rank new", bitseq.rank1(i), bitnew.rank1(i));
				
				if(bitseq.rank1(i)!=count) {
					System.out.println("Different");
					
					long a= bitseq.rank1(i);
				}
				
				assertEquals(count, bitseq.rank1(i));
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
	public void testSelectNext1() {
		for(long i=0;i<bitseq.getNumBits();i++) {
//			System.out.println("Pos: "+i+" => "+ (bitseq.access(i)?"1":"0")+ " Next: "+bitseq.selectNext1(i));
		}	
	}
	
	public long select0b(Bitmap375 bitseq, long x) {
		if(x<=0) {
			return -1;
		}
		long numzeros=0;
		for(long i=0;i<bitseq.getNumBits();i++) {
			if(!bitseq.access(i)) {
				numzeros++;
			}
			if(numzeros==x) {
				return i;
			}
		}
		return bitseq.getNumBits();
	}
	
	@Test
	public void testSelect0() {
		long numzeros=0;
		for(long i=0;i<bitseq.getNumBits();i++) {
			
//			System.out.print("***"+i+"> "+(bitseq.access(i)?1:0)+ " \t  Rank1("+i+"): "+ bitseq.rank1(i) + " Select1("+bitseq.rank1(i)+"): " + bitseq.select1(bitseq.rank1(i))+ " SelectNext1("+i+"): "+ bitseq.selectNext1(i));
			if(bitseq.access(i)) {
//				assertEquals("Select0 wrong",i,  (bitseq.select0(bitseq.rank0(i))));
			} else {
				numzeros++;
			}
			long rk = bitseq.rank0(i);
			long sel = bitseq.select0(rk);
			long selb = select0b(bitseq, rk);
//			System.out.println(i+" => "+ (bitseq.access(i) ? "1" : "0") +" Zeros: "+numzeros+" Rank0="+rk+ " Sel0="+sel+" Sel0b="+selb);
			assertEquals("Select0 wrong", sel, selb);
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
	

	@Test
	public void testBitutilSelect0() {
//		IOUtil.printBitsln(bitseq.words[0],64);
		int numbits = 64-Long.bitCount(bitseq.words[0]);
//		for(int i=1;i<=numbits;i++) {
//			System.out.println("Select0("+i+") = "+BitUtil.select0(bitseq.words[0], i));
//		}
	}
}
