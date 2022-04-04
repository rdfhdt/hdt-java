package org.rdfhdt.hdt.hdt;

import org.rdfhdt.hdt.compact.bitmap.Bitmap;
import org.rdfhdt.hdt.dictionary.DictionaryPrivate;
import org.rdfhdt.hdt.dictionary.impl.CompressFourSectionDictionary;
import org.rdfhdt.hdt.dictionary.impl.MultipleSectionDictionary;
import org.rdfhdt.hdt.enums.CompressionType;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.impl.HDTBase;
import org.rdfhdt.hdt.hdt.impl.HDTImpl;
import org.rdfhdt.hdt.hdt.impl.TempHDTImporterOnePass;
import org.rdfhdt.hdt.hdt.impl.TempHDTImporterTwoPass;
import org.rdfhdt.hdt.hdt.impl.WriteHDTImpl;
import org.rdfhdt.hdt.hdt.impl.diskimport.CompressTripleMapper;
import org.rdfhdt.hdt.hdt.impl.diskimport.CompressionResult;
import org.rdfhdt.hdt.hdt.impl.diskimport.SectionCompressor;
import org.rdfhdt.hdt.hdt.impl.diskimport.TripleCompressionResult;
import org.rdfhdt.hdt.hdt.writer.TripleWriterHDT;
import org.rdfhdt.hdt.header.HeaderPrivate;
import org.rdfhdt.hdt.header.HeaderUtil;
import org.rdfhdt.hdt.iterator.utils.FileTripleIDIterator;
import org.rdfhdt.hdt.iterator.utils.FileTripleIterator;
import org.rdfhdt.hdt.listener.MultiThreadListener;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTOptionsKeys;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.rdf.RDFParserCallback;
import org.rdfhdt.hdt.rdf.RDFParserFactory;
import org.rdfhdt.hdt.rdf.TripleWriter;
import org.rdfhdt.hdt.triples.TempTriples;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.triples.TriplesPrivate;
import org.rdfhdt.hdt.util.BitUtil;
import org.rdfhdt.hdt.util.Profiler;
import org.rdfhdt.hdt.util.StopWatch;
import org.rdfhdt.hdt.util.StringUtil;
import org.rdfhdt.hdt.util.concurrent.TreeWorker;
import org.rdfhdt.hdt.util.io.CloseSuppressPath;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.io.compress.MapCompressTripleMerger;
import org.rdfhdt.hdt.util.io.compress.TripleGenerator;
import org.rdfhdt.hdt.util.listener.IntermediateListener;
import org.rdfhdt.hdt.util.listener.ListenerUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.UUID;
import org.rdfhdt.hdt.util.StopWatch;
import org.rdfhdt.hdt.util.io.IOUtil;

public class HDTManagerImpl extends HDTManager {

	private boolean useSimple(HDTOptions spec) {
		String value = spec.get("parser.ntSimpleParser");
		return value != null && !value.isEmpty() && !value.equals("false");
	}

	@Override
	public HDTOptions doReadOptions(String file) throws IOException {
		return new HDTSpecification(file);
	}

	@Override
	public HDT doLoadHDT(String hdtFileName, ProgressListener listener, HDTOptions spec) throws IOException {
		HDTPrivate hdt = new HDTImpl(spec);
		hdt.loadFromHDT(hdtFileName, listener);
		return hdt;
	}

	@Override
	protected HDT doMapHDT(String hdtFileName, ProgressListener listener, HDTOptions spec) throws IOException {
		HDTPrivate hdt = new HDTImpl(spec);
		hdt.mapFromHDT(new File(hdtFileName), 0, listener);
		return hdt;
	}


	@Override
	public HDT doLoadHDT(InputStream hdtFile, ProgressListener listener, HDTOptions spec) throws IOException {
		HDTPrivate hdt = new HDTImpl(spec);
		hdt.loadFromHDT(hdtFile, listener);
		return hdt;
	}

