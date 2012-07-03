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
package hdt.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BitUtil {
	/**
	 * Number of bits needed to store up to n
	 * @param n
	 * @return
	 */
	public static final int log2(long n) {
        int b = 0;
        while(n!=0) {
                b++;
                n>>>=1;
        }
        return b;
	}
	
	public static final long maxVal(int numbits) {
		return ~(~0L<<numbits);
	}
	
	public static final long readLowerBitsByteAligned(long numbits, InputStream in) throws IOException {
        int bitsRead = 0;
        long value = 0;
        while(bitsRead<numbits) {
                long readByte = in.read();
                value |= readByte<<bitsRead;
                bitsRead+=8;
        }
        return value;
}

	public static final void writeLowerBitsByteAligned(long value, long numbits, OutputStream out) throws IOException {
		while(numbits>0) {
			out.write((int)(value & 0xFF));
			value>>>=8;
        	numbits-=8;
		}
	}
	
	public static final int select1(long value, int rank) {
        int bitpos=0;
        while(rank>0 && value!=0) {
                //System.out.println("\t"+Long.toBinaryString(blockData)+ " Bitpos: "+bitpos+ " Countdown: "+countdown);
                if( (value & 1L)!=0) {
                        rank--;
                }
                bitpos++;
                value>>>=1;
        }
        return bitpos;
	}
}
