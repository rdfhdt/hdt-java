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

package org.rdfhdt.hdt.util.string;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.rdfhdt.hdt.exceptions.NotImplementedException;


/**
 * CharSequence implementation suitable for appending or replacing the suffix of the string.
 * It grows as necessary but it never returns that size back.
 * 
 * @author mario.arias
 *
 */
public class ReplazableString implements CharSequence {
	
	byte [] buffer;
	int used, reserved;
	/**
	 * 
	 */
	public ReplazableString() {
		reserved = 100;
		buffer = new byte[reserved];
		used=0;
	}
	
	private ReplazableString(byte [] buffer) {
		this.buffer = buffer;
		this.used = this.reserved = buffer.length;
	}
	
	public byte [] getBuffer() {
		return buffer;
	}
	
	private void ensureSize(int size) {
		if(size>reserved) {
			resizeArray(size);
		}
	}
	
	private void resizeArray(int newSize) {
		byte [] newData = new byte[newSize];
		System.arraycopy(buffer, 0, newData, 0, Math.min(reserved, newSize));
		buffer = newData;
		reserved = newSize;
	}
	
	public void append(byte [] data, int offset, int len) {
		this.replace(used, data, offset, len);
	}
	
	public void append(CharSequence other) {
		ensureSize(this.used+other.length());
		for(int i=0;i<other.length();i++) {
			buffer[this.used+i] = (byte) other.charAt(i);
		}
		used+=other.length();
	}
	
	
	public void replace(int pos, byte [] data, int offset, int len) {
		ensureSize(pos+len);
		System.arraycopy(data, offset, buffer, pos, len);
		used = pos+len;
	}
	
	public void replace(InputStream in, int pos, int len) throws IOException {
		ensureSize(pos+len);
		in.read(buffer, pos, len);
		used = pos+len;
	}
	
	public void replace(ByteBuffer in, int pos, int len) throws IOException {
		ensureSize(pos+len);
		in.get(buffer, pos, len);
		used = pos+len;
	}
	
	public void replace(InputStream in, int pos) throws IOException {
		used = pos;
		
		while(true) {
			int value = in.read();
			if(value==-1) {
				throw new IllegalArgumentException("Was reading a string but stream ended before finding the null terminator");
			}
			if(value==0) {
				break;
			}
			if(pos>=reserved) {
				ensureSize(reserved*2);
			}
			buffer[pos++] = (byte)(value&0xFF);
			used++;
		}
	}
	
	public void replace(ByteBuffer in, int pos) throws IOException {
		used = pos;
		
		int n = in.capacity()-in.position();
		while(n-- != 0) {
			byte value = in.get();
			if(value==0) {
				return;
			}
			if(used>=reserved) {
				ensureSize(reserved*2);
			}
			buffer[used++] = value;
		}
		throw new IllegalArgumentException("Was reading a string but stream ended before finding the null terminator");				
	}
	
	/* (non-Javadoc)
	 * @see java.lang.CharSequence#charAt(int)
	 */
	@Override
	public char charAt(int index) {
		return (char)buffer[index];
	}

	/* (non-Javadoc)
	 * @see java.lang.CharSequence#length()
	 */
	@Override
	public int length() {
		return used;
	}
	
	public int hashCode() {
		// FNV Hash function: http://isthe.com/chongo/tech/comp/fnv/
		int hash = (int) 2166136261L; 				
		int i = used;

		while(i-- != 0) {
			hash = 	(hash * 16777619) ^ buffer[i];
		}

		return hash;
	}
	
	public boolean equals(Object o) {
		if(this==o) {
			return true;
		}
		if(o instanceof CompactString) {
			CompactString cmp = (CompactString) o;
			if(buffer.length!=cmp.data.length) {
				return false;
			}
			
			// Byte by byte comparison
			int i = buffer.length;
			while(i-- != 0) {
				if(buffer[i]!=cmp.data[i]) {
					return false;
				}
			}
			return true;
		} else if (o instanceof CharSequence) {
			CharSequence other = (CharSequence) o;
			return (length()==other.length() && CharSequenceComparator.instance.compare(this, other)==0);
		}
		throw new NotImplementedException();
	}


	/* (non-Javadoc)
	 * @see java.lang.CharSequence#subSequence(int, int)
	 */
	@Override
	public CharSequence subSequence(int start, int end) {
		if (start < 0 || end > (this.length()) || (end-start)<0) {
			throw new IllegalArgumentException("Illegal range " +
					start + "-" + end + " for sequence of length " + length());
		}
		byte [] newdata = new byte[end-start];
		System.arraycopy(buffer, start, newdata, 0, end-start);
		return new ReplazableString(newdata);
	}
	
	public String toString() {
		return new String(buffer, 0, used, ByteStringUtil.STRING_ENCODING);
	}
}
