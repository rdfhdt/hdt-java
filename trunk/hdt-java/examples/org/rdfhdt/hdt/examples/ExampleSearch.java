/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/examples/org/rdfhdt/hdt/examples/ExampleInsert.java $
 * Revision: $Rev: 58 $
 * Last modified: $Date: 2012-08-26 00:31:15 +0100 (dom, 26 ago 2012) $
 * Last modified by: $Author: simpsonim13@gmail.com $
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

package org.rdfhdt.hdt.examples;

import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;

/**
 * @author mario.arias
 *
 */
public class ExampleSearch {

	public static void main(String[] args) throws Exception {
		// Load HDT file NOTE: Use loadIndexedHDT() if you are doing ?P?, ?PO, ??O queries
		HDT hdt = HDTManager.loadHDT("data/example.hdt", null);
		
		// Search pattern: Empty string means "any"
		IteratorTripleString it = hdt.search("", "", "");
		System.out.println("Estimated number of results: "+it.estimatedNumResults());
		while(it.hasNext()) {
			TripleString ts = it.next();
			System.out.println(ts);
		}
	}
}
