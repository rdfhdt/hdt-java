package org.rdfhdt.hdt.hdt.writer;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.rdfhdt.hdt.dictionary.TempDictionary;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.hdt.TempHDT;
import org.rdfhdt.hdt.hdt.impl.HDTImpl;
import org.rdfhdt.hdt.hdt.impl.ModeOfLoading;
import org.rdfhdt.hdt.hdt.impl.TempHDTImpl;
import org.rdfhdt.hdt.header.HeaderUtil;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.rdf.TripleWriter;
import org.rdfhdt.hdt.triples.TempTriples;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.util.StopWatch;

public class TripleWriterHDT implements TripleWriter {

	private OutputStream out;
	private boolean close=false;
	HDTOptions spec;
	String baseUri;
	
	
	StopWatch st = new StopWatch();
	TempHDT modHDT;
	TempDictionary dictionary;
	TempTriples triples;
	long num=0;
	long size=0;
	
	public TripleWriterHDT(String baseUri, HDTOptions spec, String outFile, boolean compress) throws IOException {
		this.baseUri=baseUri;
		this.spec=spec;
		if(compress) {
			this.out = new BufferedOutputStream(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(outFile))));
		} else {
			this.out = new BufferedOutputStream(new FileOutputStream(outFile));
		}
		close = true;
		init();
	}

	public TripleWriterHDT(String baseUri, HDTOptions spec, OutputStream out) {
		this.baseUri=baseUri;
		this.spec=spec;
		this.out = new BufferedOutputStream(out);
		init();
	}
	
	private void init() {
		// Create TempHDT
		modHDT = new TempHDTImpl(spec, baseUri, ModeOfLoading.ONE_PASS);
		dictionary = modHDT.getDictionary();
		triples = modHDT.getTriples();

        // Load RDF in the dictionary and generate triples
        dictionary.startProcessing();
	}
	
	
	@Override
	public void addTriple(TripleString triple) throws IOException {
		triples.insert(
    			dictionary.insert(triple.getSubject(), TripleComponentRole.SUBJECT),
    			dictionary.insert(triple.getPredicate(), TripleComponentRole.PREDICATE),
    			dictionary.insert(triple.getObject(), TripleComponentRole.OBJECT)
    			);
    	num++;
		size+=triple.getSubject().length()+triple.getPredicate().length()+triple.getObject().length()+4;  // Spaces and final dot
	}

	@Override
	public void close() throws IOException {
		ProgressListener listener=null;
		
        dictionary.endProcessing();
		
		// Reorganize both the dictionary and the triples
		modHDT.reorganizeDictionary(listener);
		modHDT.reorganizeTriples(listener);
		
		modHDT.getHeader().insert( "_:statistics", HDTVocabulary.ORIGINAL_SIZE, size);
				
		
		// Convert to HDT
		HDTImpl hdt = new HDTImpl(spec); 
		hdt.loadFromModifiableHDT(modHDT, listener);
		hdt.populateHeaderStructure(modHDT.getBaseURI());
		
		// Add file size to Header
		try {
			long originalSize = HeaderUtil.getPropertyLong(modHDT.getHeader(), "_:statistics", HDTVocabulary.ORIGINAL_SIZE);
			hdt.getHeader().insert("_:statistics", HDTVocabulary.ORIGINAL_SIZE, originalSize);
		} catch (NotFoundException e) {
		}
		
		modHDT.close();
		
		hdt.saveToHDT(out, listener);
		hdt.close();
		
		if(close) {
			out.close();
		} else {
			out.flush();
		}
	}

}
