package org.rdfhdt.hdt.hdt.impl;

import java.io.IOException;

import org.rdfhdt.hdt.dictionary.ModifiableDictionary;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.ModHDTImporter;
import org.rdfhdt.hdt.hdt.ModifiableHDT;
import org.rdfhdt.hdt.listener.ListenerUtil;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.rdf.RDFParserCallback;
import org.rdfhdt.hdt.rdf.RDFParserCallback.RDFCallback;
import org.rdfhdt.hdt.rdf.RDFParserFactory;
import org.rdfhdt.hdt.triples.ModifiableTriples;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.util.StopWatch;

public class ModHDTImporterTwoPass implements ModHDTImporter {

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
	public ModifiableHDT loadFromRDF(HDTSpecification spec, String filename, String baseUri, RDFNotation notation, ProgressListener listener)
			throws IOException, ParserException {
		
		// Create Modifiable Instance and parser
		/* HDTRW modHDT = new HDTRW(spec); */
		ModifiableHDT modHDT = HDTFactory.createModifiableHDT(spec, baseUri);
		RDFParserCallback parser = RDFParserFactory.getParserCallback(notation);
		
		// Load dictionary
		ModifiableDictionary dictionary = (ModifiableDictionary) modHDT.getDictionary();
		dictionary.startProcessing();
		parser.doParse(filename, baseUri, notation, new DictionaryAppender(dictionary, listener));
		dictionary.endProcessing();
		
		// Reorganize IDs
		dictionary.reorganize();
		
		// Load triples
		ModifiableTriples triples = (ModifiableTriples) modHDT.getTriples();
		parser.doParse(filename, baseUri, notation, new TripleAppender2(dictionary, triples, listener));
		
		// Sort and Duplicates
		String orderStr = spec.get("triples.component.order");
		if(orderStr==null)
			orderStr = "SPO";
		
		StopWatch sortDupTime = new StopWatch();
		triples.sort(TripleComponentOrder.valueOf(orderStr), listener);
		triples.removeDuplicates(listener);
		System.out.println("Sort triples and remove duplicates: "+sortDupTime.stopAndShow());
		
		// Mark as reorganized
		((HDTRW)modHDT).isOrganized = true; //FIXME this is a hack... should be done better somehow
		//modHDT.baseUri = baseUri;
		
		return modHDT;
	}


}
