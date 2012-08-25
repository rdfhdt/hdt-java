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

package org.rdfhdt.hdt.examples;

import java.io.IOException;

import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTFactory;
import org.rdfhdt.hdt.hdt.ModifiableHDT;
import org.rdfhdt.hdt.options.HDTSpecification;

/**
 * @author mario.arias
 *
 */
public class ExampleInsert {

	public static void main(String[] args) throws IOException {
		
		// Create instance
		ModifiableHDT mhdt = HDTFactory.createModifiableHDT(new HDTSpecification(),"http://example.com/base/", null);
		
		// Load data
		mhdt.insert("uri1", "p1", "val1");
		mhdt.insert("uri1", "p1", "val2");
		mhdt.insert("uri1", "p2", "val3");
		mhdt.insert("uri1", "p2", "val4");
			
		// Compact and save
		HDT hdt = HDTFactory.createHDTFromModHDT(new HDTSpecification(), mhdt, null);
		hdt.saveToHDT("data/example.hdt", null);
	}
}
