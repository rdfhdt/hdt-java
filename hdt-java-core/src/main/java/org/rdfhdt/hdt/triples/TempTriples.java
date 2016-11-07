/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/triples/TempTriples.java $
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

import java.io.Closeable;

import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.listener.ProgressListener;

/**
 * Interface for TempTriples implementation.
 * 
 * This is a dynamic interface. For static(read-only) behaviour have a look at
 * {@link Triples}
 * 
 */
public interface TempTriples extends TriplesPrivate, Closeable {
	/**
	 * Add one triple
	 * @param subject
	 * @param predicate
	 * @param object
	 * @return
	 */
	boolean insert(int subject, int predicate, int object);
	
	/**
	 * Adds one or more triples
	 * 
	 * @param triples
	 *            The triples to be inserted
	 * @return boolean
	 */
	boolean insert(TripleID... triples);
	
	/**
	 * Updates the given TripleID with the new values.
	 * 
	 * The control has to be given over this method to the TempTriples
	 * implementation instead of changing the triple manually because some
	 * implementations might not react well to that (TriplesSet for example, changing
	 * the triple will not trigger reordering)
	 * 
	 * @param triple
	 * @param subj
	 * @param pred
	 * @param obj
	 * @return
	 */
	boolean update(TripleID triple, int subj, int pred, int obj);

	/**
	 * Deletes one or more triples according to a pattern
	 * 
	 * @param pattern
	 *            The pattern to match against
	 * @return boolean
	 */
	boolean remove(TripleID... pattern);

	/**
	 * Sorts the triples based on the order(TripleComponentOrder) of the
	 * triples.
	 * If you want to sort in a different order use setOrder first.
	 */
	void sort(ProgressListener listener);

	void removeDuplicates(ProgressListener listener);
	
	/**
	 * Sets a type of order(TripleComponentOrder)
	 * 
	 * @param order
	 *            The order to set
	 */
	void setOrder(TripleComponentOrder order);
	
	/**
	 * Gets the currently set order(TripleComponentOrder)
	 */
	TripleComponentOrder getOrder();
	
	/**
	 * Clear all triples, resulting in an empty triples section.
	 */
	void clear();
	
	/**
	 * Load triples from another instance.
	 */
	void load(Triples triples, ProgressListener listener);
}
