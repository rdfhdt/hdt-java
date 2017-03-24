/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/util/string/ReplazableString.java $
 * Revision: $Rev: 191 $
 * Last modified: $Date: 2013-03-03 11:41:43 +0000 (dom, 03 mar 2013) $
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
 */

package org.rdfhdt.hdt.util.string;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.rdfhdt.hdt.exceptions.NotImplementedException;


/**
 * CharSequence implementation suitable for appending or replacing the suffix of the string.
 * It grows as necessary but it never returns that size back.
 * 
 * @author mario.arias
 *
 */
public final class ReplazableString implements CharSequence, Comparable<ReplazableString> {
	
	byte [] buffer;
	int used;

	public ReplazableString() {
		this(128);
	}

	public ReplazableString(int initialCapacity) {
		buffer = new byte[initialCapacity];
		used=0;
	}
	
	private ReplazableString(byte [] buffer) {
		this.buffer = buffer;
		this.used = buffer.length;
	}
	
	public byte [] getBuffer() {
		return buffer;
	}
	
	private void ensureSize(int size) {
		if(size>buffer.length) {
			buffer = Arrays.copyOf(buffer, Math.max(size, buffer.length * 2));
		}
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
	
	public void replace2(InputStream in, int pos) throws IOException {
		used = pos;
		
		while(true) {
			int value = in.read();
			if(value==-1) {
				throw new IllegalArgumentException("Was reading a string but stream ended before finding the null terminator");
			}
			if(value==0) {
				break;
			}
			if(used>=buffer.length) {
				buffer = Arrays.copyOf(buffer, buffer.length*2);
			}
			buffer[used++] = (byte)(value&0xFF);
		}
	}
	
	private static final int READ_AHEAD = 1024;
	
	public void replace(InputStream in, int pos) throws IOException {
		
		if(!in.markSupported()) {
			replace2(in,pos);
			return;
		}
		used = pos;

		
		while(true) {
			if(used+READ_AHEAD>buffer.length) {
				buffer = Arrays.copyOf(buffer, Math.max(buffer.length*2, used+READ_AHEAD));
			}
			in.mark(READ_AHEAD);
			int numread = in.read(buffer, used, READ_AHEAD);
			if(numread==-1){
				throw new IllegalArgumentException("Was reading a string but stream ended before finding the null terminator");
			}
			
			int i=0;
			while(i<numread) {
//				System.out.println("Char: "+buffer[used+i]+"/"+(char)buffer[used+i]);
				if(buffer[used+i]==0) {
					in.reset();
					in.skip(i+1);
					used+=i;
					return;
				}
				i++;
			}
			used+=numread;
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
			if(used>=buffer.length) {
				buffer = Arrays.copyOf(buffer, buffer.length*2);
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
		return (char)(buffer[index] & 0xFF);
	}

	/* (non-Javadoc)
	 * @see java.lang.CharSequence#length()
	 */
	@Override
	public int length() {
		return used;
	}

	@Override
	public int hashCode() {
		// FNV Hash function: http://isthe.com/chongo/tech/comp/fnv/
		int hash = (int) 2166136261L; 				
		int i = used;

		while(i-- != 0) {
			hash = 	(hash * 16777619) ^ buffer[i];
		}

		return hash;
	}

	@Override
	public boolean equals(Object o) {
		if(o==null) {
			return false;
		}
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
		} else if(o instanceof ReplazableString) {
			ReplazableString cmp = (ReplazableString) o;
			if(this.used!=cmp.used) {
				return false;
			}
			
			// Byte by byte comparison
			int i = this.used;
			while(i-- != 0) {
				if(buffer[i]!=cmp.buffer[i]) {
					return false;
				}
			}
			return true;
		} else if (o instanceof CharSequence) {
			CharSequence other = (CharSequence) o;
			return length()==other.length() && CharSequenceComparator.getInstance().compare(this, other)==0;
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

	@Override
	public String toString() {
		return new String(buffer, 0, used, ByteStringUtil.STRING_ENCODING);
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ReplazableString other) {
        int n = Math.min(used, other.used);

        int k = 0;
        while (k < n) {
            int c1 = this.buffer[k] & 0xFF;
            int c2 = other.buffer[k] & 0xFF;
            if (c1 != c2) {
                return c1 - c2;
            }
            k++;
        }
        return used - other.used;
	}
	
	public CharSequence getDelayed() {
		return new DelayedString(this);
	}
}
