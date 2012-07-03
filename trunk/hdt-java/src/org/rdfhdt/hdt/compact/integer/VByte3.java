/**
 * 
 */
package org.rdfhdt.hdt.compact.integer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Uses 1 byte for 0-254. For 255 or more, it encodes the following bytes using 
 * traditional VByte.
 * 0-254 1Byte.
 * 255-370 2Byte.
 * 371-xxx 3+ Byte.
 * 
 * @author mck
 *
 */
public class VByte3 {
	public static void encode(OutputStream out, int value) throws IOException {
		if(value<255) {
			out.write(value);
		} else {
			out.write(255);
			VByte.encode(out, value-255);
		}
	}
	
	public static int decode(InputStream in) throws IOException {
		int read = in.read();
		if(read<255) {
			return read;
		}
		return 255+VByte.decode(in);
	}
}
