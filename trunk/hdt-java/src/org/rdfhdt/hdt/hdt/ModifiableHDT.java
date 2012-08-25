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

package org.rdfhdt.hdt.hdt;

import java.io.Closeable;

import org.rdfhdt.hdt.iterator.IteratorTripleString;
import org.rdfhdt.hdt.listener.ProgressListener;

/**
 * This interface specifies methods that enable HDT objects
 * to be modifiable in the sence that triples can be inserted
 * and removed from them and they can be reorganized after changes
 * have been made (so they can be used to load data from them into
 * a non-modifiable HDT).
 * 
 * @author mario.arias, Eugen
 *
 */
public interface ModifiableHDT extends HDT, Closeable {
	
	/**
	 * Enumeration for specifing the mode of loading of a
	 * modifiable HDT from RDF
	 * @author Eugen
	 *
	 */
	public enum ModeOfLoading {
		ONE_PASS,
		TWO_PASS
	}
	
	void insert(CharSequence subject, CharSequence predicate, CharSequence object);
	
	void insert(IteratorTripleString triples);
	
	void remove(CharSequence subject, CharSequence predicate, CharSequence object);
	
	void remove(IteratorTripleString triples);
	
	void loadFromHDT(HDT hdt, ProgressListener listener);
	
	/**
	 * This method should be used before reorganizing triples!
	 * 
	 * It reorganizes the dictionary and updates it's ID's
	 * (usually done just by calling reorganize method of
	 * dictionary)
	 * 
	 * @param listener
	 */
	void reorganizeDictionary(ProgressListener listener);
	
	/**
	 * 
	 * It sorts the triples and removes duplicates.
	 * 
	 * @param listener
	 */
	void reorganizeTriples(ProgressListener listener);
	
	boolean isOrganized();
	
	void clear();
}
