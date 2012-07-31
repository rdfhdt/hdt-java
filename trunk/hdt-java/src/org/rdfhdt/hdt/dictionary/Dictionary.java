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

import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.header.Header;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleString;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Interface that specifies the methods for a Dictionary implementation
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
	 * Converts a TripleID to a TripleString
	 * 
	 * @param tripleID
	 *            The Triple to convert from
	 * @return TripleString
	 */
	public TripleString tripleIDtoTripleString(TripleID tripleID);

	/**
	 * Returns the string for a given id
	 * 
	 * @param id
	 *            The id to convert to string
	 * @param position
	 *            TriplePosition of the id in the dictionary
	 * @return String
	 */
	public CharSequence idToString(int id, TripleComponentRole position);

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
	
	public void load(Dictionary other, ProgressListener listener);

	/**
	 * Returns the number of elements in the dictionary
	 * 
	 * @return int
	 */
	public long getNumberOfElements();

	public long size();
	
	public long getNsubjects();
	
	public long getNpredicates();
	
	public long getNobjects();
	
	public long getNshared();
	
	public long getMaxID();
	
	public long getMaxSubjectID();
	
	public long getMaxPredicateID();
	
	public long getMaxObjectID();
	
	public void populateHeader(Header header, String rootNode);
	
	public String getType(); 
	
	public DictionarySection getSubjects();
	
	public DictionarySection getPredicates();
	
	public DictionarySection getObjects();
	
	public DictionarySection getShared();
}
