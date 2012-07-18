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

public class ModHDTImporterOnePass implements ModHDTImporter {

	class TripleAppender implements RDFCallback {
		ModifiableDictionary dict;
		ModifiableTriples triples;

		public TripleAppender(ModifiableDictionary dict, ModifiableTriples triples) {
			this.dict = dict;
			this.triples = triples;
		}

		public void processTriple(TripleString triple, long pos) {
			triples.insert(
					dict.insert(triple.getSubject(), TripleComponentRole.SUBJECT),
					dict.insert(triple.getPredicate(), TripleComponentRole.PREDICATE),
					dict.insert(triple.getObject(), TripleComponentRole.OBJECT)
			);
		}
	};

	private void reorganize(HDTRW modHDT, HDTSpecification spec, ProgressListener listener) {
		ModifiableDictionary dictionary = modHDT.dictionary;
		ModifiableTriples triples = modHDT.triples;
		
		// Reorganize dictionary and update IDs on Triples accordingly.
		StopWatch reorgStp = new StopWatch();
		dictionary.reorganize(triples);
		System.out.println("Dictionary reorganized in "+reorgStp.stopAndShow());

		// Reorganize triples.
		String orderStr = spec.get("triples.component.order");
		if(orderStr==null) {
			orderStr = "SPO";
		}
		StopWatch sortDupTime = new StopWatch();
		triples.sort(TripleComponentOrder.valueOf(orderStr), listener);
		triples.removeDuplicates(listener);
		System.out.println("Sort triples and remove duplicates: "+sortDupTime.stopAndShow());
	}
	
	@Override
	public ModifiableHDT loadFromRDF(HDTSpecification spec, String filename, String baseUri, RDFNotation notation, ProgressListener listener)
			throws IOException, ParserException {
		
		// Create Modifiable Instance
		HDTRW modHDT = new HDTRW(new HDTSpecification());
		ModifiableDictionary dictionary = modHDT.dictionary;
		ModifiableTriples triples = modHDT.triples;
		
        RDFParserCallback parser = RDFParserFactory.getParserCallback(notation);

        // Import all triples
        dictionary.startProcessing();
        parser.doParse(filename, baseUri, notation, new TripleAppender(dictionary, triples));
		
		// Reorganize
		this.reorganize(modHDT, spec, listener);
		
		return modHDT; 
	}

}
