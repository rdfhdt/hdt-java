package org.rdfhdt.hdt.hdt;

import org.rdfhdt.hdt.compact.bitmap.Bitmap;
import org.rdfhdt.hdt.dictionary.impl.MultipleSectionDictionary;
import org.rdfhdt.hdt.enums.CompressionType;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.impl.HDTDiskImporter;
import org.rdfhdt.hdt.hdt.impl.HDTImpl;
import org.rdfhdt.hdt.hdt.impl.TempHDTImporterOnePass;
import org.rdfhdt.hdt.hdt.impl.TempHDTImporterTwoPass;
import org.rdfhdt.hdt.hdt.writer.TripleWriterHDT;
import org.rdfhdt.hdt.header.HeaderUtil;
import org.rdfhdt.hdt.iterator.utils.FluxStopTripleStringIterator;
import org.rdfhdt.hdt.iterator.utils.MapIterator;
import org.rdfhdt.hdt.iterator.utils.PipedCopyIterator;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTOptionsKeys;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.options.HideHDTOptions;
import org.rdfhdt.hdt.rdf.RDFFluxStop;
import org.rdfhdt.hdt.rdf.RDFParserCallback;
import org.rdfhdt.hdt.rdf.RDFParserFactory;
import org.rdfhdt.hdt.rdf.TripleWriter;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.util.BitUtil;
import org.rdfhdt.hdt.util.Profiler;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.listener.PrefixListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class HDTManagerImpl extends HDTManager {
	private static final Logger logger = LoggerFactory.getLogger(HDTManagerImpl.class);

	private boolean useSimple(HDTOptions spec) {
		String value = spec.get(HDTOptionsKeys.NT_SIMPLE_PARSER_KEY);
		return value != null && !value.isEmpty() && !value.equals("false");
	}

	@Override
	public HDTOptions doReadOptions(String file) throws IOException {
		return new HDTSpecification(file);
	}

	private HDT loadOrMapHDT(String hdtFileName, ProgressListener listener, HDTOptions spec) throws IOException {
		String loadingMethod = spec.get(HDTOptionsKeys.LOAD_HDT_TYPE_KEY);
		if (loadingMethod == null || loadingMethod.isEmpty() || HDTOptionsKeys.LOAD_HDT_TYPE_VALUE_MAP.equals(loadingMethod)) {
			return doMapHDT(hdtFileName, listener, spec);
		}
		if (HDTOptionsKeys.LOAD_HDT_TYPE_VALUE_LOAD.equals(loadingMethod)) {
			return doLoadHDT(hdtFileName, listener, spec);
		}
		throw new IllegalArgumentException("Bad loading method: " + loadingMethod);
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

	private RDFFluxStop readFluxStopOrSizeLimit(HDTOptions spec) {
		// if no config, use default implementation
		return spec.getFluxStop(HDTOptionsKeys.RDF_FLUX_STOP_KEY,
			() -> {
				// get the chunk size to base the work
				String loaderType = spec.get(HDTOptionsKeys.LOADER_CATTREE_LOADERTYPE_KEY);

				if (!HDTOptionsKeys.LOADER_TYPE_VALUE_DISK.equals(loaderType)) {
					// memory based implementation, we can only store the NT file
					return RDFFluxStop.sizeLimit(getMaxChunkSize());
				}

				// disk based implementation, we only have to reduce the fault-factor of the map files
				long triplesCount = findBestMemoryChunkDiskMapTreeCat();

				double factor = spec.getDouble(HDTOptionsKeys.LOADER_CATTREE_MEMORY_FAULT_FACTOR, 1.4);

				if (factor <= 0) {
					throw new IllegalArgumentException(HDTOptionsKeys.LOADER_CATTREE_MEMORY_FAULT_FACTOR + " can't have a negative or 0 value!");
				}

				// create a count limit from the chunk size / factor, set a minimum value for low factor
				return RDFFluxStop.countLimit(Math.max(128, (long) (triplesCount * factor)));
			}
		);
	}

	@Override
	public HDT doGenerateHDT(String rdfFileName, String baseURI, RDFNotation rdfNotation, HDTOptions spec, ProgressListener listener) throws IOException, ParserException {
		//choose the importer
		String loaderType = spec.get(HDTOptionsKeys.LOADER_TYPE_KEY);
		TempHDTImporter loader;
		if (HDTOptionsKeys.LOADER_TYPE_VALUE_DISK.equals(loaderType)) {
			return doGenerateHDTDisk(rdfFileName, baseURI, rdfNotation, CompressionType.guess(rdfFileName), spec, listener);
		} else if (HDTOptionsKeys.LOADER_TYPE_VALUE_CAT.equals(loaderType)) {
			return doHDTCatTree(readFluxStopOrSizeLimit(spec), HDTSupplier.fromSpec(spec), rdfFileName, baseURI, rdfNotation, spec, listener);
		} else if (HDTOptionsKeys.LOADER_TYPE_VALUE_TWO_PASS.equals(loaderType)) {
			loader = new TempHDTImporterTwoPass(useSimple(spec));
		} else {
			if (loaderType != null && !HDTOptionsKeys.LOADER_TYPE_VALUE_ONE_PASS.equals(loaderType)) {
				logger.warn("Used the option {} with value {}, which isn't recognize, using default value {}",
						HDTOptionsKeys.LOADER_TYPE_KEY, loaderType, HDTOptionsKeys.LOADER_TYPE_VALUE_ONE_PASS);
			}
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
		try (PipedCopyIterator<TripleString> iterator = RDFParserFactory.readAsIterator(parser, fileStream, baseURI, true, rdfNotation)) {
			return doGenerateHDT(iterator, baseURI, hdtFormat, listener);
		}
	}

	@Override
	public HDT doGenerateHDT(Iterator<TripleString> triples, String baseURI, HDTOptions spec, ProgressListener listener) throws IOException {
		//choose the importer
		String loaderType = spec.get(HDTOptionsKeys.LOADER_TYPE_KEY);
		TempHDTImporterOnePass loader;
		if (HDTOptionsKeys.LOADER_TYPE_VALUE_DISK.equals(loaderType)) {
			try {
				return doGenerateHDTDisk(triples, baseURI, spec, listener);
			} catch (ParserException e) {
				throw new RuntimeException(e);
			}
		} else if (HDTOptionsKeys.LOADER_TYPE_VALUE_CAT.equals(loaderType)) {
			try {
				return doHDTCatTree(readFluxStopOrSizeLimit(spec), HDTSupplier.fromSpec(spec), triples, baseURI, spec, listener);
			} catch (ParserException e) {
				throw new RuntimeException(e);
			}
		} else {
			if (loaderType != null) {
				if (HDTOptionsKeys.LOADER_TYPE_VALUE_TWO_PASS.equals(loaderType)) {
					logger.warn("Used the option {} with value {}, which isn't available for stream generation, using default value {}",
							HDTOptionsKeys.LOADER_TYPE_KEY, loaderType, HDTOptionsKeys.LOADER_TYPE_VALUE_ONE_PASS);
				} else if (!HDTOptionsKeys.LOADER_TYPE_VALUE_ONE_PASS.equals(loaderType)) {
					logger.warn("Used the option {} with value {}, which isn't recognize, using default value {}",
							HDTOptionsKeys.LOADER_TYPE_KEY, loaderType, HDTOptionsKeys.LOADER_TYPE_VALUE_ONE_PASS);
				}
			}
			loader = new TempHDTImporterOnePass(useSimple(spec));
		}

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
		try (PipedCopyIterator<TripleString> iterator = RDFParserFactory.readAsIterator(parser, fileStream, baseURI, true, rdfNotation)) {
			return doGenerateHDTDisk0(iterator, true, baseURI, hdtFormat, listener);
		}
	}

	/**
	 * @return a theoretical maximum amount of memory the JVM will attempt to use
	 */
	static long getMaxChunkSize() {
		Runtime runtime = Runtime.getRuntime();
		return  (long) ((runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())) * 0.85);
	}

	private static long findBestMemoryChunkDiskMapTreeCat() {
		Runtime runtime = Runtime.getRuntime();
		long maxRam = (long) ((runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())) * 0.85) / 3;

		int shift = 0;

		while (shift != 63 && (1L << shift) * BitUtil.log2(1L << shift) < maxRam) {
			shift++;
		}

		// it will take at most "shift" bits per triple
		// we divide by 3 for the 3 maps
		return maxRam / shift;
	}

	@Override
	public HDT doGenerateHDTDisk(Iterator<TripleString> iterator, String baseURI, HDTOptions hdtFormat, ProgressListener progressListener) throws IOException, ParserException {
		return doGenerateHDTDisk0(iterator, hdtFormat.getBoolean(HDTOptionsKeys.LOADER_DISK_NO_COPY_ITERATOR_KEY), baseURI, hdtFormat, progressListener);
	}

	private HDT doGenerateHDTDisk0(Iterator<TripleString> iterator, boolean copyIterator, String baseURI, HDTOptions hdtFormat, ProgressListener progressListener) throws IOException, ParserException {
		try (HDTDiskImporter hdtDiskImporter = new HDTDiskImporter(hdtFormat, progressListener, baseURI)) {
			if (copyIterator) {
				return hdtDiskImporter.runAllSteps(iterator);
			} else {
				// create a copy of the triple at loading time to avoid weird behaviors
				return hdtDiskImporter.runAllSteps(new MapIterator<>(iterator, TripleString::tripleToString));
			}
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
		try (HDT hdt1 = loadOrMapHDT(hdtFileName1, listener, hdtFormat);
			 HDT hdt2 = loadOrMapHDT(hdtFileName2, listener, hdtFormat)) {
			HDTImpl hdt = new HDTImpl(hdtFormat);
			try (Profiler profiler = Profiler.createOrLoadSubSection("hdtCat", hdtFormat, false)) {
				try {
					if (hdt1.getDictionary() instanceof MultipleSectionDictionary
							&& hdt2.getDictionary() instanceof MultipleSectionDictionary) {
						hdt.catCustom(location, hdt1, hdt2, listener, profiler);
					} else {
						hdt.cat(location, hdt1, hdt2, listener, profiler);
					}
				} finally {
					profiler.stop();
					profiler.writeProfiling();
				}
			}
			return hdt;
		}
	}

	@Override
	public HDT doHDTDiff(String hdtFileName1, String hdtFileName2, HDTOptions hdtFormat, ProgressListener listener) throws IOException {
		try (HDT hdt1 = loadOrMapHDT(hdtFileName1, listener, hdtFormat);
			 HDT hdt2 = loadOrMapHDT(hdtFileName2, listener, hdtFormat)) {
			HDTImpl hdt = new HDTImpl(hdtFormat);
			try (Profiler profiler = Profiler.createOrLoadSubSection("hdtDiff", hdtFormat, true)) {
				hdt.diff(hdt1, hdt2, listener, profiler);
			}
			return hdt;
		}
	}

	@Override
	protected HDT doHDTDiffBit(String location, String hdtFileName, Bitmap deleteBitmap, HDTOptions hdtFormat, ProgressListener listener) throws IOException {
		try (HDT hdtOriginal = loadOrMapHDT(hdtFileName, listener, hdtFormat)) {
			HDTImpl hdt = new HDTImpl(hdtFormat);
			try (Profiler profiler = Profiler.createOrLoadSubSection("hdtDiffBit", hdtFormat, true)) {
				hdt.diffBit(location, hdtOriginal, deleteBitmap, listener, profiler);
			} catch (Throwable t) {
				try {
					throw t;
				} finally {
					hdt.close();
				}
			}
			return hdt;
		}
	}

	@Override
	protected HDT doHDTCatTree(RDFFluxStop fluxStop, HDTSupplier supplier, String filename, String baseURI, RDFNotation rdfNotation, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException {
		try (InputStream is = IOUtil.getFileInputStream(filename)) {
			return doHDTCatTree(fluxStop, supplier, is, baseURI, rdfNotation, hdtFormat, listener);
		}
	}

	@Override
	protected HDT doHDTCatTree(RDFFluxStop fluxStop, HDTSupplier supplier, InputStream stream, String baseURI, RDFNotation rdfNotation, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException {
		RDFParserCallback parser = RDFParserFactory.getParserCallback(rdfNotation, useSimple(hdtFormat));
		try (PipedCopyIterator<TripleString> iterator = RDFParserFactory.readAsIterator(parser, stream, baseURI, true, rdfNotation)) {
			return doHDTCatTree(fluxStop, supplier, iterator, baseURI, hdtFormat, listener);
		}
	}

	@Override
	protected HDT doHDTCatTree(RDFFluxStop fluxStop, HDTSupplier supplier, Iterator<TripleString> iterator, String baseURI, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException {
		Path basePath;
		String baseNameOpt = hdtFormat.get(HDTOptionsKeys.LOADER_CATTREE_LOCATION_KEY);

		if (baseNameOpt == null || baseNameOpt.isEmpty()) {
			basePath = Files.createTempDirectory("hdt-java-cat-tree");
		} else {
			basePath = Path.of(baseNameOpt);
		}

		// hide the loader type to avoid infinite recursion
		hdtFormat = new HideHDTOptions(hdtFormat, key -> HDTOptionsKeys.LOADER_TYPE_KEY.equals(key) ? HDTOptionsKeys.LOADER_CATTREE_LOADERTYPE_KEY : key);

		Path futureHDTLocation = Optional.ofNullable(hdtFormat.get(HDTOptionsKeys.LOADER_CATTREE_FUTURE_HDT_LOCATION_KEY)).map(Path::of).orElse(null);

		try (Profiler profiler = Profiler.createOrLoadSubSection("doHDTCatTree", hdtFormat, true)) {
			FluxStopTripleStringIterator it = new FluxStopTripleStringIterator(iterator, fluxStop);

			List<HDTFile> files = new ArrayList<>();

			long gen = 0;
			long cat = 0;

			Path hdtStore = basePath.resolve("hdt-store");
			Path hdtCatLocationPath = basePath.resolve("cat");
			String hdtCatLocation = hdtCatLocationPath.toAbsolutePath().toString();

			Files.createDirectories(hdtStore);
			Files.createDirectories(hdtCatLocationPath);

			boolean nextFile;
			do {
				// generate the hdt
				gen++;
				profiler.pushSection("generateHDT #" + gen);
				PrefixListener il = PrefixListener.of("gen#" + gen, listener);
				Path hdtLocation = hdtStore.resolve("hdt-" + gen + ".hdt");
				supplier.doGenerateHDT(it, baseURI, hdtFormat, il, hdtLocation);
				il.clearThreads();

				nextFile = it.hasNextFlux();
				HDTFile hdtFile = new HDTFile(hdtLocation, 1);
				profiler.popSection();

				// merge the generated hdt with each block with enough size
				while (!files.isEmpty() && (!nextFile || (files.get(files.size() - 1)).getChunks() <= hdtFile.getChunks())) {
					HDTFile lastHDTFile = files.remove(files.size() - 1);
					cat++;
					profiler.pushSection("catHDT #" + cat);
					PrefixListener ilc = PrefixListener.of("cat#" + cat, listener);
					Path hdtCatFileLocation = hdtStore.resolve("hdtcat-" + cat + ".hdt");
					try (HDT abcat = HDTManager.catHDT(
							hdtCatLocation,
							lastHDTFile.getHdtFile().toAbsolutePath().toString(),
							hdtFile.getHdtFile().toAbsolutePath().toString(),
							hdtFormat, ilc)) {
						abcat.saveToHDT(hdtCatFileLocation.toAbsolutePath().toString(), ilc);
					}
					ilc.clearThreads();
					// delete previous chunks
					Files.delete(lastHDTFile.getHdtFile());
					Files.delete(hdtFile.getHdtFile());
					// note the new hdt file and the number of chunks
					hdtFile = new HDTFile(hdtCatFileLocation, lastHDTFile.getChunks() + hdtFile.getChunks());

					profiler.popSection();
				}
				files.add(hdtFile);
			} while (nextFile);

			listener.notifyProgress(100, "done, loading HDT");

			Path hdtFile = files.get(0).hdtFile;

			assert files.get(0).getChunks() == gen;
			assert cat < gen;

			try {
				// if a future HDT location has been asked, move to it and map the HDT
				if (futureHDTLocation != null) {
					Files.createDirectories(futureHDTLocation.toAbsolutePath().getParent());
					Files.deleteIfExists(futureHDTLocation);
					Files.move(hdtFile, futureHDTLocation);
					return HDTManager.mapHDT(futureHDTLocation.toAbsolutePath().toString());
				}

				// if no future location has been asked, load the HDT and delete it after
				return HDTManager.loadHDT(hdtFile.toAbsolutePath().toString());
			} finally {
				Files.deleteIfExists(hdtFile);
				profiler.stop();
				profiler.writeProfiling();
			}
		}
	}

	private static class HDTFile {
		private final Path hdtFile;
		private final long chunks;

		public HDTFile(Path hdtFile, long chunks) {
			this.hdtFile = hdtFile;
			this.chunks = chunks;
		}

		public long getChunks() {
			return chunks;
		}

		public Path getHdtFile() {
			return hdtFile;
		}
	}
}
