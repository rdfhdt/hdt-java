package org.rdfhdt.hdt.hdt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import org.rdfhdt.hdt.compact.bitmap.Bitmap;
import org.rdfhdt.hdt.enums.CompressionType;
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
	 * @param hdtFileName file path to load
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @param hdtFormat Parameters to tune the loaded HDT index. Can be null for default options.
	 * @throws IOException when the file cannot be found
	 * @return HDT
	 */
	public static HDT loadHDT(String hdtFileName, ProgressListener listener, HDTOptions hdtFormat) throws IOException {
		return HDTManager.getInstance().doLoadHDT(hdtFileName, listener, hdtFormat);
	}

	/**
	 * Load an HDT file into memory to use it. NOTE: Use this method to go through all elements. If you plan
	 * to do queries, use loadIndexedHDT() instead.
	 * @param hdtFileName file path to load
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @throws IOException when the file cannot be found
	 * @return HDT
	 */
	public static HDT loadHDT(String hdtFileName, ProgressListener listener) throws IOException {
		return HDTManager.getInstance().doLoadHDT(hdtFileName, listener, null);
	}

	/**
	 * Load an HDT file into memory to use it. NOTE: Use this method to go through all elements. If you plan
	 * to do queries, use loadIndexedHDT() instead.
	 * @param hdtFileName file path to load
	 * @throws IOException when the file cannot be found
	 * @return HDT
	 */
	public static HDT loadHDT(String hdtFileName) throws IOException {
		return HDTManager.getInstance().doLoadHDT(hdtFileName, null, null);
	}

	/**
	 * Map an HDT file into memory to use it. This method does not load the whole file into memory,
	 * it lets the OS to handle memory pages as desired. Therefore it uses less memory but can be slower
	 * for querying because it needs to load those blocks from disk.
	 * NOTE: Use this method to go through all elements. If you plan to do queries, use mapIndexedHDT() instead.
	 * @param hdtFileName file path to map
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @param hdtFormat Parameters to tune the loaded HDT index. Can be null for default options.
	 * @throws IOException when the file cannot be found
	 * @return HDT
	 */
	public static HDT mapHDT(String hdtFileName, ProgressListener listener, HDTOptions hdtFormat) throws IOException {
		return HDTManager.getInstance().doMapHDT(hdtFileName, listener, hdtFormat);
	}
	/**
	 * Map an HDT file into memory to use it. This method does not load the whole file into memory,
	 * it lets the OS to handle memory pages as desired. Therefore it uses less memory but can be slower
	 * for querying because it needs to load those blocks from disk.
	 * NOTE: Use this method to go through all elements. If you plan to do queries, use mapIndexedHDT() instead.
	 * @param hdtFileName file path to map
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @throws IOException when the file cannot be found
	 * @return HDT
	 */
	public static HDT mapHDT(String hdtFileName, ProgressListener listener) throws IOException {
		return HDTManager.getInstance().doMapHDT(hdtFileName, listener, null);
	}
	/**
	 * Map an HDT file into memory to use it. This method does not load the whole file into memory,
	 * it lets the OS to handle memory pages as desired. Therefore it uses less memory but can be slower
	 * for querying because it needs to load those blocks from disk.
	 * NOTE: Use this method to go through all elements. If you plan to do queries, use mapIndexedHDT() instead.
	 * @param hdtFileName file path to map
	 * @throws IOException when the file cannot be found
	 * @return HDT
	 */
	public static HDT mapHDT(String hdtFileName) throws IOException {
		return HDTManager.getInstance().doMapHDT(hdtFileName, null, null);
	}

	/**
	 * Load an HDT from an InputStream (File, socket...). NOTE: Use this method to go through all elements. If you plan
	 * to do queries, use loadIndexedHDT() instead.
	 * @param hdtFile file path to load
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @param hdtFormat Parameters to tune the loaded HDT index. Can be null for default options.
	 * @throws IOException when the file cannot be found
	 * @return HDT
	 */
	public static HDT loadHDT(InputStream hdtFile, ProgressListener listener, HDTOptions hdtFormat) throws IOException {
		return HDTManager.getInstance().doLoadHDT(hdtFile, listener, hdtFormat);
	}
	/**
	 * Load an HDT from an InputStream (File, socket...). NOTE: Use this method to go through all elements. If you plan
	 * to do queries, use loadIndexedHDT() instead.
	 * @param hdtFile file path to load
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @throws IOException when the file cannot be found
	 * @return HDT
	 */
	public static HDT loadHDT(InputStream hdtFile, ProgressListener listener) throws IOException {
		return HDTManager.getInstance().doLoadHDT(hdtFile, listener, null);
	}
	/**
	 * Load an HDT from an InputStream (File, socket...). NOTE: Use this method to go through all elements. If you plan
	 * to do queries, use loadIndexedHDT() instead.
	 * @param hdtFile file path to load
	 * @throws IOException when the file cannot be found
	 * @return HDT
	 */
	public static HDT loadHDT(InputStream hdtFile) throws IOException {
		return HDTManager.getInstance().doLoadHDT(hdtFile, null, null);
	}

	/**
	 * Load an HDT File, and load/create additional indexes to support all kind of queries efficiently.
	 * @param hdtFileName file path to load
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @param hdtFormat Parameters to tune the loaded HDT index. Can be null for default options.
	 * @throws IOException when the file cannot be found
	 * @return HDT
	 */
	public static HDT loadIndexedHDT(String hdtFileName, ProgressListener listener, HDTOptions hdtFormat) throws IOException {
		return HDTManager.getInstance().doLoadIndexedHDT(hdtFileName, listener, hdtFormat);
	}

	/**
	 * Load an HDT File, and load/create additional indexes to support all kind of queries efficiently.
	 * @param hdtFileName file path to load
	 * @param listener – Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @throws IOException when the file cannot be found
	 * @return HDT
	 */
	public static HDT loadIndexedHDT(String hdtFileName, ProgressListener listener) throws IOException {
		return HDTManager.getInstance().doLoadIndexedHDT(hdtFileName, listener, null);
	}
	/**
	 * Load an HDT File, and load/create additional indexes to support all kind of queries efficiently.
	 * @param hdtFileName file path to load
	 * @throws IOException when the file cannot be found
	 * @return HDT
	 */
	public static HDT loadIndexedHDT(String hdtFileName) throws IOException {
		return HDTManager.getInstance().doLoadIndexedHDT(hdtFileName, null, null);
	}

	/**
	 * Maps an HDT File into virtual memory, and load/create additional indexes to support all kind of queries efficiently.
	 * @param hdtFileName file path to map
	 * @param spec HDTOptions to the new mapped HDT. Can be null for default options.
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @throws IOException when the file cannot be found
	 * @return HDT
	 */
	public static HDT mapIndexedHDT(String hdtFileName, HDTOptions spec, ProgressListener listener) throws IOException {
		return HDTManager.getInstance().doMapIndexedHDT(hdtFileName, listener, spec);
	}

	/**
	 * Maps an HDT File into virtual memory, and load/create additional indexes to support all kind of queries efficiently.
	 * @param hdtFileName file path to map
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @throws IOException when the file cannot be found
	 * @return HDT
	 */
	public static HDT mapIndexedHDT(String hdtFileName, ProgressListener listener) throws IOException {
		return HDTManager.getInstance().doMapIndexedHDT(hdtFileName, listener, null);
	}

	/**
	 * Maps an HDT File into virtual memory, and load/create additional indexes to support all kind of queries efficiently.
	 * @param hdtFileName file path to map
	 * @throws IOException when the file cannot be found
	 * @return HDT
	 */
	public static HDT mapIndexedHDT(String hdtFileName) throws IOException {
		return HDTManager.getInstance().doMapIndexedHDT(hdtFileName, null, null);
	}

	/**
	 * Load an HDT file from InputStream, and create additional indexes to support all kind of queries efficiently.
	 * @param hdtFileName file path to load
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @throws IOException when the file cannot be found
	 * @return HDT
	 */
	public static HDT loadIndexedHDT(InputStream hdtFileName, ProgressListener listener) throws IOException {
		return HDTManager.getInstance().doLoadIndexedHDT(hdtFileName, listener, null);
	}

	/**
	 * Load an HDT file from InputStream, and create additional indexes to support all kind of queries efficiently.
	 * @param hdtFileName file path to load
	 * @throws IOException when the file cannot be found
	 * @return HDT
	 */
	public static HDT loadIndexedHDT(InputStream hdtFileName) throws IOException {
		return HDTManager.getInstance().doLoadIndexedHDT(hdtFileName, null, null);
	}
	/**
	 * Load an HDT file from InputStream, and create additional indexes to support all kind of queries efficiently.
	 * @param hdtFileName file path to load
	 * @param hdtFormat Parameters to tune the loaded HDT. Can be null for default options.
	 * @throws IOException when the file cannot be found
	 * @return HDT
	 */
	public static HDT loadIndexedHDT(InputStream hdtFileName, ProgressListener listener, HDTOptions hdtFormat) throws IOException {
		return HDTManager.getInstance().doLoadIndexedHDT(hdtFileName, listener, hdtFormat);
	}

	/**
	 * Return an indexed HDT that is efficient for all kind of queries, given a not indexed HDT.
	 * @param hdt file path to index
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @throws IOException when the file cannot be found
	 * @return HDT
	 */
	public static HDT indexedHDT(HDT hdt, ProgressListener listener) throws IOException {
		return HDTManager.getInstance().doIndexedHDT(hdt, listener);
	}

	/**
	 * Create an HDT file from an RDF file.
	 * @param rdfFileName File name.
	 * @param baseURI Base URI for the dataset.
	 * @param rdfNotation Format of the source RDF File (NTriples, N3, RDF-XML...)
	 * @param hdtFormat Parameters to tune the generated HDT.
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 *
	 * @throws IOException when the file cannot be found
	 * @throws ParserException when the file cannot be parsed
	 * @return HDT
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
	 * @throws IOException when the file cannot be found
	 * @throws ParserException when the file cannot be parsed
	 * @return HDT
	 */
	public static HDT generateHDT(Iterator<TripleString> iterator, String baseURI, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException {
		return HDTManager.getInstance().doGenerateHDT(iterator, baseURI, hdtFormat, listener);
	}
	/**
	 * Create an HDT file from a RDF stream.
	 * @param fileStream RDF stream to parse.
	 * @param baseURI Base URI for the dataset.
	 * @param filename the RDF file name to guess the stream format and compresion.
	 * @param hdtFormat Parameters to tune the generated HDT.
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @return HDT
	 * @throws IOException when the stream cannot be used
	 * @throws ParserException when the RDF stream can't be parsed
	 */
	public static HDT generateHDT(InputStream fileStream, String baseURI, String filename, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException {
		return HDTManager.getInstance().doGenerateHDT(fileStream, baseURI, RDFNotation.guess(filename), CompressionType.guess(filename), hdtFormat, listener);
	}
	/**
	 * Create an HDT file from a RDF stream.
	 * @param fileStream RDF stream to parse.
	 * @param baseURI Base URI for the dataset.
	 * @param rdfNotation Format of the source RDF stream (NTriples, N3, RDF-XML...)
	 * @param compressionType Compression type of the RDF stream. (GZIP, ZIP...)
	 * @param hdtFormat Parameters to tune the generated HDT.
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @return HDT
	 * @throws IOException when the stream cannot be used
	 * @throws ParserException when the RDF stream can't be parsed
	 */
	public static HDT generateHDT(InputStream fileStream, String baseURI, RDFNotation rdfNotation, CompressionType compressionType, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException {
		return HDTManager.getInstance().doGenerateHDT(fileStream, baseURI, rdfNotation, compressionType, hdtFormat, listener);
	}
	/**
	 * Create an HDT file from a RDF stream.
	 * @param fileStream RDF stream to parse.
	 * @param baseURI Base URI for the dataset.
	 * @param rdfNotation Format of the source RDF stream (NTriples, N3, RDF-XML...)
	 * @param hdtFormat Parameters to tune the generated HDT.
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @return HDT
	 * @throws IOException when the stream cannot be used
	 * @throws ParserException when the RDF stream can't be parsed
	 */
	public static HDT generateHDT(InputStream fileStream, String baseURI, RDFNotation rdfNotation, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException {
		return HDTManager.getInstance().doGenerateHDT(fileStream, baseURI, rdfNotation, CompressionType.NONE, hdtFormat, listener);
	}

	/**
	 * Create an HDT file from an RDF file by sorting the triples on disk, reduce the memory required by increasing the
	 * IO usage.
	 * @param rdfFileName RDF file to parse.
	 * @param baseURI Base URI for the dataset.
	 * @param rdfNotation Format of the source RDF File (NTriples, N3, RDF-XML...)
	 * @param compressionType Compression type of the RDF file. (GZIP, ZIP...)
	 * @param hdtFormat Parameters to tune the generated HDT.
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @return HDT
	 * @throws IOException when the file cannot be found
	 * @throws ParserException when the RDF file can't be parsed
	 */
	public static HDT generateHDTDisk(String rdfFileName, String baseURI, RDFNotation rdfNotation, CompressionType compressionType, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException {
		return HDTManager.getInstance().doGenerateHDTDisk(rdfFileName, baseURI, rdfNotation, compressionType, hdtFormat, listener);
	}
	/**
	 * Create an HDT file from an RDF file without compression by sorting the triples on disk, reduce the memory
	 * required by increasing the IO usage.
	 * @param rdfFileName RDF file to parse.
	 * @param baseURI Base URI for the dataset.
	 * @param rdfNotation Format of the source RDF File (NTriples, N3, RDF-XML...)
	 * @param hdtFormat Parameters to tune the generated HDT.
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @return HDT
	 * @throws IOException when the file cannot be found
	 * @throws ParserException when the RDF file can't be parsed
	 */
	public static HDT generateHDTDisk(String rdfFileName, String baseURI, RDFNotation rdfNotation, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException {
		return HDTManager.getInstance().doGenerateHDTDisk(rdfFileName, baseURI, rdfNotation, CompressionType.NONE, hdtFormat, listener);
	}
	/**
	 * Create an HDT file from an RDF file by sorting the triples on disk, reduce the memory required by increasing the
	 * IO usage. Will guess the RDF file compression/format with the file name.
	 * @param rdfFileName RDF file to parse.
	 * @param baseURI Base URI for the dataset.
	 * @param hdtFormat Parameters to tune the generated HDT.
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @return HDT
	 * @throws IOException when the file cannot be found
	 * @throws ParserException when the RDF file can't be parsed
	 */
	public static HDT generateHDTDisk(String rdfFileName, String baseURI, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException {
		return HDTManager.getInstance().doGenerateHDTDisk(rdfFileName, baseURI, RDFNotation.guess(rdfFileName), CompressionType.guess(rdfFileName), hdtFormat, listener);
	}
	/**
	 * Create an HDT file from an RDF stream by sorting the triples on disk, reduce the memory required by increasing
	 * the IO usage.
	 * @param fileStream RDF stream to parse.
	 * @param baseURI Base URI for the dataset.
	 * @param filename the RDF file name to guess the stream format and compresion.
	 * @param hdtFormat Parameters to tune the generated HDT.
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @return HDT
	 * @throws IOException when the stream cannot be used
	 * @throws ParserException when the RDF stream can't be parsed
	 */
	public static HDT generateHDTDisk(InputStream fileStream, String baseURI, String filename, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException {
		return HDTManager.getInstance().doGenerateHDTDisk(fileStream, baseURI, RDFNotation.guess(filename), CompressionType.guess(filename), hdtFormat, listener);
	}
	/**
	 * Create an HDT file from an RDF stream by sorting the triples on disk, reduce the memory required by increasing
	 * the IO usage.
	 * @param fileStream RDF stream to parse.
	 * @param baseURI Base URI for the dataset.
	 * @param rdfNotation Format of the source RDF stream (NTriples, N3, RDF-XML...)
	 * @param compressionType Compression type of the RDF stream. (GZIP, ZIP...)
	 * @param hdtFormat Parameters to tune the generated HDT.
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @return HDT
	 * @throws IOException when the stream cannot be used
	 * @throws ParserException when the RDF stream can't be parsed
	 */
	public static HDT generateHDTDisk(InputStream fileStream, String baseURI, RDFNotation rdfNotation, CompressionType compressionType, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException {
		return HDTManager.getInstance().doGenerateHDTDisk(fileStream, baseURI, rdfNotation, compressionType, hdtFormat, listener);
	}
	/**
	 * Create an HDT file from an RDF stream by sorting the triples on disk, reduce the memory required by increasing
	 * the IO usage.
	 * @param fileStream RDF stream to parse.
	 * @param baseURI Base URI for the dataset.
	 * @param rdfNotation Format of the source RDF stream (NTriples, N3, RDF-XML...)
	 * @param hdtFormat Parameters to tune the generated HDT.
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @return HDT
	 * @throws IOException when the stream cannot be used
	 * @throws ParserException when the RDF stream can't be parsed
	 */
	public static HDT generateHDTDisk(InputStream fileStream, String baseURI, RDFNotation rdfNotation, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException {
		return HDTManager.getInstance().doGenerateHDTDisk(fileStream, baseURI, rdfNotation, CompressionType.NONE, hdtFormat, listener);
	}
	/**
	 * Create an HDT file from an RDF stream by sorting the triples on disk, reduce the memory required by increasing
	 * the IO usage.
	 * @param baseURI Base URI for the dataset.
	 * @param hdtFormat Parameters to tune the generated HDT.
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @return HDT
	 * @throws IOException when the stream cannot be used
	 */
	public static HDT generateHDTDisk(Iterator<TripleString> iterator, String baseURI, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException {
		return HDTManager.getInstance().doGenerateHDTDisk(iterator, baseURI, hdtFormat, listener);
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
	 * @throws IOException when the file cannot be found
	 * @return HDT
	 */
	public static HDT catHDT(String location, String hdtFileName1, String hdtFileName2, HDTOptions hdtFormat, ProgressListener listener) throws IOException {
		return HDTManager.getInstance().doHDTCat(location, hdtFileName1, hdtFileName2, hdtFormat, listener);
	}
	/**
	 * Create a new HDT by removing from hdt1 the triples of hdt2.
	 * @param hdtFileName1 First hdt file name
	 * @param hdtFileName2 Second hdt file name
	 * @param hdtFormat Parameters to tune the generated HDT.
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @return HDT
	 * @throws IOException when the file cannot be found
	 */
	public static HDT diffHDT(String hdtFileName1, String hdtFileName2, HDTOptions hdtFormat, ProgressListener listener) throws IOException {
		return HDTManager.getInstance().doHDTDiff(hdtFileName1, hdtFileName2, hdtFormat, listener);
	}

	/**
	 * Create a new HDT by removing from a hdt all the triples marked to delete in a bitmap
	 * @param location where the new HDT file is stored
	 * @param hdtFileName hdt file name
	 * @param deleteBitmap delete bitmap
	 * @param hdtFormat  Parameters to tune the generated HDT.
	 * @param listener Listener to get notified of loading progress. Can be null if no notifications needed.
	 * @return HDT
	 * @throws IOException when the file cannot be found
	 */
	public static HDT diffHDTBit(String location, String hdtFileName, Bitmap deleteBitmap, HDTOptions hdtFormat, ProgressListener listener) throws IOException {
		return HDTManager.getInstance().doHDTDiffBit(location, hdtFileName, deleteBitmap, hdtFormat, listener);
	}

	// Abstract methods for the current implementation
	protected abstract HDTOptions doReadOptions(String file) throws IOException;
	protected abstract HDT doLoadHDT(String hdtFileName, ProgressListener listener, HDTOptions spec) throws IOException;
	protected abstract HDT doLoadHDT(InputStream hdtFile, ProgressListener listener, HDTOptions spec) throws IOException;
	protected abstract HDT doMapHDT(String hdtFileName, ProgressListener listener, HDTOptions spec) throws IOException;
	protected abstract HDT doLoadIndexedHDT(String hdtFileName, ProgressListener listener, HDTOptions spec) throws IOException;
	protected abstract HDT doLoadIndexedHDT(InputStream hdtFileName, ProgressListener listener, HDTOptions spec) throws IOException;
	protected abstract HDT doMapIndexedHDT(String hdtFileName, ProgressListener listener, HDTOptions spec) throws IOException;
	protected abstract HDT doIndexedHDT(HDT hdt, ProgressListener listener) throws IOException;
	protected abstract HDT doGenerateHDT(String rdfFileName, String baseURI, RDFNotation rdfNotation, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException;
	protected abstract HDT doGenerateHDT(InputStream fileStream, String baseURI, RDFNotation rdfNotation, CompressionType compressionType, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException;
	protected abstract HDT doGenerateHDT(Iterator<TripleString> iterator, String baseURI,	HDTOptions hdtFormat, ProgressListener listener) throws IOException;
	protected abstract HDT doGenerateHDTDisk(String rdfFileName, String baseURI, RDFNotation rdfNotation, CompressionType compressionType, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException;
	protected abstract HDT doGenerateHDTDisk(InputStream fileStream, String baseURI, RDFNotation rdfNotation, CompressionType compressionType, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException;
	protected abstract HDT doGenerateHDTDisk(Iterator<TripleString> iterator, String baseURI, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException;
	protected abstract TripleWriter doGetHDTWriter(OutputStream out, String baseURI, HDTOptions hdtFormat) throws IOException;
	protected abstract TripleWriter doGetHDTWriter(String outFile, String baseURI, HDTOptions hdtFormat) throws IOException;
	protected abstract HDT doHDTCat(String location, String hdtFileName1, String hdtFileName2, HDTOptions hdtFormat, ProgressListener listener) throws IOException;
	protected abstract HDT doHDTDiff(String hdtFileName1, String hdtFileName2, HDTOptions hdtFormat, ProgressListener listener) throws IOException;
	protected abstract HDT doHDTDiffBit(String location, String hdtFileName, Bitmap deleteBitmap, HDTOptions hdtFormat, ProgressListener listener) throws IOException;

}
