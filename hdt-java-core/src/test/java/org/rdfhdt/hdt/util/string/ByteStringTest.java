package org.rdfhdt.hdt.util.string;

import org.junit.Test;

import java.text.Collator;

public class ByteStringTest {
	private static void printHex(byte[] b) {
		for (byte bb : b) {
			System.out.printf("%2x ", bb);
		}
		System.out.println();
	}
	private static void printBin(byte[] b) {
		for (byte bb : b) {
			String s = Integer.toBinaryString(bb & 0xFF);
			System.out.print("0".repeat(8 - s.length()) + s + " ");
		}
		System.out.println();
	}
	@Test
	public void utf32Test() {
		String ss1 = "\uD85B\uDCE3";
		String ss2 = "\uF4D1";

		ByteString b1 = ByteString.of(ss1);
		ByteString b2 = ByteString.of(ss2);

		assert ss1.equals(b1.toString());
		assert ss2.equals(b2.toString());

		Collator coll = Collator.getInstance();

		System.out.println("BYTESTRING: " + b1 + (b1.compareTo(b2) < 0 ? " < " : " > ") + b2);
		System.out.println("STRING    : " + b1 + (b1.toString().compareTo(b2.toString()) < 0 ? " < " : " > ") + b2);
		System.out.println("COLLATOR  : " + b1 + (coll.compare(b1.toString(), b2.toString()) < 0 ? " < " : " > ") + b2);

		printHex(b1.getBuffer());
		printHex(b2.getBuffer());

		printBin(b1.getBuffer());
		printBin(b2.getBuffer());

		System.out.println(Character.isHighSurrogate(ss1.charAt(0)) + ", " + Character.isLowSurrogate(ss1.charAt(1)));
		System.out.println(Character.toCodePoint(ss1.charAt(0), ss1.charAt(1)));
		System.out.println((int) ss2.charAt(0));
	}
}
