/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/examples/org/rdfhdt/hdt/examples/ExampleInsert.java $
 * Revision: $Rev: 27 $
 * Last modified: $Date: 2012-07-19 01:22:22 +0100 (jue, 19 jul 2012) $
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

package org.rdfhdt.hdt.examples;

import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.ModifiableHDT;
import org.rdfhdt.hdt.hdt.QueryableHDT;
import org.rdfhdt.hdt.hdt.impl.HDTFactory;
import org.rdfhdt.hdt.iterator.IteratorTripleString;
import org.rdfhdt.hdt.options.HDTSpecification;

/**
 * @author mario.arias
 *
 */
public class ExampleModHDT {

	public static void main(String[] args) throws Exception {
		// Create instance
		ModifiableHDT mhdt = HDTFactory.createModifiableHDT(new HDTSpecification(),"http://example.com/base/");
		
		// Load data
		mhdt.insert("uri1", "p1", "val1");
		mhdt.insert("uri1", "p1", "val2");
		mhdt.insert("uri1", "p2", "val3");
		mhdt.insert("uri2", "p2", "val4");
		
		// Convert to HDT
		HDT hdt = HDTFactory.createHDTFromModHDT(new HDTSpecification(), mhdt, null);
		// Close ModifiableHDT
		mhdt.close();
		
		// Load back
		ModifiableHDT mhdt2 = HDTFactory.createModifiableHDT(new HDTSpecification(),"http://example.com/base/");
		mhdt2.loadFromHDT(hdt, null);
		
		IteratorTripleString it = ((QueryableHDT)mhdt2).search("", "", "");
		while(it.hasNext()) {
			System.out.println(it.next());
		}
	}
}
