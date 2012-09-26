/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/hdt/impl/HDTFactory.java $
 * Revision: $Rev: 57 $
 * Last modified: $Date: 2012-08-24 01:26:52 +0100 (Fri, 24 Aug 2012) $
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

package org.rdfhdt.hdt.hdt.impl;

import java.io.File;
import java.io.IOException;

import org.rdfhdt.hdt.dictionary.ModifiableDictionary;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDTFactory;
import org.rdfhdt.hdt.hdt.ModHDTLoader;
import org.rdfhdt.hdt.hdt.ModifiableHDT;
import org.rdfhdt.hdt.hdt.ModifiableHDT.ModeOfLoading;
import org.rdfhdt.hdt.listener.ListenerUtil;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.rdf.RDFParserCallback;
import org.rdfhdt.hdt.rdf.RDFParserCallback.RDFCallback;
import org.rdfhdt.hdt.rdf.RDFParserFactory;
import org.rdfhdt.hdt.triples.ModifiableTriples;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.triples.TriplesFactory;
import org.rdfhdt.hdt.util.RDFInfo;

public class ModHDTLoaderOnePass implements ModHDTLoader {

	class TripleAppender implements RDFCallback {
		ModifiableDictionary dict;
		ModifiableTriples triples;
		ProgressListener listener;
		long num = 0;

		public TripleAppender(ModifiableDictionary dict, ModifiableTriples triples, ProgressListener listener) {
			this.dict = dict;
			this.triples = triples;
			this.listener = listener;
		}

		public void processTriple(TripleString triple, long pos) {
			triples.insert(
					dict.insert(triple.getSubject(), TripleComponentRole.SUBJECT),
					dict.insert(triple.getPredicate(), TripleComponentRole.PREDICATE),
					dict.insert(triple.getObject(), TripleComponentRole.OBJECT)
			);
			num++;
			ListenerUtil.notifyCond(listener, "Loaded "+num+" triples", num, 0, 100);
		}
	};
	
	@Override
	public ModifiableHDT loadFromRDF(HDTSpecification specs, String filename, String baseUri, RDFNotation notation, ProgressListener listener)
			throws IOException, ParserException {
		
		// Fill the specs with missing properties
		if (RDFInfo.getLines(specs)==null) {//if lines set by user believe them
			//FIXME constant used here... not good practice.
			//Done this way because TriplesSet does not need to know the number of
			//triples in advance to be memory efficient (and on a billion triples in
			//nt format it takes around 45 minutes to just count the lines xD)
			if (TriplesFactory.MOD_TRIPLES_IMPL_LIST.equals(
					specs.get("tempTriples.impl"))){
				//count lines and set them
				RDFInfo.setLines(RDFInfo.countLines(new File(filename)), specs);
			}
		}
		RDFInfo.setSizeInBytes(new File(filename).length(), specs);
		
		// Create Modifiable Instance
		ModifiableHDT modHDT = HDTFactory.createModifiableHDT(specs, baseUri, 
				ModeOfLoading.ONE_PASS);
		ModifiableDictionary dictionary = (ModifiableDictionary)modHDT.getDictionary();
		ModifiableTriples triples = (ModifiableTriples)modHDT.getTriples();
		
        RDFParserCallback parser = RDFParserFactory.getParserCallback(notation);

        // Load RDF in the dictionary and generate triples
        dictionary.startProcessing();
        parser.doParse(filename, baseUri, notation, new TripleAppender(dictionary, triples, listener));
        dictionary.endProcessing();
		
		// Reorganize both the dictionary and the triples
		modHDT.reorganizeDictionary(listener);
		modHDT.reorganizeTriples(listener);
		
		return modHDT; 
	}

}
