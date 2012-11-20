package org.rdfhdt.hdt.compact.array;

import java.io.FileOutputStream;

import org.rdfhdt.hdt.compact.sequence.SequenceLog64;
import org.rdfhdt.hdt.util.BitUtil;

public class LogArray64Dump {

	private static long calculate(long bits, long n) {
		return ((bits*n+7)/8);
	}
	
	/**
	 * @param args
	 * @throws Throwable 
	 */
	public static void main(String[] args) throws Throwable {
		int num = 100;
		System.out.println("Bits: "+BitUtil.log2(num)+" Num: "+num);
		SequenceLog64 arr = new SequenceLog64(BitUtil.log2(num), num);
		for(int i=0;i<num;i++) {
			arr.append(i);
		}
		FileOutputStream out = new FileOutputStream("logarr2.bin");
		arr.save(out, null);
		out.close();
		
		for(int i=0;i<num;i++) {
			System.out.println(arr.get(i));
		}
		
		int bits = 7;
		for(int i=0;i<100;i++) {
			System.out.println(i+" values of "+ bits+" bits: "+ calculate(bits,i)+" bytes    Total bits: "+ (bits*i) + " Bytes: "+ (bits*i)/8+ " Remaind: "+(bits*i)%8);
		}
	}

}
