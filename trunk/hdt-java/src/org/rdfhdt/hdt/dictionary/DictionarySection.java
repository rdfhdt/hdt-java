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

package org.rdfhdt.hdt.dictionary;

import org.rdfhdt.hdt.listener.ProgressListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * @author mario.arias
 *
 */
public interface DictionarySection {
	/**
	 * Find a String and return its ID.
	 * @param s
	 * @return
	 */
	public int locate(CharSequence s);
	
	/**
	 * Find the String associated to an ID
	 * @param pos
	 * @return
	 */
	public CharSequence extract(int pos);
	
	/**
	 * Size in bytes of the structure
	 * @return
	 */
	public long size();
	
	/**
	 * Number of entries in the dictionary.
	 * @return
	 */
	public int getNumberOfElements();
	
	/**
	 * Iterator over all entries in the dictionary, sorted lexicographically.
	 * @return
	 */
	public Iterator<? extends CharSequence> getSortedEntries();
	
	/**
	 * Serialize dictionary to a stream.
	 * @param output
	 * @param listener
	 * @throws IOException
	 */
	public void save(OutputStream output, ProgressListener listener) throws IOException;
	
	/**
	 * Load dictionary from a stream.
	 * @param input
	 * @param listener
	 * @throws IOException
	 */
	public void load(InputStream input, ProgressListener listener) throws IOException;
	
	/**
	 * Load entries from another dictionary.
	 * @param other
	 * @param listener
	 */
	public void load(DictionarySection other, ProgressListener listener);
}
