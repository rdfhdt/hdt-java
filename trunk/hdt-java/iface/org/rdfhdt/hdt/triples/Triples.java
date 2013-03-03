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

package org.rdfhdt.hdt.triples;

import org.rdfhdt.hdt.header.Header;

/**
 * Interface for Triples implementation.
 * 
 */
public interface Triples {
	/**
	 * Iterates over all triples. Equivalent to this.search(new TripleID());
	 * @return
	 */
	public IteratorTripleID searchAll();
	
	/**
	 * Iterates over all triples that match the pattern.
	 * 
	 * @param pattern
	 *            The pattern to match against
	 * @return IteratorTripleID 
	 * 
	 */
	public IteratorTripleID search(TripleID pattern);
	
	/**
	 * Returns the total number of triples
	 * 
	 * @return int
	 */
	public long getNumberOfElements();

	/**
	 * Returns the size in bytes of the internal representation
	 * 
	 * @return int
	 */
	public long size();

	/**
	 * Populates HDT Header with all information relevant to this Triples under a RDF root node.
	 * @param head
	 * @param rootNode
	 */
	public void populateHeader(Header head, String rootNode);
	
	/**
	 * Returns a unique identifier of this Triples Implementation
	 * @return
	 */
	public String getType();
}