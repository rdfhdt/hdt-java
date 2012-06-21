/**
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
/**
 * 
 */
package hdt.triples;

import hdt.enums.TripleComponentOrder;
import hdt.listener.ProgressListener;

/**
 * Interface for ModifiedTriples implementation.
 * 
 * This is a dynamic interface. For static(read-only) behaviour have a look at
 * {@link Triples}
 * 
 */
public interface ModifiableTriples extends Triples {
	/**
	 * Add one triple
	 * @param subject
	 * @param predicate
	 * @param object
	 * @return
	 */
	public abstract boolean insert(int subject, int predicate, int object);
	
	/**
	 * Adds one or more triples
	 * 
	 * @param triples
	 *            The triples to be inserted
	 * @return boolean
	 */
	public abstract boolean insert(TripleID... triples);

	/**
	 * Deletes one or more triples according to a pattern
	 * 
	 * @param pattern
	 *            The pattern to match against
	 * @return boolean
	 */
	public abstract boolean remove(TripleID... pattern);
	
	public abstract void replace(int id, int subject, int predicate, int object);

	/**
	 * Sorts the triples based on an order(TripleComponentOrder)
	 * 
	 * @param order
	 *            The order to sort the triples with
	 */
	public abstract void sort(TripleComponentOrder order, ProgressListener listener);

	public abstract void removeDuplicates(ProgressListener listener);
	
	/**
	 * Sets a type of order(TripleComponentOrder)
	 * 
	 * @param order
	 *            The order to set
	 */
	public abstract void setOrder(TripleComponentOrder order);
}
