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

package org.rdfhdt.hdt.hdt.impl;

import java.io.File;
import java.io.IOException;

import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.hdt.ModHDTImporter;
import org.rdfhdt.hdt.hdt.ModifiableHDT;
import org.rdfhdt.hdt.hdt.QueryableHDT;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.util.StopWatch;

/**
 * Factory that creates HDT objects
 * 
 */
public class HDTFactory {
	
	// TODO: Choose from config file / depending on input size?
	//private static ModHDTImporter converter = new ModHDTImporterOnePass();
	private static ModHDTImporter converter = new ModHDTImporterTwoPass();

	/**
	 * Creates a default HDT
	 * 
	 * @return HDT
	 */
	public static QueryableHDT createHDT() {
		return createHDT(new HDTSpecification());
	}

	/**
	 * Creates an HDT with the specified spec
	 * 
	 * @return HDT
	 */
	public static QueryableHDT createHDT(HDTSpecification specification) {
		return new BaseHDT(specification);
	}

	public static HDT createHDTFromModHDT(HDTSpecification spec, ModifiableHDT modHdt, ProgressListener listener) throws IOException {
		BaseHDT hdt = new BaseHDT(spec);
		
		modHdt.reorganize(listener);
		
		hdt.loadFromModifiableHDT(modHdt, listener);
		hdt.populateHeaderStructure(modHdt.getBaseURI());

		return hdt;
	}
	
	public static HDT createHDTFromRDF(HDTSpecification spec, String filename, String baseUri, RDFNotation notation, ProgressListener listener) throws IOException, ParserException {
		StopWatch st = new StopWatch();
		
		// Create ModifiableHDT
		ModifiableHDT modHdt = HDTFactory.createModHDTFromRDF(spec, filename, baseUri, notation, listener);
		
		// Convert to HDT
		HDT hdt = HDTFactory.createHDTFromModHDT(spec, modHdt, listener);
		
		// Add file size to Header
		hdt.getHeader().insert("_:statistics", HDTVocabulary.ORIGINAL_SIZE, new File(filename).length());
		
		System.out.println("File converted in: "+st.stopAndShow());
		
		modHdt.close();
		
		return hdt;
	}
	
	/**
	 * Creates a ModifiableHDT
	 * @param spec
	 * @return
	 */
	public static ModifiableHDT createModifiableHDT(HDTSpecification spec, String baseUri) {
		HDTRW modhdt = new HDTRW(spec);
		modhdt.baseUri = baseUri;
		return modhdt;
	}
	
	public static ModifiableHDT createModHDTFromRDF(HDTSpecification spec, String filename, String baseUri, RDFNotation notation, ProgressListener listener) throws IOException, ParserException {
		return converter.loadFromRDF(spec, filename, baseUri, notation, listener);
	}

	
}
