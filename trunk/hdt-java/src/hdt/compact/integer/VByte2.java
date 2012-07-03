/**
 * 
 */
package hdt.compact.integer;

import hdt.util.Mutable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Another implementation of Variable-Byte encoding for Integer.
 * 
 * This implementation uses the special code 255 to indicate that there 
 * are more bytes remaining. 
 * 
 * Its advantage over VByte.java is that it uses fewer bytes for small values,
 * but the byte-growth is exponential. That makes it suitable only for the cases
 * when very small values (0-254) are really much more frequent than bigger values, 
 * and there are no huge values. Otherwise use traditional VByte instead.
 * 
 * Numbers from 0 to 254 are encoded using one byte.
 * Numbers from 255 to 509 are encoded using two bytes.
 * Numbers from 510 to 754 are encoded using three bytes.
 * Numbers from 755 to 1019 are encoded using four bytes.
 * So on, adding 254.
 * 
 * @author mck
 *
 */
public class VByte2 {
	public static void encode(OutputStream out, int value) throws IOException {
		while(value>254) {
			value -= 255;
			out.write(255);
		}
		out.write(value);
	}
	
	public static int decode(InputStream in) throws IOException {
		int out = 0;
		while(in.read()==255) {
			out+=255;
		}
		out+=in.read();
		return out;
	}
	
	public static int encode(byte[] data, int offset, int value) {
		int bytes = 0;
		while(value>254) {
			value -= 255;
			data[offset+bytes] = (byte)255;
			bytes++;
		}
		data[offset+bytes] = (byte) value;
		bytes++;
		return bytes;
	}
	
	public static int decode(byte[] data, int offset, Mutable<Integer> value) {
		int out = 0;
		int i=0;
		while(data[offset+i]==(byte)255) {
			out+=255;
			i++;
		}
		out+= (data[offset+i]&0xFF);
		i++;
		value.setValue(out);
		return i;
	}
}
