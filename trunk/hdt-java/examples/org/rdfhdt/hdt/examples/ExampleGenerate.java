/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/examples/org/rdfhdt/hdt/examples/ExampleInsert.java $
 * Revision: $Rev: 17 $
 * Last modified: $Date: 2012-07-03 21:43:15 +0100 (mar, 03 jul 2012) $
 * Last modified by: $Author: mario.arias@gmail.com $
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

import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.options.HDTSpecification;

/**
 * @author mario.arias
 *
 */
public class ExampleGenerate {

	public static void main(String[] args) throws Exception {
		// Configuration variables
		String baseURI = "http://example.com/mydataset";
		String rdfInput = "/path/to/dataset.nt";
		String inputType = "ntriples";
		String hdtOutput = "/path/to/dataset.hdt";
		
		// Create HDT from RDF file
		HDT hdt = HDTManager.generateHDT(rdfInput, baseURI, RDFNotation.parse(inputType), new HDTSpecification(), null);
		
		// Save generated HDT to a file
		hdt.saveToHDT(hdtOutput, null);
	}
}
