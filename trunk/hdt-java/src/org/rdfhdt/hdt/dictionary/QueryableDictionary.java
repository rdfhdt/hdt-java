/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/dictionary/Dictionary.java $
 * Revision: $Rev: 45 $
 * Last modified: $Date: 2012-07-31 12:00:35 +0100 (Tue, 31 Jul 2012) $
 * Last modified by: $Author: mario.arias $
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
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleString;

/**
 * Interface that specifies the methods for a dictionary that can be queried
 * for a mapping of a part of a triple from String to ID or ID to String.
 * 
 * @author Eugen
 *
 */
public interface QueryableDictionary extends Dictionary {

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
	 * Fills the header with information from the dictionary
	 */
	public void populateHeader(Header header, String rootNode);
	
	/**
	 * Returns the type of the dictionary (the way it is written onto file/held in memory)
	 * @return
	 */
	public String getType();

}
