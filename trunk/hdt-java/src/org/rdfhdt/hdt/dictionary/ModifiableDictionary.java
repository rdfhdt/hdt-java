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
import org.rdfhdt.hdt.triples.ModifiableTriples;

/**
 * @author mario.arias
 *
 */
public interface ModifiableDictionary extends Dictionary {
	
	/**
	 * To be executed at the start of the processing
	 * 
	 */
	public void startProcessing();
	
	/**
	 * To be executed at the end of the processing
	 * 
	 */
	public void endProcessing();

	/**
	 * Inserts a string in the dictionary in a position
	 * 
	 * @param str
	 *            The string to be inserted
	 * @param position
	 *            TriplePosition to be inserted in
	 */
	public int insert(CharSequence str, TripleComponentRole position);

	/**
	 * Reorganizes the dictionary (Extract shared SO, sort sections)
	 * 
	 */
	public void reorganize();
	
	/**
	 * Reorganizes the dictionary (Extract shared SO, sort sections)
	 * and updates the IDs of the triples.
	 */
	public void reorganize(ModifiableTriples triples);
	
	/**
	 * Empty all the strings of the dictionary.
	 */
	public void clear();
}
