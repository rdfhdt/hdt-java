/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/hdt/HDT.java $
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

package org.rdfhdt.hdt.hdt;

import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.iterator.IteratorTripleString;

/**
 * Interface for a queryable HDT object.
 * 
 * @author Eugen
 *
 */
public interface QueryableHDT extends HDT {
	
	/**
	 * Search a pattern in a HDT
	 * 
	 * The null string acts as a wildcard. (i.e. search(null, null, null)
	 * returns all elements)
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
	public IteratorTripleString search(CharSequence subject, CharSequence predicate, CharSequence object) throws NotFoundException;

}
