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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleString;


/**
 * Interface that specifies the basic methods for any Dictionary implementation
 * 
 */
public interface Dictionary {
	
	/**
	 * Converts a TripleString to a TripleID
	 * 
	 * @param tripleString
	 *            The Triple to convert from
	 * @return TripleID
	 */
	public TripleID tripleStringtoTripleID(TripleString tripleString);
	
	/**
	 * Returns the id for a given string
	 * 
	 * @param str
	 *            The string to convert to id
	 * @param position
	 *            TriplePosition of the string in the dictionary
	 * @return int
	 */
	public int stringToId(CharSequence str, TripleComponentRole position);
	
	/**
	 * Loads all information from another dictionary into this dictionary.
	 */
	public void load(Dictionary other, ProgressListener listener);
	
	/**
	 * Saves the dictionary to a OutputStream
	 */
	public void save(OutputStream output, ControlInformation ci, ProgressListener listener) throws IOException;

	/**
	 * Loads a dictionary from a InputStream
	 * 
	 * @param input
	 *            InputStream to load the dictionary from
	 * @throws IOException
	 */
	public void load(InputStream input, ControlInformation ci, ProgressListener listener) throws IOException;

	/**
	 * Returns the number of elements in the dictionary
	 */
	public long getNumberOfElements();

	/**
	 * Return the combined size of the sections of the dictionary (in bytes)
	 */
	public long size();
	
	/**
	 * Returns the number of subjects in the dictionary.
	 */
	public long getNsubjects();
	
	/**
	 * Returns the number of predicates in the dictionary.
	 */
	public long getNpredicates();
	
	/**
	 * Returns the number of objects in the dictionary.
	 */
	public long getNobjects();
	
	/**
	 * Returns the number of subjects/objects in the dictionary.
	 */
	public long getNshared();
	
	/**
	 * Returns the biggest ID that a subject or an objects in the dictionary has
	 */
	public long getMaxID();
	
	/**
	 * Returns the biggest ID that a subject in the dictionary has
	 */
	public long getMaxSubjectID();

	/**
	 * Returns the biggest ID that a predicate in the dictionary has
	 */
	public long getMaxPredicateID();
	
	/**
	 * Returns the biggest ID that an object in the dictionary has
	 */
	public long getMaxObjectID(); 
	
	public DictionarySection getSubjects();
	
	public DictionarySection getPredicates();
	
	public DictionarySection getObjects();
	
	public DictionarySection getShared();
}
