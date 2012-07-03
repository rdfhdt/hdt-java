/**
 * File: $HeadURL$
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
package hdt.util.io;

import hdt.listener.ProgressListener;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author mario.arias
 *
 */
public class IOUtil {
	public static String readLine(DataInput in, char character) throws IOException {
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
	
	public static String readChars(DataInput in, int numChars) throws IOException {
		StringBuilder out = new StringBuilder();
		for(int i=0;i<numChars;i++) {
			char c = (char) in.readByte();
			out.append(c);
		}
		return out.toString();
	}
	
	public static void writeLine(DataOutput out, String line) throws IOException {
		for(int i=0;i<line.length();i++) {
			byte charByte = (byte) line.charAt(i);
			out.writeByte(charByte);
		}
	}
	
	public static void writeBuffer(OutputStream output, byte [] buffer, int offset, int length, ProgressListener listener) throws IOException {
		// FIXME: Do by blocks and notify listener
		output.write(buffer, offset, length);
	}
	
	public static void writeLong(OutputStream output, long value) throws IOException {
		final byte [] writeBuffer = new byte[8];
        writeBuffer[0] = (byte)(value >>> 56);
        writeBuffer[1] = (byte)(value >>> 48);
        writeBuffer[2] = (byte)(value >>> 40);
        writeBuffer[3] = (byte)(value >>> 32);
        writeBuffer[4] = (byte)(value >>> 24);
        writeBuffer[5] = (byte)(value >>> 16);
        writeBuffer[6] = (byte)(value >>>  8);
        writeBuffer[7] = (byte)(value >>>  0);
        output.write(writeBuffer, 0, 8);
	}
	
	public static final long readLong(InputStream input) throws IOException {
		final byte [] readBuffer = new byte[8];
		int nRead;
		int pos=0;
		while ((nRead = input.read(readBuffer, pos, 8-pos)) >0) {
			// TODO: Notify progress listener
			pos += nRead;
		}
		
		if(pos!=8) {
			throw new IOException("EOF while reading array from InputStream");
		}
		
		return (((long)readBuffer[0] << 56) +
				((long)(readBuffer[1] & 255) << 48) +
				((long)(readBuffer[2] & 255) << 40) +
				((long)(readBuffer[3] & 255) << 32) +
				((long)(readBuffer[4] & 255) << 24) +
				((readBuffer[5] & 255) << 16) +
				((readBuffer[6] & 255) <<  8) +
				((readBuffer[7] & 255) <<  0));
	}

	/**
	 * @param din
	 * @param bytes
	 * @param listener
	 * @return
	 */
	public static final byte[] readBuffer(InputStream input, int length, ProgressListener listener) throws IOException {
		int nRead;
		int pos=0;
		byte[] data = new byte[length];

		while ((nRead = input.read(data, pos, length-pos)) >0) {
			// TODO: Notify progress listener
			pos += nRead;
		}
		
		if(pos!=length) {
			throw new IOException("EOF while reading array from InputStream");
		}

		return data;
	}
	
	public static CharSequence toBinaryString(long val) {
		StringBuilder str = new StringBuilder(64);
		int bits = 64;
		while(bits-- != 0) {
			str.append(((val>>>bits) & 1) !=0 ? '1' : '0');
		}
		return str;
	}
	
	public static final void printBits(long val, int bits) {
		while(bits-- != 0) {
			System.out.print( ((val>>>bits) & 1) !=0 ? '1' : '0');
		}
		System.out.println();
	}
}
