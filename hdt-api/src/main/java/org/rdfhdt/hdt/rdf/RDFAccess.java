/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/iface/org/rdfhdt/hdt/rdf/RDFAccess.java $
 * Revision: $Rev: 191 $
 * Last modified: $Date: 2013-03-03 11:41:43 +0000 (dom, 03 mar 2013) $
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

package org.rdfhdt.hdt.rdf;

import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.triples.IteratorTripleString;

/**
 * @author mario.arias
 *
 */
public interface RDFAccess {
	
	/**
	 * Iterate over the triples of an RDF Set that match the specified pattern.
	 * null and empty strings act as a wildcard. 
	 * (e.g. search(null, null, null) iterates over all elements)
	 * 
	 * @param subject
	 *            The subject to search
	 * @param predicate
	 *            The predicate to search
	 * @param object
	 *            The object to search
	 * 
	 * @return Iterator of TripleStrings
	 * @throws NotFoundException 
	 */
	IteratorTripleString search(CharSequence subject, CharSequence predicate, CharSequence object) throws NotFoundException;
}
