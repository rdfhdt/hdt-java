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
import org.rdfhdt.hdt.util.RDFInfo;

public class ModHDTLoaderTwoPass implements ModHDTLoader {

	class DictionaryAppender implements RDFCallback {

		ModifiableDictionary dict;
		ProgressListener listener;
		long count=0;

		DictionaryAppender(ModifiableDictionary dict, ProgressListener listener) {
			this.dict = dict;
			this.listener = listener;
		}
		
		@Override
		public void processTriple(TripleString triple, long pos) {
			dict.insert(triple.getSubject(), TripleComponentRole.SUBJECT);
			dict.insert(triple.getPredicate(), TripleComponentRole.PREDICATE);
			dict.insert(triple.getObject(), TripleComponentRole.OBJECT);
			count++;
			ListenerUtil.notifyCond(listener, "Generating dictionary "+count+" triples processed.", count, 0, 100);
		}
		
		public long getCount() {
			return count;
		}
	};
	
	/**
	 * Warning: different from HDTConverterOnePass$TripleAppender
	 * This one uses dict.stringToID, the other uses dict.insert
	 * @author mario.arias
	 *
	 */
	class TripleAppender2 implements RDFCallback {
		ModifiableDictionary dict;
		ModifiableTriples triples;
		ProgressListener listener;
		long count=0;

		public TripleAppender2(ModifiableDictionary dict, ModifiableTriples triples, ProgressListener listener) {
			this.dict = dict;
			this.triples = triples;
			this.listener = listener;
		}

		public void processTriple(TripleString triple, long pos) {
			triples.insert(
					dict.stringToId(triple.getSubject(), TripleComponentRole.SUBJECT),
					dict.stringToId(triple.getPredicate(), TripleComponentRole.PREDICATE),
					dict.stringToId(triple.getObject(), TripleComponentRole.OBJECT)
			);
			count++;
			ListenerUtil.notifyCond(listener, "Generating triples "+count+" triples processed.", count, 0, 100);
		}
	};
	
	@Override
	public ModifiableHDT loadFromRDF(HDTSpecification specs, String filename, String baseUri, RDFNotation notation, ProgressListener listener)
			throws IOException, ParserException {
		
		// Create Modifiable Instance and parser
		ModifiableHDT modHDT = HDTFactory.createModifiableHDT(specs, baseUri,
				ModeOfLoading.TWO_PASS);
		ModifiableDictionary dictionary = (ModifiableDictionary)modHDT.getDictionary();
		ModifiableTriples triples = (ModifiableTriples)modHDT.getTriples();
		
        RDFParserCallback parser = RDFParserFactory.getParserCallback(notation);
		DictionaryAppender appender = new DictionaryAppender(dictionary, listener);
        
		// Load RDF in the dictionary
		dictionary.startProcessing();
		parser.doParse(filename, baseUri, notation, appender);
		dictionary.endProcessing();
		
		// Fill the specs with missing properties (disregard possible preset values because these are 100% correct
													//with no calculation overhead - unlike in one-pass mode)
		RDFInfo.fillHDTSpecifications(new File(filename).length(), appender.count, specs);
		
		// Reorganize IDs before loading triples
		modHDT.reorganizeDictionary(listener);
		
		// Load triples (second pass)
		parser.doParse(filename, baseUri, notation, new TripleAppender2(dictionary, triples, listener));
		
		//reorganize HDT
		modHDT.reorganizeTriples(listener);
		
		return modHDT;
	}


}
