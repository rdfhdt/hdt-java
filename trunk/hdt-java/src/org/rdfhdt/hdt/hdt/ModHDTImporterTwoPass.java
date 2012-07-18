package org.rdfhdt.hdt.hdt;

import java.io.IOException;

import org.rdfhdt.hdt.dictionary.ModifiableDictionary;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.rdf.RDFParserCallback;
import org.rdfhdt.hdt.rdf.RDFParserCallback.RDFCallback;
import org.rdfhdt.hdt.rdf.RDFParserFactory;
import org.rdfhdt.hdt.triples.ModifiableTriples;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.util.StopWatch;

public class ModHDTImporterTwoPass implements ModHDTImporter {

	public class DictionaryAppender implements RDFCallback {

		ModifiableDictionary dict;

		public DictionaryAppender(ModifiableDictionary dict) {
			this.dict = dict;
		}
		
		@Override
		public void processTriple(TripleString triple, long pos) {
			dict.insert(triple.getSubject(), TripleComponentRole.SUBJECT);
			dict.insert(triple.getPredicate(), TripleComponentRole.PREDICATE);
			dict.insert(triple.getObject(), TripleComponentRole.OBJECT);
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

		public TripleAppender2(ModifiableDictionary dict, ModifiableTriples triples) {
			this.dict = dict;
			this.triples = triples;
		}

		public void processTriple(TripleString triple, long pos) {
			triples.insert(
					dict.stringToId(triple.getSubject(), TripleComponentRole.SUBJECT),
					dict.stringToId(triple.getPredicate(), TripleComponentRole.PREDICATE),
					dict.stringToId(triple.getObject(), TripleComponentRole.OBJECT)
			);
		}
	};
	
	@Override
	public ModifiableHDT loadFromRDF(HDTSpecification spec, String filename, String baseUri, RDFNotation notation, ProgressListener listener)
			throws IOException, ParserException {
		
		// Create Modifiable Instance and parser
		HDTRW modHDT = new HDTRW(new HDTSpecification());
		RDFParserCallback parser = RDFParserFactory.getParserCallback(notation);
		
		// Load dictionary
		ModifiableDictionary dictionary = (ModifiableDictionary) modHDT.getDictionary();
		dictionary.startProcessing();
		parser.doParse(filename, baseUri, notation, new DictionaryAppender(dictionary));
		dictionary.reorganize();
		
		// Load triples
		ModifiableTriples triples = (ModifiableTriples) modHDT.getTriples();
		parser.doParse(filename, baseUri, notation, new TripleAppender2(dictionary, triples));
		
		// SORT and duplicates
		String orderStr = spec.get("triples.component.order");
		if(orderStr==null)
			orderStr = "SPO";
		
		StopWatch sortDupTime = new StopWatch();
		triples.sort(TripleComponentOrder.valueOf(orderStr), listener);
		triples.removeDuplicates(listener);
		System.out.println("Sort triples and remove duplicates: "+sortDupTime.stopAndShow());
		
		return modHDT;
	}


}
