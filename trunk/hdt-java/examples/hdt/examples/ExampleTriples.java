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
package hdt.examples;

import hdt.iterator.IteratorTripleID;
import hdt.triples.ModifiableTriples;
import hdt.triples.TripleID;
import hdt.triples.TriplesFactory;
import hdt.triples.impl.BitmapTriples;

/**
 * @author mck
 *
 */
public class ExampleTriples {
public static void main(String[] args) {
	
	// Create triples
	ModifiableTriples modTriples = TriplesFactory.createModifiableTriples();
	
	// Load data
	// WARNING: All the subjects from 1 to n must appear at least once.
	// The triples do not need to be sorted.
	modTriples.insert(1,1,1);
	modTriples.insert(1,2,1);
	modTriples.insert(2,3,2);
	
	// Compact
	BitmapTriples triples = new BitmapTriples();
	triples.load(modTriples, null);
	
	// Old no longer necessary, free memory.
	modTriples = null;
	
	// Search. Note: 0 means "any"
	IteratorTripleID it = triples.search(new TripleID(1,0,0));
	while(it.hasNext()) {
		TripleID triple = it.next();
		System.out.println(triple);
	}
}
}
