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
package hdt.util.string;

import hdt.exceptions.NotImplementedException;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author mck
 *
 */
public class ByteStringUtil {
	public static int longestCommonPrefix(byte [] buf1, int off1, byte [] buf2, int off2) {
		int len1 = buf1.length;
		int len2 = buf2.length;
		int p1 = off1;
		int p2 = off2;
		while(
				(p1<len1) &&
        	    (p2<len2) &&
        	    (buf1[p1]!=0) &&
        	    (buf2[p2]!=0) && 
        	    (buf1[p1] == buf2[p2])
        	 ){
			p1++;
			p2++;
		}
		return p1-off1;
	} 
	
	public static int longestCommonPrefix(CharSequence str1, CharSequence str2) {
		return longestCommonPrefix(str1, str2, 0);
	}
	
	public static int longestCommonPrefix(CharSequence str1, CharSequence str2, int from) {
		int len = Math.min(str1.length(), str2.length());
		int delta = from;
		while(delta<len && str1.charAt(delta)==str2.charAt(delta)) {
			delta++;
		}
		return delta-from;
	}
	
	public static int strcmp(byte [] buff1, int off1, byte [] buff2, int off2) {
		byte a,b;
		int diff;
		int p1 = off1;
		int p2 = off2;	
		int n = Math.min(buff1.length-off1, buff2.length-off2);

		if (n == 0) {
			return 0;
		}
		
		do {
			a = buff1[p1++];
			b = buff2[p2++];
			diff = a-b;
		} while ( (diff==0) && (a!=0) && (--n != 0) );
		
		if(n==0 && diff==0) {
			if(buff1.length-p1!=0 && buff1[p1]!=0){
				// Still remaining in string one, second is shorter
				return 1;
			} 
			if(buff2.length-p2!=0 && buff2[p2]!=0){
				// Still remaining in string two, first is shorter.
				return -1;
			}
			return 0;
		}
		
		return diff;
	}
	
	public static int strlen(byte [] buff, int off) {
		int len = buff.length;
		int pos = off;
		while(pos<len && buff[pos]!=0) {
			pos++;
		}
		return pos-off;
	}
	
	public static int strlen(ByteBuffer pos) {
		pos.mark();
		int len=0;
		while(pos.hasRemaining() && pos.get() != 0) {
			len++;
		}
		pos.reset();
		return len;
	}
	
	public static int strcmp(CharSequence str, byte [] text, int offset) {
		if(str instanceof CompactString) {
			return strcmp(((CompactString) str).getData(), 0, text, offset);
		}
		if(str instanceof String) {
			return strcmp(((String) str).getBytes(), 0, text, offset);
		}
		throw new NotImplementedException();
	}
	
	public static String asString(byte [] buff, int offset) {
		int len = strlen(buff, offset);
		return new String(buff, offset, len);
	}
	
	public static int strcmp(CharSequence str, ByteBuffer buffer) {
		byte [] buf=null;
		
		if(str instanceof CompactString) {
			buf = ((CompactString) str).getData();
		}else if(str instanceof String) {
			buf = ((String) str).getBytes();
		} else {
			throw new NotImplementedException();
		}
		
		return strcmp(ByteBuffer.wrap(buf), buffer);
	}
	
	public static final int strcmp(ByteBuffer a, ByteBuffer b) {
		int x=1;
		int y=1;
		while(a.hasRemaining() && b.hasRemaining() && x!=0 && y!=0 && x==y) {
			x = a.get();
			y = b.get();
		}
		return y-x;
	}
	
	public static int append(CharSequence str, int start, byte [] buffer, int bufpos) {
		byte [] bytes;
		int len;
		if(str instanceof String) {
			bytes = ((String) str).getBytes();
			len = bytes.length;
		} else if(str instanceof CompactString) {
			bytes = ((CompactString) str).getData();
			len = bytes.length;
		} else if(str instanceof ReplazableString) {
			bytes = ((ReplazableString) str).getBuffer();
			len = ((ReplazableString) str).used;
		} else {
			throw new NotImplementedException();
		}
		System.arraycopy(bytes, start, buffer, bufpos, len - start);
		return len - start;		
	}
	
	public static int append(DataOutput out, CharSequence str, int start) throws IOException {
		byte [] bytes;
		int len;
		if(str instanceof String) {
			bytes = ((String) str).getBytes();
			len = bytes.length;
		} else if(str instanceof CompactString) {
			bytes = ((CompactString) str).getData();
			len = bytes.length;
		} else if(str instanceof ReplazableString) {
			bytes = ((ReplazableString) str).getBuffer();
			len = ((ReplazableString) str).used;
		} else {
			throw new NotImplementedException();
		}
		out.write(bytes, start, len - start);
		return len - start;		
	}
	
	/**
	 * Add numbits of value to buffer at pos
	 * @param buffer
	 * @param pos Position in bits.
	 * @param value Value to be added
	 * @param numbits numbits of value to be added.
	 */
	public static void append(long [] buffer, long pos, long value, int numbits) {
		final int W = 64;
		
		int i=(int)(pos/W), j=(int)(pos%W);
		
		// NOTE: Assumes it was empty before. 
		// Otherwise we would need to clean the bits beforehand
		if(numbits>(W-j)) {
			// Two words
			buffer[i] |= value << j >>> j;
			buffer[i+1] |= (value<<(W-j-numbits));
		} else {
			buffer[i] |= (value<<(W-j-numbits));
		}
	}
}
