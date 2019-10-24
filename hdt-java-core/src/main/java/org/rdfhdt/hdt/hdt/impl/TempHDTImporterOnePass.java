/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/hdt/impl/TempHDTImporterOnePass.java $
 * Revision: $Rev: 191 $
 * Last modified: $Date: 2013-03-03 11:41:43 +0000 (dom, 03 mar 2013) $
 * Last modified by: $Author: mario.arias $
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 3.0 of the License.
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

import java.io.IOException;
import java.util.Iterator;

import org.rdfhdt.hdt.dictionary.TempDictionary;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.hdt.TempHDT;
import org.rdfhdt.hdt.hdt.TempHDTImporter;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.rdf.RDFParserCallback;
import org.rdfhdt.hdt.rdf.RDFParserCallback.RDFCallback;
import org.rdfhdt.hdt.rdf.RDFParserFactory;
import org.rdfhdt.hdt.triples.TempTriples;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.util.listener.ListenerUtil;

public class TempHDTImporterOnePass implements TempHDTImporter {

	class TripleAppender implements RDFCallback {
		final TempDictionary dict;
		final TempTriples triples;
		final ProgressListener listener;
		long num;
		long size;

		public TripleAppender(TempDictionary dict, TempTriples triples, ProgressListener listener) {
			this.dict = dict;
			this.triples = triples;
			this.listener = listener;
		}

		@Override
        public void processTriple(TripleString triple, long pos) {
			triples.insert(
					dict.insert(triple.getSubject(), TripleComponentRole.SUBJECT),
					dict.insert(triple.getPredicate(), TripleComponentRole.PREDICATE),
					dict.insert(triple.getObject(), TripleComponentRole.OBJECT)
			);
			num++;
			size+=triple.getSubject().length()+triple.getPredicate().length()+triple.getObject().length()+4;  // Spaces and final dot
			ListenerUtil.notifyCond(listener, "Loaded "+num+" triples", num, 0, 100);
		}
	}

	@Override
	public TempHDT loadFromRDF(HDTOptions specs, String filename, String baseUri, RDFNotation notation, ProgressListener listener)
			throws IOException, ParserException {
		
		RDFParserCallback parser = RDFParserFactory.getParserCallback(notation);
		
		// Create Modifiable Instance
		TempHDT modHDT = new TempHDTImpl(specs, baseUri, ModeOfLoading.ONE_PASS);
		TempDictionary dictionary = modHDT.getDictionary();
		TempTriples triples = modHDT.getTriples();
		TripleAppender appender = new TripleAppender(dictionary, triples, listener);

        // Load RDF in the dictionary and generate triples
        dictionary.startProcessing();
        parser.doParse(filename, baseUri, notation, appender);
        dictionary.endProcessing();
		
		// Reorganize both the dictionary and the triples
		modHDT.reorganizeDictionary(listener);
		modHDT.reorganizeTriples(listener);
		
		modHDT.getHeader().insert( "_:statistics", HDTVocabulary.ORIGINAL_SIZE, appender.size);
		
		return modHDT; 
	}
	
	public TempHDT loadFromTriples(HDTOptions specs, Iterator<TripleString> iterator, String baseUri, ProgressListener listener)
			throws IOException {
			
		// Create Modifiable Instance
		TempHDT modHDT = new TempHDTImpl(specs, baseUri, ModeOfLoading.ONE_PASS);
		TempDictionary dictionary = modHDT.getDictionary();
		TempTriples triples = modHDT.getTriples();

        // Load RDF in the dictionary and generate triples
        dictionary.startProcessing();
        long num=0;
        long size=0;
        while(iterator.hasNext()) {
        	TripleString triple = iterator.next();
        	triples.insert(
        			dictionary.insert(triple.getSubject(), TripleComponentRole.SUBJECT),
        			dictionary.insert(triple.getPredicate(), TripleComponentRole.PREDICATE),
        			dictionary.insert(triple.getObject(), TripleComponentRole.OBJECT)
        			);
        	num++;
			size+=triple.getSubject().length()+triple.getPredicate().length()+triple.getObject().length()+4;  // Spaces and final dot
        	ListenerUtil.notifyCond(listener, "Loaded "+num+" triples", num, 0, 100);
        }
        dictionary.endProcessing();
		
		// Reorganize both the dictionary and the triples
		modHDT.reorganizeDictionary(listener);
		modHDT.reorganizeTriples(listener);
		
		modHDT.getHeader().insert( "_:statistics", HDTVocabulary.ORIGINAL_SIZE, size);
		
		return modHDT; 
	}
}
