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
package hdt.util.io;

import hdt.listener.ProgressListener;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author mck
 *
 */
public class IOUtil {
	public static String readLine(DataInputStream in, char character) throws IOException {
		StringBuilder out = new StringBuilder();
		while(true) {
			char c = (char) in.readByte();
			if(c==character) {
				break;
			}
			out.append(c);
		}
		return out.toString();
	}
	
	public static String readChars(DataInputStream in, int numChars) throws IOException {
		StringBuilder out = new StringBuilder();
		for(int i=0;i<numChars;i++) {
			char c = (char) in.readByte();
			out.append(c);
		}
		return out.toString();
	}
	
	public static void writeLine(DataOutputStream out, String line) throws IOException {
		for(int i=0;i<line.length();i++) {
			byte charByte = (byte) line.charAt(i);
			out.writeByte(charByte);
		}
	}
	
	public static void writeBuffer(OutputStream output, byte [] buffer, int offset, int length, ProgressListener listener) throws IOException {
		// FIXME: Do by blocks and notify listener
		output.write(buffer, offset, length);
	}

	/**
	 * @param din
	 * @param bytes
	 * @param listener
	 * @return
	 */
	public static final byte[] readBuffer(DataInputStream input, int length, ProgressListener listener) throws IOException {
		// FIXME: Do by blocks and notify listener
		byte [] buffer = new byte[length];
		input.read(buffer, 0, length);
		return buffer;
	}
	
	public static final void printBits(long val, int bits) {
		while(bits-- != 0) {
			System.out.print( ((val>>>bits) & 1) !=0 ? '1' : '0');
		}
		System.out.println();
	}
}
