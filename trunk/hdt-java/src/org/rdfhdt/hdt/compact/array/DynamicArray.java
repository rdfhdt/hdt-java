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

package org.rdfhdt.hdt.compact.array;

/**
 * @author mario.arias
 *
 */
public interface DynamicArray extends StaticArray {
	/**
	 * Set a new value at the specified position.
	 * @param index
	 * @param value
	 */
	public void set(long index, long value);
	
	/**
	 * Append a new value after the last position, increasing the number of elements by one.
	 * @param value
	 */
	public void append(long value);
	
	/**
	 * Trim the internal data structure to fit the actual number of elements.
	 */
	void trimToSize();
	
	/**
	 * Trim the internal data structure to fit the actual number of elements. 
	 * Use advanced algorithm to reduce the size to the minimum, even if it is costly.
	 */
	void aggresiveTrimToSize();
}
