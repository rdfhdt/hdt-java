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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Stream that allows efficiently reading single bits by buffering a full byte.
 * @author mario.arias
 *
 */
public class BitInputStream extends DataInputStream {
	int usedBits = 0;
	byte currentValue = 0;
	
	/**
	 * @param arg0
	 */
	public BitInputStream(InputStream arg0) {
		super(arg0);

	}
	
	/* (non-Javadoc)
	 * @see java.io.FilterInputStream#read()
	 */
	@Override
	public int read() throws IOException {
		// When reading a full byte, discard buffer.
		currentValue=0;
		usedBits=0;
		return super.read();
	}
	
	public long readBits(int numbits) throws IOException {
		assert numbits<=64;
		
		long out = 0;
		while(numbits>0) {
			// No bits remaining, read byte.
			if(usedBits<=0) {
				int read = read();
				if(read==-1) {
					return -1;
				}
				currentValue = (byte) read;
				usedBits=8;
			}
			
			if(numbits>8) {
				out |= currentValue<<(numbits-8);
			} else {
				out |= currentValue>>>(8-numbits);
			}
	
			int added = Math.min(numbits,usedBits);
			
			currentValue<<=added;
			numbits-=added;
			usedBits-=added;
		}
		return out;
	}

	/* (non-Javadoc)
	 * @see java.io.FilterInputStream#reset()
	 */
	@Override
	public synchronized void reset() throws IOException {
		currentValue = 0;
		usedBits = 0;
		super.reset();
	}

	/**
	 * @return
	 * @throws IOException 
	 */
	public int readBit() throws IOException {
		int out = 0;
		if(usedBits<=0) {
			int read = read();
			if(read==-1) {
				return -1;
			}
			currentValue = (byte) read;
			usedBits=8;
		}
		out = currentValue & 0x80;
		currentValue <<= 1;
		usedBits--;

		return out>>>7;
	}
}
