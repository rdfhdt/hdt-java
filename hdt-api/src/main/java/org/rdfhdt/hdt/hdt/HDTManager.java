package org.rdfhdt.hdt.hdt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.rdf.TripleWriter;
import org.rdfhdt.hdt.triples.TripleString;

public abstract class HDTManager {
	
	private static HDTManager instance;
	
	private static HDTManager getInstance() {
		if(instance==null) {
			try {
				// Try to instantiate pro
				Class<?> managerImplClass = Class.forName("org.rdfhdt.hdt.pro.HDTManagerProImpl");
				instance = (HDTManager) managerImplClass.newInstance();
			} catch (Exception e1) {
				try {
					// Pro not found, instantiate normal
					Class<?> managerImplClass = Class.forName("org.rdfhdt.hdt.hdt.HDTManagerImpl");
					instance = (HDTManager) managerImplClass.newInstance();
				} catch (ClassNotFoundException e) {
					throw new RuntimeException("Class org.rdfhdt.hdt.hdt.HDTManagerImpl not found. Did you include the HDT implementation jar?");
				} catch (InstantiationException e) {
					throw new RuntimeException("Cannot create implementation for HDTManager. Does the class org.rdfhdt.hdt.hdt.HDTManagerImpl inherit from HDTManager?");
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return instance;
	}
	
	public static HDTOptions readOptions(String file) throws IOException {
		return HDTManager.getInstance().doReadOptions(file);
	}
	
	/**
	 * Load an HDT file into memory to use it. NOTE: Use this method to go through all elements. If you plan 
	 * to do queries, use loadIndexedHDT() instead.
	 * @param hdtFileName
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @return
	 * @throws IOException 
	 */
	public static HDT loadHDT(String hdtFileName, ProgressListener listener) throws IOException {
		return HDTManager.getInstance().doLoadHDT(hdtFileName, listener);
	}
	
	/**
	 * Load an HDT file into memory to use it. NOTE: Use this method to go through all elements. If you plan 
	 * to do queries, use loadIndexedHDT() instead.
	 * @param hdtFileName
	 * @return
	 * @throws IOException 
	 */
	public static HDT loadHDT(String hdtFileName) throws IOException {
		return HDTManager.getInstance().doLoadHDT(hdtFileName, null);
	}
	
	/**
	 * Map an HDT file into memory to use it. This method does not load the whole file into memory,
	 * it lets the OS to handle memory pages as desired. Therefore it uses less memory but can be slower
	 * for querying because it needs to load those blocks from disk.
	 * NOTE: Use this method to go through all elements. If you plan to do queries, use mapIndexedHDT() instead.
	 * @param hdtFileName
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @return
	 * @throws IOException 
	 */
	public static HDT mapHDT(String hdtFileName, ProgressListener listener) throws IOException {
		return HDTManager.getInstance().doMapHDT(hdtFileName, listener);
	}
	
	/**
	 * Map an HDT file into memory to use it. This method does not load the whole file into memory,
	 * it lets the OS to handle memory pages as desired. Therefore it uses less memory but can be slower
	 * for querying because it needs to load those blocks from disk.
	 * NOTE: Use this method to go through all elements. If you plan to do queries, use mapIndexedHDT() instead.
	 * @param hdtFileName
	 * @return
	 * @throws IOException 
	 */
	public static HDT mapHDT(String hdtFileName) throws IOException {
		return HDTManager.getInstance().doMapHDT(hdtFileName, null);
	}

	/**
	 * Load an HDT from an InputStream (File, socket...). NOTE: Use this method to go through all elements. If you plan 
	 * to do queries, use loadIndexedHDT() instead. 
	 * @param hdtFile
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @return
	 * @throws IOException 
	 */
	public static HDT loadHDT(InputStream hdtFile, ProgressListener listener) throws IOException {
		return HDTManager.getInstance().doLoadHDT(hdtFile, listener);
	}
	
	/**
	 * Load an HDT from an InputStream (File, socket...). NOTE: Use this method to go through all elements. If you plan 
	 * to do queries, use loadIndexedHDT() instead. 
	 * @param hdtFile
	 * @return
	 * @throws IOException 
	 */
	public static HDT loadHDT(InputStream hdtFile) throws IOException {
		return HDTManager.getInstance().doLoadHDT(hdtFile, null);
	}
	
	/**
	 * Load an HDT File, and load/create additional indexes to support all kind of queries efficiently.
	 * @param hdtFileName
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @return
	 * @throws IOException 
	 */
	public static HDT loadIndexedHDT(String hdtFileName, ProgressListener listener) throws IOException {
		return HDTManager.getInstance().doLoadIndexedHDT(hdtFileName, listener);
	}
	
	/**
	 * Load an HDT File, and load/create additional indexes to support all kind of queries efficiently.
	 * @param hdtFileName
	 * @return
	 * @throws IOException 
	 */
	public static HDT loadIndexedHDT(String hdtFileName) throws IOException {
		return HDTManager.getInstance().doLoadIndexedHDT(hdtFileName, null);
	}
	
	/**
	 * Maps an HDT File into virtual memory, and load/create additional indexes to support all kind of queries efficiently.
	 * @param hdtFileName
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @return
	 * @throws IOException 
	 */
	public static HDT mapIndexedHDT(String hdtFileName, ProgressListener listener) throws IOException {
		return HDTManager.getInstance().doMapIndexedHDT(hdtFileName, listener);
	}
	
	/**
	 * Maps an HDT File into virtual memory, and load/create additional indexes to support all kind of queries efficiently.
	 * @param hdtFileName
	 * @return
	 * @throws IOException 
	 */
	public static HDT mapIndexedHDT(String hdtFileName) throws IOException {
		return HDTManager.getInstance().doMapIndexedHDT(hdtFileName, null);
	}
	
	/**
	 * Load an HDT file from InputStream, and create additional indexes to support all kind of queries efficiently.
	 * @param hdtFileName
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @return
	 * @throws IOException 
	 */
	public static HDT loadIndexedHDT(InputStream hdtFileName, ProgressListener listener) throws IOException {
		return HDTManager.getInstance().doLoadIndexedHDT(hdtFileName, listener);
	}
	
	/**
	 * Load an HDT file from InputStream, and create additional indexes to support all kind of queries efficiently.
	 * @param hdtFileName
	 * @return
	 * @throws IOException 
	 */
	public static HDT loadIndexedHDT(InputStream hdtFileName) throws IOException {
		return HDTManager.getInstance().doLoadIndexedHDT(hdtFileName, null);
	}
	
	/**
	 * Return an indexed HDT that is efficient for all kind of queries, given a not indexed HDT.
	 * @param hdt
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @return
	 */
	public static HDT indexedHDT(HDT hdt, ProgressListener listener) {
		return HDTManager.getInstance().doIndexedHDT(hdt, listener);
	}
	
	/**
	 * Create an HDT file from an RDF file.
	 * @param rdfFileName File name.
	 * @param baseURI Base URI for the dataset.
	 * @param rdfNotation Format of the source RDF File (NTriples, N3, RDF-XML...)
	 * @param hdtFormat Parameters to tune the generated HDT.
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @return
	 * @throws IOException 
	 * @throws ParserException 
	 */
	public static HDT generateHDT(String rdfFileName, String baseURI, RDFNotation rdfNotation, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException {
		return HDTManager.getInstance().doGenerateHDT(rdfFileName, baseURI, rdfNotation, hdtFormat, listener);
	}
	
	/**
	 * Create an HDT file from an RDF file.
	 * @param iterator A provider of triples. Must implement hasNext(), next() and estimatedNumResults.
	 * @param baseURI Base URI for the dataset.
	 * @param hdtFormat Parameters to tune the generated HDT.
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @return
	 * @throws IOException  
	 */
	public static HDT generateHDT(Iterator<TripleString> iterator, String baseURI, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException {
		return HDTManager.getInstance().doGenerateHDT(iterator, baseURI, hdtFormat, listener);
	}

	public static TripleWriter getHDTWriter(OutputStream out, String baseURI, HDTOptions hdtFormat) throws IOException {
		return HDTManager.getInstance().doGetHDTWriter(out, baseURI, hdtFormat);
	}

	public static TripleWriter getHDTWriter(String outFile, String baseURI, HDTOptions hdtFormat) throws IOException {
		return HDTManager.getInstance().doGetHDTWriter(outFile, baseURI, hdtFormat);
	}

	/**
	 * Create an HDT file from two HDT files by joining the triples.
	 * @param location where the new HDT file is stored
	 * @param hdtFileName1 First hdt file name
	 * @param hdtFileName2 Second hdt file name
	 * @param hdtFormat Parameters to tune the generated HDT.
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @return
	 * @throws IOException
	 */
	public static HDT catHDT(String location, String hdtFileName1, String hdtFileName2, HDTOptions hdtFormat, ProgressListener listener) throws IOException {
		return HDTManager.getInstance().doHDTCat(location, hdtFileName1, hdtFileName2, hdtFormat, listener);
	}

	// Abstract methods for the current implementation
	protected abstract HDTOptions doReadOptions(String file) throws IOException;
	protected abstract HDT doLoadHDT(String hdtFileName, ProgressListener listener) throws IOException;
	protected abstract HDT doLoadHDT(InputStream hdtFile, ProgressListener listener) throws IOException;
	protected abstract HDT doMapHDT(String hdtFileName, ProgressListener listener) throws IOException;
	protected abstract HDT doLoadIndexedHDT(String hdtFileName, ProgressListener listener) throws IOException;
	protected abstract HDT doLoadIndexedHDT(InputStream hdtFileName, ProgressListener listener) throws IOException;
	protected abstract HDT doMapIndexedHDT(String hdtFileName, ProgressListener listener) throws IOException;
	protected abstract HDT doIndexedHDT(HDT hdt, ProgressListener listener);
	protected abstract HDT doGenerateHDT(String rdfFileName, String baseURI, RDFNotation rdfNotation, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException;
	protected abstract HDT doGenerateHDT(Iterator<TripleString> iterator, String baseURI,	HDTOptions hdtFormat, ProgressListener listener) throws IOException;
	protected abstract TripleWriter doGetHDTWriter(OutputStream out, String baseURI, HDTOptions hdtFormat) throws IOException;
	protected abstract TripleWriter doGetHDTWriter(String outFile, String baseURI, HDTOptions hdtFormat) throws IOException;
	protected abstract HDT doHDTCat(String location, String hdtFileName1, String hdtFileName2, HDTOptions hdtFormat, ProgressListener listener) throws IOException;

}
