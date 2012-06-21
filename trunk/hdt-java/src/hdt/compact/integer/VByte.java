/**
 * Revision: $Rev$
 * Last modified: $Date$
 * Last modified by: $Author$
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Contacting the authors:
 *   Mario Arias:               mario.arias@deri.org
 *   Javier D. Fernandez:       jfergar@infor.uva.es
 *   Miguel A. Martinez-Prieto: migumar2@infor.uva.es
 *   Alejandro Andres:          fuzzy.alej@gmail.com
 */
/**
 * 
 */
package hdt.compact.integer;

import hdt.util.Mutable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Typical implementation of Variable-Byte encoding for integers.
 * http://nlp.stanford.edu/IR-book/html/htmledition/variable-byte-codes-1.html
 * 
 * The first bit of each byte specifies whether there are more bytes available.
 * Numbers from 0 to 126 are encoded using just one byte.
 * Numbers from 127 to 16383 are encoded using two bytes.
 * Numbers from 16384 to 2097151 are encodeng using three bytes.
 * 
 * @author mck
 *
 */
public class VByte {
	public static void encode(OutputStream out, int value) throws IOException {
		while( value > 127) {
			out.write(value & 127);
			value>>>=7;
		}
		out.write(value|0x80);
	}
	
	public static int decode(InputStream in) throws IOException {
		int out = 0;
		int shift=0;
		byte readbyte = (byte)in.read();
		while( (readbyte & 0x80)==0) {
			out |= (readbyte & 127) << shift;
			readbyte = (byte)in.read();
			shift+=7;
		}
		out |= (readbyte & 127) << shift;
		return out;
	}
	
	public static int encode(byte[] data, int offset, int value) {
		int i=0;
		while( value > 127) {
			data[offset+i] = (byte)(value & 127);
			i++;
			value>>>=7;
		}
		data[offset+i] = (byte)(value|0x80);
		i++;
		
		return i;
	}
	
	public static int decode(byte[] data, int offset, Mutable<Integer> value) {
		int out = 0;
		int i=0;
		int shift=0;
		while( (0x80 & data[offset+i])==0) {
			out |= (data[offset+i] & 127) << shift;
			i++;
			shift+=7;
		}
		out |= (data[offset+i] & 127) << shift;
		i++;
		value.setValue(out);
		return i;
	}
	
	public static void show(byte[] data, int len) {
		for(int i=0;i<len;i++) {
			System.out.print(Long.toHexString(data[i]&0xFF)+" ");
		}
	}
}
