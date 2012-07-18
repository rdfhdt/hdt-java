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
package org.rdfhdt.hdt.util.io;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.rdfhdt.hdt.listener.ProgressListener;

/**
 * @author mario.arias
 *
 */
public class IOUtil {
	public static String readLine(InputStream in, char character) throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		while(true) {
			int value = in.read();
			if(value==-1) {
				throw new EOFException();
			}
			if(value==character) {
				break;
			}
			buf.write(value);
		}
		return new String(buf.toByteArray()); // Uses default encoding
	}
	
	public static String readChars(InputStream in, int numChars) throws IOException {
		StringBuilder out = new StringBuilder();
		for(int i=0;i<numChars;i++) {
			int c = in.read();
			if(c==-1) {
				throw new EOFException();
			}
			out.append((char)c);
		}
		return out.toString();
	}
	
	public static void writeLine(OutputStream out, String line) throws IOException {
		out.write(line.getBytes());
	}
	
	public static void writeBuffer(OutputStream output, byte [] buffer, int offset, int length, ProgressListener listener) throws IOException {
		// FIXME: Do by blocks and notify listener
		output.write(buffer, offset, length);
	}
	
	private static byte writeBuffer[] = new byte[8];
	
	/**
	 * Write long, little endian
	 * @param output
	 * @param value
	 * @throws IOException
	 */
	public static void writeLong(OutputStream output, long value) throws IOException {
		writeBuffer[7] = (byte)(value >>> 56);
		writeBuffer[6] = (byte)(value >>> 48);
		writeBuffer[5] = (byte)(value >>> 40);
		writeBuffer[4] = (byte)(value >>> 32);
		writeBuffer[3] = (byte)(value >>> 24);
		writeBuffer[2] = (byte)(value >>> 16);
		writeBuffer[1] = (byte)(value >>>  8);
		writeBuffer[0] = (byte)(value);
		output.write(writeBuffer, 0, 8);
	}
	
	private static byte readBuffer[] = new byte[8];
	
	/**
	 * Read long, little endian. Warning: Dont use concurrently!!
	 * @param input
	 * @return
	 * @throws IOException
	 */
	public static final long readLong(InputStream input) throws IOException {	
		int n = 0;
		while (n < 8) {
			int count = input.read(readBuffer, 0 , 8);
			if (count < 0)
				throw new EOFException();
			n += count;
		}
		
		return ( ((long)readBuffer[7] << 56) +
				 ((long)(readBuffer[6] & 255) << 48) +
				 ((long)(readBuffer[5] & 255) << 40) +
				 ((long)(readBuffer[4] & 255) << 32) +
				 ((long)(readBuffer[3] & 255) << 24) +
				 ((readBuffer[2] & 255) << 16) +
				 ((readBuffer[1] & 255) <<  8) +
				 ((readBuffer[0] & 255))
			);
	}

	/**
	 * Write int, little endian
	 * @param output
	 * @param value
	 * @throws IOException
	 */
	public static void writeInt(OutputStream output, int value) throws IOException {
		writeBuffer[0] = (byte) (value & 0xFF);
		writeBuffer[1] = (byte) ((value>>8) & 0xFF);
		writeBuffer[2] = (byte) ((value>>16) & 0xFF);
		writeBuffer[3] = (byte) ((value>>24) & 0xFF);
		output.write(writeBuffer,0,4);
	}
	
	/**
	 * Read int, little endian
	 * @param input
	 * @return
	 * @throws IOException
	 */
	public static final int readInt(InputStream in) throws IOException {
		int ch1 = in.read();
		int ch2 = in.read();
		int ch3 = in.read();
		int ch4 = in.read();
		if ((ch1 | ch2 | ch3 | ch4) < 0)
			throw new EOFException();
		return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
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
	
	public static CharSequence toBinaryString(int val) {
		StringBuilder str = new StringBuilder(32);
		int bits = 32;
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

	public static short readShort(InputStream in) throws IOException {
		int ch1 = in.read();
		int ch2 = in.read();

		if ((ch1 | ch2) < 0) {
			throw new EOFException();
		}
		
		return (short)((ch2 << 8) + (ch1));
	}
	
	public static void writeShort(OutputStream out, short value) throws IOException {
		out.write(value & 0xFF);
		out.write((value >> 8) & 0xFF);
	}
}
