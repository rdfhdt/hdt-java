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

import java.io.File;
import java.io.IOException;

import org.rdfhdt.hdt.dictionary.DictionaryFactory;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.ModifiableHDT.ModeOfLoading;
import org.rdfhdt.hdt.hdt.impl.BaseHDT;
import org.rdfhdt.hdt.hdt.impl.BaseModifiableHDT;
import org.rdfhdt.hdt.hdt.impl.HDTRW;
import org.rdfhdt.hdt.hdt.impl.ModHDTImporterTwoPass;
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
	public static QueryableHDT createQueryableHDT() {
		return createQueryableHDT(new HDTSpecification());
	}

	/**
	 * Creates an HDT with the specified spec
	 * 
	 * @return HDT
	 */
	public static QueryableHDT createQueryableHDT(HDTSpecification specification) {
		//TODO ... some options here, a choice?
		return new BaseHDT(specification);
	}
	
	/**
	 * Creates a ModifiableHDT with the specified spec, baseUri and ModeOfLoading.
	 * 
	 * ModeOfLoading can be null if the ModifiableHDT is not meant to be populated from RDF
	 * (i.e. ModHDTImporter object not used).
	 * 
	 * @return ModifiableHDT
	 */
	public static ModifiableHDT createModifiableHDT(HDTSpecification spec, String baseUri, ModeOfLoading modeOfLoading) {
		
		//FIXME ... some options here, a choice? (BaseModifiableHDT?
		String dictName = spec.get("dictionary.name");
		//TODO switch-case can use String in 1.7 and after...
		if (DictionaryFactory.DICT_MOD_HASH.equalsIgnoreCase(dictName)){
			return new HDTRW(spec, baseUri, modeOfLoading);
		} else if (DictionaryFactory.DICT_MOD_JDBM.equalsIgnoreCase(dictName)){
			return new BaseModifiableHDT(spec, baseUri, modeOfLoading);
		} else if (DictionaryFactory.DICT_MOD_BERKELEY.equalsIgnoreCase(dictName)){
			return new BaseModifiableHDT(spec, baseUri, modeOfLoading);
		} else if (DictionaryFactory.DICT_MOD_BERKELEY_NATIVE.equalsIgnoreCase(dictName)){
			return new BaseModifiableHDT(spec, baseUri, modeOfLoading);
		} else if (DictionaryFactory.DICT_MOD_KYOTO.equals(dictName)){
			return new BaseModifiableHDT(spec, baseUri, modeOfLoading);
		} else {
			return new HDTRW(spec, baseUri, modeOfLoading);
		}
		
	}
	
//----------------------------------------------------------------------------------------------------
	
	/**
	 * Creates a ModifiableHDT from an RDF file and returns it.
	 * The ModifiableHDT object is created using the ModHDTImporter object (converter), and
	 * the class type of that object determines which way the RDF is read and the
	 * ModifiableHDT constructed (one-pass or two-pass).
	 * 
	 */
	public static ModifiableHDT createModHDTFromRDF(HDTSpecification spec, String filename, String baseUri, RDFNotation notation, ProgressListener listener) throws IOException, ParserException {
		return converter.loadFromRDF(spec, filename, baseUri, notation, listener);
	}

	/**
	 * This method creates a QueryableHDT object from a ModifiableHDT object by loading
	 * information from it (using method loadFromModifiableHDT) and doing some other
	 * operations on the newly created object.
	 */
	public static QueryableHDT createHDTFromModHDT(HDTSpecification spec, ModifiableHDT modHdt, ProgressListener listener) throws IOException {
		
		BaseHDT hdt = new BaseHDT(spec); //FIXME should use "HDTFactory.createHDT(spec)" but can't...
		
		hdt.loadFromModifiableHDT(modHdt, listener);
		hdt.populateHeaderStructure(modHdt.getBaseURI()); //FIXME ... because of this (unless casted, but that doesn't make any more sense)

		return hdt;
	}
	
	/**
	 * This method is a simple way of converting an RDF file into an HDT file.
	 * 
	 * It basically does this in 2 steps by combining the createModHDTFromRDF and createHDTFromModHDT
	 * methods of this class (HDTFactory).
	 * 
	 */
	public static QueryableHDT createHDTFromRDF(HDTSpecification spec, String filename, String baseUri, RDFNotation notation, ProgressListener listener) throws IOException, ParserException {
		StopWatch st = new StopWatch();
		
		// Create ModifiableHDT
		ModifiableHDT modHdt = HDTFactory.createModHDTFromRDF(spec, filename, baseUri, notation, listener);
		
		// Convert to HDT
		QueryableHDT hdt = HDTFactory.createHDTFromModHDT(spec, modHdt, listener);
		
		// Add file size to Header
		hdt.getHeader().insert("_:statistics", HDTVocabulary.ORIGINAL_SIZE, new File(filename).length());
		
		System.out.println("File converted in: "+st.stopAndShow());
		
		modHdt.close();
		
		return hdt;
	}
	
}