	@Override
	public HDT doLoadIndexedHDT(String hdtFileName, ProgressListener listener, HDTOptions spec) throws IOException {
		HDTPrivate hdt = new HDTImpl(spec);
		hdt.loadFromHDT(hdtFileName, listener);
		hdt.loadOrCreateIndex(listener);
		return hdt;
	}


	@Override
	public HDT doMapIndexedHDT(String hdtFileName, ProgressListener listener, HDTOptions spec) throws IOException {
		HDTPrivate hdt = new HDTImpl(spec);
		hdt.mapFromHDT(new File(hdtFileName), 0, listener);
		hdt.loadOrCreateIndex(listener);
		return hdt;
	}

	@Override
	public HDT doLoadIndexedHDT(InputStream hdtFile, ProgressListener listener, HDTOptions spec) throws IOException {
		HDTPrivate hdt = new HDTImpl(spec);
		hdt.loadFromHDT(hdtFile, listener);
		hdt.loadOrCreateIndex(listener);
		return hdt;
	}

	@Override
	public HDT doIndexedHDT(HDT hdt, ProgressListener listener) throws IOException {
		((HDTPrivate) hdt).loadOrCreateIndex(listener);
		return hdt;
	}

	@Override
	public HDT doGenerateHDT(String rdfFileName, String baseURI, RDFNotation rdfNotation, HDTOptions spec, ProgressListener listener) throws IOException, ParserException {
		//choose the importer
		String loaderType = spec.get(HDTOptionsKeys.LOADER_TYPE_KEY);
		TempHDTImporter loader;
		if (HDTOptionsKeys.LOADER_TYPE_VALUE_TWO_PASS.equals(loaderType)) {
			loader = new TempHDTImporterTwoPass(useSimple(spec));
		} else {
			loader = new TempHDTImporterOnePass(useSimple(spec));
		}

		// Create TempHDT
		try (TempHDT modHdt = loader.loadFromRDF(spec, rdfFileName, baseURI, rdfNotation, listener)) {

			// Convert to HDT
			HDTImpl hdt = new HDTImpl(spec);
			hdt.loadFromModifiableHDT(modHdt, listener);
			hdt.populateHeaderStructure(modHdt.getBaseURI());

			// Add file size to Header
			try {
				long originalSize = HeaderUtil.getPropertyLong(modHdt.getHeader(), "_:statistics", HDTVocabulary.ORIGINAL_SIZE);
				hdt.getHeader().insert("_:statistics", HDTVocabulary.ORIGINAL_SIZE, originalSize);
			} catch (NotFoundException e) {
				// ignore
			}

			return hdt;
		}
	}

	@Override
	public HDT doGenerateHDT(InputStream fileStream, String baseURI, RDFNotation rdfNotation, CompressionType compressionType, HDTOptions hdtFormat, ProgressListener listener) throws IOException {
		// uncompress the stream if required
		fileStream = IOUtil.asUncompressed(fileStream, compressionType);
		// create a parser for this rdf stream
		RDFParserCallback parser = RDFParserFactory.getParserCallback(rdfNotation);
		// read the stream as triples
		Iterator<TripleString> iterator = RDFParserFactory.readAsIterator(parser, fileStream, baseURI, true, rdfNotation);

		return doGenerateHDT(iterator, baseURI, hdtFormat, listener);
	}

	@Override
	public HDT doGenerateHDT(Iterator<TripleString> triples, String baseURI, HDTOptions spec, ProgressListener listener) throws IOException {
		//choose the importer
		TempHDTImporterOnePass loader = new TempHDTImporterOnePass(false);

		// Create TempHDT
		try (TempHDT modHdt = loader.loadFromTriples(spec, triples, baseURI, listener)) {
			// Convert to HDT
			HDTImpl hdt = new HDTImpl(spec);
			hdt.loadFromModifiableHDT(modHdt, listener);
			hdt.populateHeaderStructure(modHdt.getBaseURI());

			// Add file size to Header
			try {
				long originalSize = HeaderUtil.getPropertyLong(modHdt.getHeader(), "_:statistics", HDTVocabulary.ORIGINAL_SIZE);
				hdt.getHeader().insert("_:statistics", HDTVocabulary.ORIGINAL_SIZE, originalSize);
			} catch (NotFoundException e) {
				// ignore
			}

			return hdt;
		}
	}

