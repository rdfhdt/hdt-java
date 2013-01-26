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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.rdfhdt.hdt.exceptions.NotImplementedException;

/**
 * @author mario.arias
 *
 */
public class ByteStringUtil {
	
	/**
	 * For use in the project when using String.getBytes() and making Strings from byte[]
	 */
	public static final Charset STRING_ENCODING = Charset.forName("UTF-8");
	
	public static final String asString(byte [] buff, int offset) {
		int len = strlen(buff, offset);
		return new String(buff, offset, len);
	}
	
	public static final String asString(ByteBuffer buff, int offset) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int i=0;
		int n=buff.capacity()-offset;
		while(i<n) {
			byte b = buff.get(offset+i);
			if(b==0) {
				break;
			}
			out.write(b);
			i++;
		}
		return new String(out.toByteArray(), STRING_ENCODING);
	}
	
	public static final int strlen(byte [] buff, int off) {
		int len = buff.length;
		int pos = off;
		while(pos<len && buff[pos]!=0) {
			pos++;
		}
		return pos-off;
	}
	
	public static final int strlen(ByteBuffer buf, int base) {
		int len=0;
		int n=buf.capacity()-base;
		while(len<n) {
			if(buf.get(base+len)==0) {
				return len;
			}
			len++;
		}
		throw new IllegalArgumentException("Buffer not Null-Terminated");
	}
	
	public static final int longestCommonPrefix(CharSequence str1, CharSequence str2) {
		return longestCommonPrefix(str1, str2, 0);
	}
	
	public static final int longestCommonPrefix(CharSequence str1, CharSequence str2, int from) {
		int len = Math.min(str1.length(), str2.length());
		int delta = from;
		while(delta<len && str1.charAt(delta)==str2.charAt(delta)) {
			delta++;
		}
		return delta-from;
	}
	
	public static final int strcmp(byte [] buff1, int off1, byte [] buff2, int off2) {
		int len = Math.min(buff1.length-off1, buff2.length-off2);
		
		if(len==0) {
			// Empty string is smaller than any string.
			return (buff2.length-off2)-(buff1.length-off1);
		}
		return strcmp(buff1, off1, buff2, off2, len);
	}
	
	public static final int strcmp(byte [] buff1, int off1, byte [] buff2, int off2, int n) {
		byte a,b;
		int diff;
		int p1 = off1;
		int p2 = off2;	
	
		if (n == 0) {
			return 0;
		}
		
		// Check whether one of the buffers is already at the end
		if(p1 < buff1.length && p2==buff2.length) {
			return 1;
		} else if(p1==buff1.length && p2<buff2.length) {
			return -1;
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
	
	public static int strcmp(CharSequence str, byte [] text, int offset) {
		if(str instanceof CompactString) {
			return strcmp(((CompactString) str).getData(), 0, text, offset);
		}
		if(str instanceof String) {
			return strcmp(((String) str).getBytes(ByteStringUtil.STRING_ENCODING), 0, text, offset);
		}
		if(str instanceof ReplazableString) {
			return strcmp(((ReplazableString) str).buffer, 0, text, offset, ((ReplazableString) str).used);
		}
		throw new NotImplementedException();
	}
	
	public static int strcmp(CharSequence str, ByteBuffer buffer, int offset) {
		byte [] buf=null;
		int len;
		
		if(str instanceof CompactString) {
			buf = ((CompactString) str).getData();
			len = buf.length;
		} else if(str instanceof String) {
			buf = ((String) str).getBytes(ByteStringUtil.STRING_ENCODING);
			len = buf.length;
		} else if(str instanceof ReplazableString) {
			buf = ((ReplazableString) str).buffer;
			len = ((ReplazableString) str).used;
		} else {
			throw new NotImplementedException();
		}

		// FIXME: Some way to avoid this copy?
		ByteBuffer bbuf = ByteBuffer.allocate(len+1);
		bbuf.put(buf);
		bbuf.put((byte)0);
		
		return strcmp(bbuf, 0, buffer, offset);
	}
	
	public static final int strcmp(ByteBuffer a, int abase, ByteBuffer b, int bbase) {
		int i=0;
		int n = Math.min(a.capacity()-abase, b.capacity()-bbase);
		while(i<n) {
			byte v1 = a.get(abase+i);
	        byte v2 = b.get(bbase+i);

	        if(v1==0) {
	        	if(v2==0) {
	        		return 0;
	        	} else {
	        		return -1;
	        	}
	        }
	        if(v2==0) {
        		return +1;
	        }

	        if(v1!=v2) {
	        	if (v1 < v2) {
	        		return -1;
	        	} else {
	        		return +1;	        	
	        	}
	        }
	        i++;
	    }
	    throw new IllegalArgumentException("One of the buffers not NULL-Terminated when comparing strings.");
	}
	
	public static final int append(CharSequence str, int start, byte [] buffer, int bufpos) {
		byte [] bytes;
		int len;
		if(str instanceof String) {
			bytes = ((String) str).getBytes(ByteStringUtil.STRING_ENCODING);
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

	public static final int append(OutputStream out, CharSequence str, int start) throws IOException {
		byte [] bytes;
		int len;
		if(str instanceof String) {
			bytes = ((String) str).getBytes(ByteStringUtil.STRING_ENCODING);
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
	
}
