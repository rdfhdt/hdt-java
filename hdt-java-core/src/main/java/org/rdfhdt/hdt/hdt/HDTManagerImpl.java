package org.rdfhdt.hdt.hdt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.impl.HDTImpl;
import org.rdfhdt.hdt.hdt.impl.TempHDTImporterOnePass;
import org.rdfhdt.hdt.hdt.impl.TempHDTImporterTwoPass;
import org.rdfhdt.hdt.hdt.writer.TripleWriterHDT;
import org.rdfhdt.hdt.header.HeaderUtil;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.rdf.TripleWriter;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.util.StopWatch;

public class HDTManagerImpl extends HDTManager {

	@Override
	public HDTOptions doReadOptions(String file) throws IOException {
		return new HDTSpecification(file);
	}

	@Override
	public HDT doLoadHDT(String hdtFileName, ProgressListener listener) throws IOException {
		HDTPrivate hdt = new HDTImpl(new HDTSpecification());
		hdt.loadFromHDT(hdtFileName, listener);
		return hdt;
	}
	
	@Override
	protected HDT doMapHDT(String hdtFileName, ProgressListener listener) throws IOException {
		HDTPrivate hdt = new HDTImpl(new HDTSpecification());
		hdt.mapFromHDT(new File(hdtFileName), 0, listener);
		return hdt;
	}


	@Override
	public HDT doLoadHDT(InputStream hdtFile, ProgressListener listener) throws IOException {
		HDTPrivate hdt = new HDTImpl(new HDTSpecification());
		hdt.loadFromHDT(hdtFile, listener);
		return hdt;
	}

	@Override
	public HDT doLoadIndexedHDT(String hdtFileName, ProgressListener listener) throws IOException {
		HDTPrivate hdt = new HDTImpl(new HDTSpecification());
		hdt.loadFromHDT(hdtFileName, listener);
		hdt.loadOrCreateIndex(listener);
		return hdt;
	}
	


	@Override
	protected HDT doMapIndexedHDT(String hdtFileName, ProgressListener listener) throws IOException {
		HDTPrivate hdt = new HDTImpl(new HDTSpecification());
		hdt.mapFromHDT(new File(hdtFileName), 0, listener);
		hdt.loadOrCreateIndex(listener);
		return hdt;
	}

	@Override
	public HDT doLoadIndexedHDT(InputStream hdtFile, ProgressListener listener) throws IOException {
		HDTPrivate hdt = new HDTImpl(new HDTSpecification());
		hdt.loadFromHDT(hdtFile, listener);
		hdt.loadOrCreateIndex(listener);
		return hdt;
	}

	@Override
	public HDT doIndexedHDT(HDT hdt, ProgressListener listener) {
		((HDTPrivate)hdt).loadOrCreateIndex(listener);
		return hdt;
	}

	@Override
	public HDT doGenerateHDT(String rdfFileName, String baseURI, RDFNotation rdfNotation, HDTOptions spec, ProgressListener listener) throws IOException, ParserException {
		//choose the importer
		String loaderType = spec.get("loader.type");
		TempHDTImporter loader;
		if ("two-pass".equals(loaderType)) {
			loader = new TempHDTImporterTwoPass();
		} else {
			loader = new TempHDTImporterOnePass();
		}
		
		// Create TempHDT
		TempHDT modHdt = loader.loadFromRDF(spec, rdfFileName, baseURI, rdfNotation, listener);
		
		// Convert to HDT
		HDTImpl hdt = new HDTImpl(spec); 
		hdt.loadFromModifiableHDT(modHdt, listener);
		hdt.populateHeaderStructure(modHdt.getBaseURI());
		
		// Add file size to Header
		try {
			long originalSize = HeaderUtil.getPropertyLong(modHdt.getHeader(), "_:statistics", HDTVocabulary.ORIGINAL_SIZE);
			hdt.getHeader().insert("_:statistics", HDTVocabulary.ORIGINAL_SIZE, originalSize);
		} catch (NotFoundException e) {
		}
		
		modHdt.close();
		
		return hdt;
	}

	@Override
	public HDT doGenerateHDT(Iterator<TripleString> triples, String baseURI, HDTOptions spec, ProgressListener listener) throws IOException {
		//choose the importer
		TempHDTImporterOnePass loader = new TempHDTImporterOnePass();

		// Create TempHDT
		TempHDT modHdt = loader.loadFromTriples(spec, triples, baseURI, listener);
		
		// Convert to HDT
		HDTImpl hdt = new HDTImpl(spec); 
		hdt.loadFromModifiableHDT(modHdt, listener);
		hdt.populateHeaderStructure(modHdt.getBaseURI());
		
		// Add file size to Header
		try {
			long originalSize = HeaderUtil.getPropertyLong(modHdt.getHeader(), "_:statistics", HDTVocabulary.ORIGINAL_SIZE);
			hdt.getHeader().insert("_:statistics", HDTVocabulary.ORIGINAL_SIZE, originalSize);
		} catch (NotFoundException e) {
		}
		
		modHdt.close();
		
		return hdt;
	}

	@Override
	protected TripleWriter doGetHDTWriter(OutputStream out, String baseURI, HDTOptions hdtFormat) throws IOException {
		return new TripleWriterHDT(baseURI, hdtFormat, out);
	}

	@Override
	protected TripleWriter doGetHDTWriter(String outFile, String baseURI, HDTOptions hdtFormat) throws IOException {
		return new TripleWriterHDT(baseURI, hdtFormat, outFile, false);
	}

	@Override
	public HDT doHDTCat(String location, String hdtFileName1, String hdtFileName2, HDTOptions hdtFormat, ProgressListener listener) throws IOException {
		StopWatch st = new StopWatch();
		HDT hdt1 = doMapHDT(hdtFileName1, listener);
		HDT hdt2 = doMapHDT(hdtFileName2, listener);
		HDTImpl hdt = new HDTImpl(new HDTSpecification());
		hdt.cat(location, hdt1, hdt2, listener);
		System.out.println("HDT file joint in: "+st.stopAndShow());
		return hdt;
	}
}