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

import org.rdfhdt.hdt.listener.ProgressListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * The array interface represents compact sequences of integers.
 * 
 */
public interface StaticArray {

	/**
	 * Adds an element to the array
	 * 
	 * @param element
	 *            The element to be added to the array
	 */
	public abstract void add(Iterator<Long> elements);

	/**
	 * Gets the element in a specific position
	 * 
	 * @param position
	 *            The position of the element to be returned
	 * @return int
	 */
	public abstract long get(long position);

	/**
	 * Gets the total number of elements in the array
	 * 
	 * @return int
	 */
	public abstract long getNumberOfElements();
	
	public abstract long size();

	/**
	 * Saves the array to an OutputStream
	 * 
	 * @param output
	 *            The OutputStream to be saved to
	 * @throws IOException
	 */
	public abstract void save(OutputStream output, ProgressListener listener) throws IOException;

	/**
	 * Loads a array from an InputStream
	 * 
	 * @param input
	 *            The InputStream to load from
	 */
	public abstract void load(InputStream input, ProgressListener listener) throws IOException;
	
	/**
	 * Type identifier of this Stream.
	 * @return
	 */
	public String getType();
}
