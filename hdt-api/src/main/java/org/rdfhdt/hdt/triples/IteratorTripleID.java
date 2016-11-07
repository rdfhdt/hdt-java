/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/iface/org/rdfhdt/hdt/triples/IteratorTripleID.java $
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

package org.rdfhdt.hdt.triples;

import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.triples.TripleID;

import java.util.Iterator;


/**
 * Iterator of TripleID
 * 
 */
public interface IteratorTripleID extends Iterator<TripleID> {

	/**
	 * Whether the iterator has previous elements.
	 * @return
	 */
	boolean hasPrevious();
	
	/**
	 * Get the previous element. Call only if hasPrevious() returns true.
	 * It moves the cursor of the Iterator to the previous entry.
	 * @return
	 */
	TripleID previous();
	
	/** 
	 * Point the cursor to the first element of the data structure.
	 */
	void goToStart();
	
	/**
	 * Specifies whether the iterator can move to a random position.
	 * @return
	 */
	boolean canGoTo();
	
	/**
	 * Go to the specified random position. Only use whenever canGoTo() returns true.
	 * @param pos
	 */
	void goTo(long pos);
	
	/** 
	 * Returns the number of estimated results of the Iterator.
	 * It is usually more efficient than going through all the results.
	 * 
	 * @return Number of estimated results.
	 */
	long estimatedNumResults();
	
	/**
	 * Returns the accuracy of the estimation of number of results as returned
	 * by estimatedNumResults()
	 * 
	 * @return
	 */
	ResultEstimationType numResultEstimation();
	
	/**
	 * Return the order in which the triples are iterated (Might be unknown)
	 * @return
	 */
	TripleComponentOrder getOrder();
}
