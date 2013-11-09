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
 */

package org.rdfhdt.hdtjena.solver;

import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdtjena.NodeDictionary;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.NiceIterator;

/**
 * @author mario.arias
 *
 */
public class HDTJenaIterator extends NiceIterator<Triple> {

	private final NodeDictionary nodeDictionary;
	private final IteratorTripleID iterator;

	public HDTJenaIterator(NodeDictionary nodeDictionary, IteratorTripleID iterator) {
		this.nodeDictionary = nodeDictionary;
		this.iterator = iterator;
	}

	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}
	
	@Override
	public Triple next() {

		TripleID triple = iterator.next();
		Triple t = new Triple(
				nodeDictionary.getNode(triple.getSubject(), TripleComponentRole.SUBJECT),
				nodeDictionary.getNode(triple.getPredicate(), TripleComponentRole.PREDICATE),
				nodeDictionary.getNode(triple.getObject(), TripleComponentRole.OBJECT)
			);
		
		return t;
	}

}