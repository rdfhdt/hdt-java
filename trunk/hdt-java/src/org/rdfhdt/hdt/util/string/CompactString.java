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

import org.rdfhdt.hdt.exceptions.NotImplementedException;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;

/**
 * Implementation of CharSequence that uses only one byte per character to save memory.
 * The String length is defined by the buffer size.
 * @author mario.arias
 *
 */
public class CompactString implements CharSequence, Serializable, Comparable<CompactString> {

	private static final long serialVersionUID = 6789858615261959413L;
	private static final String ENCODING = "UTF-8";
	
	// String buffer as bytes.
	final byte[] data;
	
	// Cached hash value.
	private int hash=0;
	
	public static final CompactString EMPTY = new CompactString();
	
	private CompactString() {
		this.data = new byte[0];
	}
	
	public CompactString(CharSequence str) {
		this(str,0, str.length());
	}
	
	public CompactString(CharSequence str, int ini, int end) {
		if(str instanceof CompactString) {
			CompactString comp = (CompactString) str;
			data = new byte[Math.min(end-ini+1,comp.data.length)];
			System.arraycopy(comp.data, ini, data, 0, data.length);
		} else if(str instanceof ReplazableString) {
			ReplazableString rep = (ReplazableString) str;
			data = new byte[Math.min(end-ini+1,rep.used)];
			System.arraycopy(rep.buffer, ini, data, 0, data.length);
		} else {
			try {
				data = str.toString().substring(ini, end).getBytes(ENCODING);
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException("Unexpected: " + ENCODING + " not supported!");
			}
		}
	}
	
	public byte [] getData() {
		return data;
	}

	private CompactString(byte[] data) {
		this.data = data;
	}

	public char charAt(int index) {
		int ix = index;
		if (ix >= data.length) {
			throw new StringIndexOutOfBoundsException("Invalid index " + index + " length " + length());
		}
		return (char) (data[ix] & 0xff);
	}

	public int length() {
		return data.length;
	}

	public CharSequence subSequence(int start, int end) {
		if (start < 0 || end > (this.length()) || (end-start)<0) {
			throw new IllegalArgumentException("Illegal range " +
					start + "-" + end + " for sequence of length " + length());
		}
		byte [] newdata = new byte[end-start];
		System.arraycopy(data, start, newdata, 0, end-start);
		return new CompactString(newdata);
	}

	public String toString() {
		try {
			return new String(data, 0, data.length, ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Unexpected: " + ENCODING + " not supported");
		}
	}
	
	public int hashCode() {
		// FNV Hash function: http://isthe.com/chongo/tech/comp/fnv/
		if(hash==0){
			hash = (int) 2166136261L; 				
			int i = data.length;
					
			while(i-- != 0) {
				hash = 	(hash * 16777619) ^ data[i];
			}
		}
		return hash;
	}
	
	public boolean equals(Object o) {
		if(this==o) {
			return true;
		}
		if(o instanceof CompactString) {
			CompactString cmp = (CompactString) o;
			if(data.length!=cmp.data.length) {
				return false;
			}
			
			// Byte by byte comparison
			int i = data.length;
			while(i-- != 0) {
				if(data[i]!=cmp.data[i]) {
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
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(CompactString o) {
		int len1 = this.data.length;
        int len2 = o.data.length;
        int n = Math.min(len1, len2);

        int k = 0;
        while (k < n) {
            byte c1 = this.data[k];
            byte c2 = o.data[k];
            if (c1 != c2) {
                return c1 - c2;
            }
            k++;
        }
        return len1 - len2;
	}
}
