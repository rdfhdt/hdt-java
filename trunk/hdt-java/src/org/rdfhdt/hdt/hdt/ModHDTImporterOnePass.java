package org.rdfhdt.hdt.hdt;

import java.io.IOException;

import org.rdfhdt.hdt.dictionary.ModifiableDictionary;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.listener.ListenerUtil;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.rdf.RDFParserCallback;
import org.rdfhdt.hdt.rdf.RDFParserCallback.RDFCallback;
import org.rdfhdt.hdt.rdf.RDFParserFactory;
import org.rdfhdt.hdt.triples.ModifiableTriples;
import org.rdfhdt.hdt.triples.TripleString;

public class ModHDTImporterOnePass implements ModHDTImporter {

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
	public ModifiableHDT loadFromRDF(HDTSpecification spec, String filename, String baseUri, RDFNotation notation, ProgressListener listener)
			throws IOException, ParserException {
		
		// Create Modifiable Instance
		/* HDTRW modHDT = new HDTRW(spec); */
		ModifiableHDT modHDT = HDTFactory.createModifiableHDT(spec, baseUri);
		ModifiableDictionary dictionary = (ModifiableDictionary)modHDT.getDictionary();
		ModifiableTriples triples = (ModifiableTriples)modHDT.getTriples();
		
        RDFParserCallback parser = RDFParserFactory.getParserCallback(notation);

        // Import all triples
        dictionary.startProcessing();
        parser.doParse(filename, baseUri, notation, new TripleAppender(dictionary, triples, listener));
        dictionary.endProcessing();
		
		// Reorganize
		modHDT.reorganize(listener);
		
		//modHDT.baseUri = baseUri;
		
		return modHDT; 
	}

}
