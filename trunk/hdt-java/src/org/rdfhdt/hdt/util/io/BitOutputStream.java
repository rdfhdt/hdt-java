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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Stream that allows writing single bits to a stream.
 * It caches the results in a long, it flushes it when full, or when flush() is called.
 * flush() must be called before writing any other datatype.
 * @author mario.arias
 *
 */
public class BitOutputStream extends DataOutputStream {
	private static final int W = 64;
	
	// Cached bits (Aligned left) 
	// First bit is the leftmost (i.e  (1<<63) )
	// Bits are appended to the right of the used bits. i.e. 101000(3) + 1 = 101100(4)
	long tmpValue = 0;
	
	// Number of valid bits in the buffer.
	int usedBits = 0;
	
	/**
	 * @param out
	 */
	public BitOutputStream(OutputStream out) {
		super(out);
	}
	/**
	 * Write a few bits to the stream. The bits in value are right-aligned. 
	 * E.g. To write four bits, you should pass value = 0000001111 and numbits=4; 
	 * @param value 
	 * @param numbits
	 * @return
	 * @throws IOException
	 */
	public int writeBits(long value, int numbits) throws IOException {
		if(numbits>=(W-usedBits)) {
			// Two words
			tmpValue |= value << usedBits >>> usedBits;
			this.writeLong(tmpValue);
			tmpValue = (value<<(W-usedBits-numbits));
			usedBits = numbits - W + usedBits;
		} else {
			tmpValue |= (value<<(W-usedBits-numbits));
			usedBits += numbits;
		}
		return numbits;
	}
	
	public void writeBit(boolean value) throws IOException {
		usedBits++;
		tmpValue |= (value ? 1 : 0) << (W-usedBits);
	}
	
	/**
	 * Flush remaining bits to the output. 
	 * WARNING: Must be called before writing any datatype other than bits.
	 * 
	 */
	public void flush() throws IOException {
		// Aling by byte instead of long
		while(usedBits>0) {
			this.writeByte((byte) (tmpValue>>56) & 0xFF);
			usedBits-=8;
			tmpValue <<= 8;
		}
		tmpValue = 0;
		usedBits = 0;
		super.flush();
	}	

}