	@Override
	public HDT doGenerateHDTDisk(String rdfFileName, String baseURI, RDFNotation rdfNotation, CompressionType compressionType, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException {
		// read this file as stream, do not compress to allow the compressionType to be different from the file extension
		try (InputStream stream = IOUtil.getFileInputStream(rdfFileName, false)) {
			return doGenerateHDTDisk(stream, baseURI, rdfNotation, compressionType, hdtFormat, listener);
		}
	}

	@Override
	public HDT doGenerateHDTDisk(InputStream fileStream, String baseURI, RDFNotation rdfNotation, CompressionType compressionType, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException {
		// uncompress the stream if required
		fileStream = IOUtil.asUncompressed(fileStream, compressionType);
		// create a parser for this rdf stream
		RDFParserCallback parser = RDFParserFactory.getParserCallback(rdfNotation, useSimple(hdtFormat));
		// read the stream as triples
		Iterator<TripleString> iterator = RDFParserFactory.readAsIterator(parser, fileStream, baseURI, true, rdfNotation);

		return doGenerateHDTDisk(iterator, baseURI, hdtFormat, listener);
	}

	/**
	 * @return a theoretical maximum amount of memory the JVM will attempt to use
	 */
	static long getMaxChunkSize(int workers) {
		Runtime runtime = Runtime.getRuntime();
		return  (long) ((runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())) * (0.85 * 1.5 * 3 * workers));
	}

	@Override
	public HDT doGenerateHDTDisk(Iterator<TripleString> iterator, String baseURI, HDTOptions hdtFormat, ProgressListener progressListener) throws IOException, ParserException {
		MultiThreadListener listener = ListenerUtil.multiThreadListener(progressListener);
		// load config
		// compression mode
		String compressMode = hdtFormat.get(HDTOptionsKeys.LOADER_DISK_COMPRESSION_MODE_KEY); // see CompressionResult
		// worker for compression tasks
		int workers = (int) hdtFormat.getInt(HDTOptionsKeys.LOADER_DISK_COMPRESSION_WORKER_KEY);
		// maximum size of a chunk
		long chunkSize = hdtFormat.getInt(HDTOptionsKeys.LOADER_DISK_CHUNK_SIZE_KEY);

		long maxFileOpenedLong = hdtFormat.getInt(HDTOptionsKeys.LOADER_DISK_MAX_FILE_OPEN_KEY);
		long kwayLong = hdtFormat.getInt(HDTOptionsKeys.LOADER_DISK_KWAY_KEY);
		long bufferSizeLong = hdtFormat.getInt(HDTOptionsKeys.LOADER_DISK_BUFFER_SIZE_KEY);
		int maxFileOpened;
		int ways;
		int bufferSize;
		// location of the working directory, will be deleted after generation
		String baseNameOpt = hdtFormat.get(HDTOptionsKeys.LOADER_DISK_LOCATION_KEY);
		CloseSuppressPath basePath;
		// location of the future HDT file, do not set to create the HDT in memory while mergin
		String futureHDTLocation = hdtFormat.get(HDTOptionsKeys.LOADER_DISK_FUTURE_HDT_LOCATION_KEY);

		Profiler profiler = new Profiler("doGenerateHDTDisk");
		String profilerString = hdtFormat.get("profiler");
		profiler.setDisabled(profilerString == null || !profilerString.equalsIgnoreCase("true"));
		// check and set default values if required
		if (workers == 0) {
			workers = Runtime.getRuntime().availableProcessors();
		} else if (workers < 0) {
			throw new IllegalArgumentException("Negative number of workers!");
		}
		if (baseNameOpt == null || baseNameOpt.isEmpty()) {
			basePath = CloseSuppressPath.of(Files.createTempDirectory("hdt-java-generate-disk"));
		} else {
			basePath = CloseSuppressPath.of(baseNameOpt);
		}
		basePath.closeWithDeleteRecurse();
		if (chunkSize == 0) {
			chunkSize = getMaxChunkSize(workers);
		} else if (chunkSize < 0) {
			throw new IllegalArgumentException("Negative chunk size!");
		}
		if (bufferSizeLong > Integer.MAX_VALUE - 5L || bufferSizeLong < 0) {
			throw new IllegalArgumentException("Buffer size can't be negative or bigger than the size of an array!");
		} else if (bufferSizeLong == 0) {
			bufferSize = CloseSuppressPath.BUFFER_SIZE;
		} else {
			bufferSize = (int) bufferSizeLong;
		}
		if (maxFileOpenedLong < 0 || maxFileOpenedLong > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("maxFileOpened can't be negative!");
		} else if (maxFileOpenedLong == 0) {
			maxFileOpened = 1024;
		} else {
			maxFileOpened = (int) maxFileOpenedLong;
		}
		if (kwayLong < 0 || kwayLong > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("kway can't be negative!");
		} else if (kwayLong == 0) {
			ways = 1; // Math.max(1, BitUtil.log2(Math.min(maxFileOpened, chunkSize / ((long) bufferSize * workers))));
		} else {
			ways = (int) kwayLong;
		}
		boolean mapHDT = futureHDTLocation != null && !futureHDTLocation.isEmpty();

		// create working directory
		basePath.mkdirs();
		try {
			// compress the triples into sections and compressed triples
			listener.notifyProgress(0, "Sorting sections with chunk of size: " + StringUtil.humanReadableByteCount(chunkSize, true) + "B with " + ways + "ways");

			FileTripleIterator triplesFile = new FileTripleIterator(iterator, chunkSize);

			profiler.pushSection("section compression");
			CompressionResult compressionResult;
			try {
				compressionResult = new SectionCompressor(basePath, triplesFile, listener, bufferSize)
						.compress(workers, ways, compressMode);
			} catch (TreeWorker.TreeWorkerException | InterruptedException e) {
				throw new ParserException(e);
			}
			profiler.popSection();

			HDTBase<? extends HeaderPrivate, ? extends DictionaryPrivate, ? extends TriplesPrivate> hdt;
			if (!mapHDT) {
				// using default implementation
				hdt = new HDTImpl(hdtFormat);
			} else {
				// using map implementation
				hdt = new WriteHDTImpl(hdtFormat, basePath.resolve("maphdt"), bufferSize);
			}
			hdt.setBaseUri(baseURI);

			listener.unregisterAllThreads();
			listener.notifyProgress(20, "Create sections and triple mapping");

			profiler.pushSection("dictionary write");
			// create sections and triple mapping
			DictionaryPrivate dictionary = hdt.getDictionary();
			CompressTripleMapper mapper = new CompressTripleMapper(basePath, compressionResult.getTripleCount(), chunkSize);
			CompressFourSectionDictionary modifiableDictionary = new CompressFourSectionDictionary(compressionResult, mapper, listener);
			try {
				dictionary.loadAsync(modifiableDictionary, listener);
			} catch (InterruptedException e) {
				throw new ParserException(e);
			}
			profiler.popSection();

			// complete the mapper with the shared count and delete compression data
			compressionResult.delete();
			mapper.setShared(dictionary.getNshared());

			listener.notifyProgress(40, "Create mapped and sort triple file");
			// create mapped triples file
			TripleCompressionResult tripleCompressionResult;
			TriplesPrivate triples = hdt.getTriples();
			TripleComponentOrder order = triples.getOrder();
			profiler.pushSection("triple compression/map");
			try {
				MapCompressTripleMerger tripleMapper = new MapCompressTripleMerger(
						basePath,
						new FileTripleIDIterator(new TripleGenerator(compressionResult.getTripleCount()), chunkSize),
						mapper,
						listener,
						order,
						bufferSize);
				tripleCompressionResult = tripleMapper.merge(workers, ways, compressMode);
			} catch (TreeWorker.TreeWorkerException | InterruptedException e) {
				throw new ParserException(e);
			}
			profiler.popSection();
			listener.unregisterAllThreads();

			profiler.pushSection("bit triple creation");
			try {
				// create bit triples and load the triples
				TempTriples tempTriples = tripleCompressionResult.getTriples();
				IntermediateListener il = new IntermediateListener(listener);
				il.setRange(80, 90);
				il.setPrefix("Create bit triples: ");
				il.notifyProgress(0, "create triples");
				triples.load(tempTriples, il);
				tempTriples.close();

				// completed the triples, delete the mapper
				mapper.delete();
			} finally {
				tripleCompressionResult.close();
			}
			profiler.popSection();
			profiler.pushSection("header creation");

			listener.notifyProgress(90, "Create HDT header");
			// header
			hdt.populateHeaderStructure(hdt.getBaseURI());
			hdt.getHeader().insert("_:statistics", HDTVocabulary.ORIGINAL_SIZE, triplesFile.getTotalSize());

			profiler.popSection();
			// return the HDT
			if (mapHDT) {
				profiler.pushSection("map to hdt");
				// write the HDT and map it
				try {
					hdt.saveToHDT(futureHDTLocation, listener);
				} finally {
					hdt.close();
				}
				IntermediateListener il = new IntermediateListener(listener);
				il.setPrefix("Map HDT: ");
				il.setRange(95, 100);
				il.notifyProgress(0, "start");
				try {
					return doMapHDT(futureHDTLocation, il, hdtFormat);
				} finally {
					profiler.popSection();
				}
			} else {
				listener.notifyProgress(100, "HDT completed");
				return hdt;
			}
		} finally {
			profiler.stop();
			profiler.writeProfiling();
			listener.notifyProgress(100, "Clearing disk");
			basePath.close();
		}
	}

	@Override
	protected TripleWriter doGetHDTWriter(OutputStream out, String baseURI, HDTOptions hdtFormat) {
		return new TripleWriterHDT(baseURI, hdtFormat, out);
	}

	@Override
	protected TripleWriter doGetHDTWriter(String outFile, String baseURI, HDTOptions hdtFormat) throws IOException {
		return new TripleWriterHDT(baseURI, hdtFormat, outFile, false);
	}

	@Override
	public HDT doHDTCat(String location, String hdtFileName1, String hdtFileName2, HDTOptions hdtFormat, ProgressListener listener) throws IOException {
		try (HDT hdt1 = doMapHDT(hdtFileName1, listener, hdtFormat);
			 HDT hdt2 = doMapHDT(hdtFileName2, listener, hdtFormat)) {
			HDTImpl hdt = new HDTImpl(hdtFormat);
			if (hdt1.getDictionary() instanceof MultipleSectionDictionary
					&& hdt2.getDictionary() instanceof MultipleSectionDictionary) {
				hdt.catCustom(location, hdt1, hdt2, listener);
			}
			else {
				hdt.cat(location, hdt1, hdt2, listener);
			}
			return hdt;
		}
	}

	@Override
	public HDT doHDTDiff(String hdtFileName1, String hdtFileName2, HDTOptions hdtFormat, ProgressListener listener) throws IOException {
		try (HDT hdt1 = doMapHDT(hdtFileName1, listener, hdtFormat);
			 HDT hdt2 = doMapHDT(hdtFileName2, listener, hdtFormat)) {
			HDTImpl hdt = new HDTImpl(hdtFormat);
			hdt.diff(hdt1, hdt2, listener);
			return hdt;
		}
	}

	@Override
	protected HDT doHDTDiffBit(String location, String hdtFileName, Bitmap deleteBitmap, HDTOptions hdtFormat, ProgressListener listener) throws IOException {
		try (HDT hdtOriginal = doMapHDT(hdtFileName, listener, hdtFormat)) {
			HDTImpl hdt = new HDTImpl(hdtFormat);
			hdt.diffBit(location, hdtOriginal, deleteBitmap, listener);
			return hdt;
		}
	}
}
