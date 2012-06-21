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

import java.io.IOException;
import java.io.InputStream;


/**
 * CharSequence implementation suitable for appending or replacing the suffix of the string.
 * It grows as necessary but it never returns that size back.
 * 
 * @author mck
 *
 */
public class ReplazableString implements CharSequence {
	byte [] buffer;
	int used, reserved;
	/**
	 * 
	 */
	public ReplazableString() {
		reserved = 10;
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

	/* (non-Javadoc)
	 * @see java.lang.CharSequence#subSequence(int, int)
	 */
	@Override
	public CharSequence subSequence(int start, int end) {
		if (start < 0 || end >= (this.length())) {
			throw new IllegalArgumentException("Illegal range " +
					start + "-" + end + " for sequence of length " + length());
		}
		byte [] newdata = new byte[end-start];
		System.arraycopy(buffer, start, newdata, 0, end-start);
		return new ReplazableString(newdata);
	}
	
	public String toString() {
		return new String(buffer, 0, used);
	}
}
