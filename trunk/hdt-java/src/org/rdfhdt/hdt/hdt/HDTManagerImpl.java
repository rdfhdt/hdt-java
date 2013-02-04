package org.rdfhdt.hdt.hdt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.impl.HDTImpl;
import org.rdfhdt.hdt.hdt.impl.TempHDTImporterOnePass;
import org.rdfhdt.hdt.hdt.impl.TempHDTImporterTwoPass;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTSpecification;
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
		if ("one-pass".equals(loaderType)){
			loader = new TempHDTImporterOnePass();
		} else if ("two-pass".equals(loaderType)) {
			loader = new TempHDTImporterTwoPass();
		} else {
			System.err.println("Loader type not specified: using one-pass");
			spec.set("loader.type", "one-pass");
			loader = new TempHDTImporterOnePass();
		}
		
		StopWatch st = new StopWatch();
		
		// Create TempHDT
		TempHDT modHdt = loader.loadFromRDF(spec, rdfFileName, baseURI, rdfNotation, listener);
		
		// Convert to HDT
		HDTImpl hdt = new HDTImpl(spec); 
		hdt.loadFromModifiableHDT(modHdt, listener);
		hdt.populateHeaderStructure(modHdt.getBaseURI());
		
		// Add file size to Header
		hdt.getHeader().insert("_:statistics", HDTVocabulary.ORIGINAL_SIZE, new File(rdfFileName).length());
		
		System.out.println("File converted in: "+st.stopAndShow());
		
		modHdt.close();
		
		return hdt;
	}

}
