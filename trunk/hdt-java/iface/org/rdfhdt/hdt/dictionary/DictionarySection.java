package org.rdfhdt.hdt.dictionary;
/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/dictionary/DictionarySection.java $
 * Revision: $Rev: 58 $
 * Last modified: $Date: 2012-08-26 00:31:15 +0100 (dom, 26 ago 2012) $
 * Last modified by: $Author: simpsonim13@gmail.com $
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


import java.util.Iterator;

/**
 * Interface that specifies the basic methods for a dictionary section
 * 
 * @author mario.arias, Eugen
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
	 * Find the String associated to a given ID
	 * @param pos
	 * @return
	 */
	public CharSequence extract(int pos);
	
	/**
	 * Size in bytes of the strings held in the dictionary section.
	 */
	public long size();
	
	/**
	 * Number of entries in the dictionary section.
	 */
	public int getNumberOfElements();
	
	/**
	 * Iterator over all entries in the dictionary, sorted lexicographically.
	 */
	public Iterator<? extends CharSequence> getSortedEntries();
	
}
