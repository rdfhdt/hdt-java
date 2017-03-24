/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/util/io/IOUtil.java $
 * Revision: $Rev: 194 $
 * Last modified: $Date: 2013-03-04 21:30:01 +0000 (lun, 04 mar 2013) $
 * Last modified by: $Author: mario.arias $
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.util.string.ByteStringUtil;

/**
 * @author mario.arias
 *
 */
public class IOUtil {
	
	private IOUtil() {}
	
	public static InputStream getFileInputStream(String fileName) throws IOException {
		InputStream input;
		String name = fileName.toLowerCase();
		if(name.startsWith("http:/") || name.startsWith("ftp:/")) {
			URL url = new URL(fileName);
			URLConnection con = url.openConnection();
		    con.connect();
		    input = con.getInputStream();
		} else if(name.equals("-")) {
			input = new BufferedInputStream(System.in);
		} else {
			input = new BufferedInputStream(new FileInputStream(fileName));
		}
			
		if(name.endsWith(".gz")||name.endsWith(".tgz")) {
			input = new GZIPInputStream(input);
		} else if(name.endsWith("bz2") || name.endsWith("bz")) {	
			input = new BZip2CompressorInputStream(input, true);
		} else if(name.endsWith("xz")) {	
			input = new XZCompressorInputStream(input, true);
		}
		return input;
	}

	public static BufferedReader getFileReader(String fileName) throws IOException {
		return new BufferedReader(new InputStreamReader(getFileInputStream(fileName)));
	}
	
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

	public static void writeString(OutputStream out, String str) throws IOException {
		out.write(str.getBytes(ByteStringUtil.STRING_ENCODING));
	}

	public static void writeBuffer(OutputStream output, byte [] buffer, int offset, int length, ProgressListener listener) throws IOException {
		// FIXME: Do by blocks and notify listener
		output.write(buffer, offset, length);
	}

	// Copy the remaining of the Stream in, to out.
	public static void copyStream(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024*1024];
		int len;
		while ((len = in.read(buffer)) != -1) {
		    out.write(buffer, 0, len);
		}
	}

	// Copy the remaining of the Stream in, to out. Limit to n bytes.
	public static void copyStream(InputStream in, OutputStream out, long n) throws IOException {
		byte[] buffer = new byte[1024*1024];
		int len=(int) (buffer.length < n ? buffer.length : n);
		long total=0;

		while ((total<n) && (len = in.read(buffer, 0, len)) != -1 ) {
		    out.write(buffer, 0, len );

		    total+=len;
		    len = (int) (total+buffer.length>n ? n-total : buffer.length);
		}
	}

	public static void copyFile(File src, File dst) throws IOException {
		FileInputStream in = new FileInputStream(src);
		FileOutputStream out = new FileOutputStream(dst);
		try {
			copyStream(in, out);
		} finally {
			closeQuietly(in);
			closeQuietly(out);
		}
	}

	public static void moveFile(File src, File dst) throws IOException {
		copyFile(src, dst);
		src.delete();
	}

	public static void decompressGzip(File src, File trgt) throws IOException {
		InputStream in = new GZIPInputStream(new BufferedInputStream(new FileInputStream(src)));
		try {
			OutputStream out = new BufferedOutputStream(new FileOutputStream(trgt));

			try {
				copyStream(in, out);
			} finally {
				out.close();
			}
		}finally {
			in.close();
		}
	}

	/**
	 * Write long, little endian
	 * @param output
	 * @param value
	 * @throws IOException
	 */
	public static void writeLong(OutputStream output, long value) throws IOException {
		byte[] writeBuffer = new byte[8];
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


	/**
	 * Read long, little endian.
	 * @param input
	 * @return
	 * @throws IOException
	 */
	public static long readLong(InputStream input) throws IOException {
		int n = 0;
		byte[] readBuffer = new byte[8];
		while (n < 8) {
			int count = input.read(readBuffer, n , 8-n);
			if (count < 0)
				throw new EOFException();
			n += count;
		}

		return   ((long)readBuffer[7] << 56) +
				 ((long)(readBuffer[6] & 255) << 48) +
				 ((long)(readBuffer[5] & 255) << 40) +
				 ((long)(readBuffer[4] & 255) << 32) +
				 ((long)(readBuffer[3] & 255) << 24) +
				 ((readBuffer[2] & 255) << 16) +
				 ((readBuffer[1] & 255) <<  8) +
				 ((readBuffer[0] & 255)
			);
	}

	/**
	 * Write int, little endian
	 * @param output
	 * @param value
	 * @throws IOException
	 */
	public static void writeInt(OutputStream output, int value) throws IOException {
		byte[] writeBuffer = new byte[4];
		writeBuffer[0] = (byte) (value & 0xFF);
		writeBuffer[1] = (byte) ((value>>8) & 0xFF);
		writeBuffer[2] = (byte) ((value>>16) & 0xFF);
		writeBuffer[3] = (byte) ((value>>24) & 0xFF);
		output.write(writeBuffer,0,4);
	}

	/**
	 * Convert int to byte array, little endian
	 */
	public static byte[] intToByteArray(int value) {
		byte[] writeBuffer = new byte[4];
		writeBuffer[0] = (byte) (value & 0xFF);
		writeBuffer[1] = (byte) ((value>>8) & 0xFF);
		writeBuffer[2] = (byte) ((value>>16) & 0xFF);
		writeBuffer[3] = (byte) ((value>>24) & 0xFF);
		return writeBuffer;
	}

	/**
	 * Read int, little endian
	 * @param input
	 * @return
	 * @throws IOException
	 */
	public static int readInt(InputStream in) throws IOException {
		int ch1 = in.read();
		int ch2 = in.read();
		int ch3 = in.read();
		int ch4 = in.read();
		if ((ch1 | ch2 | ch3 | ch4) < 0)
			throw new EOFException();
		return (ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0);
	}

	/**
	 * Convert byte array to int, little endian
	 * @param value
	 * @return
	 */
	public static int byteArrayToInt(byte[] value){
		return (value[3] << 24) + (value[2] << 16) + (value[1] << 8) + (value[0] << 0);
	}

	/**
	 * @param din
	 * @param bytes
	 * @param listener
	 * @return
	 */
	public static byte[] readBuffer(InputStream input, int length, ProgressListener listener) throws IOException {
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

	public static void printBitsln(long val, int bits) {
		printBits(val, bits);
		System.out.println();
	}

	public static void printBits(long val, int bits) {
		while(bits-- != 0) {
			System.out.print( ((val>>>bits) & 1) !=0 ? '1' : '0');
		}
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

	public static byte readByte(InputStream in) throws IOException {
		int b = in.read();
		if (b < 0) {
			throw new EOFException();
		}
		return (byte)(b&0xFF);
	}

	public static void writeByte(OutputStream out, byte value) throws IOException {
		out.write(value);
	}

	// InputStream might not skip the specified number of bytes. This call makes multiple calls
	// if needed to ensure that the desired number of bytes is actually skipped.
	public static void skip(InputStream in, long n) throws IOException {
		if(n==0) {
			return;
		}

		long totalSkipped = in.skip(n);
		while(totalSkipped<n) {
			totalSkipped += in.skip(n-totalSkipped);
		}
	}

	public static void closeQuietly(Closeable output) {
		if( output == null )
			return;

		try {
			output.close();
		} catch (IOException e) {
		}
	}
}
