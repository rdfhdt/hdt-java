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

package hdt.examples;

import java.io.IOException;

import hdt.exceptions.NotFoundException;
import hdt.hdt.HDT;
import hdt.hdt.HDTFactory;
import hdt.iterator.IteratorTripleString;
import hdt.triples.TripleString;

/**
 * @author mario.arias
 *
 */
public class ExampleLoad {
public static void main(String[] args) throws IOException, NotFoundException {
	// Load HDT file
	HDT hdt = HDTFactory.createHDT();
	hdt.loadFromHDT("data/example.hdt", null);
	
	// Recommended: Generate index to speed up ?P? ?PO and ??O queries.
	hdt.loadOrCreateIndex(null);
	
	// Search pattern: Empty string means "any"
	IteratorTripleString it = hdt.search("", "", "");
	while(it.hasNext()) {
		TripleString ts = it.next();
		System.out.println(ts);
	}
}
}
