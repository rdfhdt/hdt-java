/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/examples/org/rdfhdt/hdt/examples/ExampleGenerate.java $
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

package org.rdfhdt.hdt.example;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.header.Header;
import org.rdfhdt.hdt.options.HDTSpecification;

/**
 * @author mario.arias
 *
 */
public class ExampleGenerate {

	public static void main(String[] args) throws Exception {
		// Configuration variables
		 String baseURI = "http://example.com/3.5";
		//String rdfInput = "/nfsmnt/proto_space/dbpedia/3.5/dbpedia";
		String rdfInput = "/nfsmnt/proto_space/dbpedia/3.5/dbpedia";
		String inputType = "ntriples";
		String hdtOutput = "/home/ludab/hdt-java/hdt-java-core/3_5_0.hdt";
	 	
		String configFile = "/home/ludab/hdt-java/hdt-java-core/hdt.cfg";
		// Create HDT from RDF file
		
		HDT hdt = HDTManager.generateHDT(rdfInput, baseURI, RDFNotation.parse(inputType), new HDTSpecification(configFile), null);
		
		// Add additional domain-specific properties to the header:
		//Header header = hdt.getHeader();
		//header.insert("myResource1", "property" , "value");
		
		//System.out.println("now try to save to file"); 
		// Save generated HDT to a file
		hdt.saveToHDT(hdtOutput, null); 
		
		//HDT mhdt = HDTManager.loadHDT("/home/ludab/hdt-java/hdt-java-core/3_8_0.hdt", null);
		
		//HDT hdt = HDTManager.mapHDT(args[0], null);

		
		hdt = HDTManager.indexedHDT(hdt,null);
		if (hdt!=null) {
			hdt.close();
		}
			
	}
}
