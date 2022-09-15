package org.rdfhdt.hdt.hdt;

import org.rdfhdt.hdt.compact.bitmap.Bitmap;
import org.rdfhdt.hdt.dictionary.impl.MultipleSectionDictionary;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.impl.HDTImpl;
import org.rdfhdt.hdt.hdt.impl.TempHDTImporterOnePass;
import org.rdfhdt.hdt.hdt.impl.TempHDTImporterTwoPass;
import org.rdfhdt.hdt.hdt.writer.TripleWriterHDT;
import org.rdfhdt.hdt.header.HeaderUtil;
import org.rdfhdt.hdt.iterator.utils.FluxStopTripleStringIterator;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.rdf.RDFFluxStop;
import org.rdfhdt.hdt.rdf.RDFParserCallback;
import org.rdfhdt.hdt.rdf.RDFParserFactory;
import org.rdfhdt.hdt.rdf.TripleWriter;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.listener.PrefixListener;

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
		((HDTPrivate)hdt).loadOrCreateIndex(listener);
		return hdt;
	}

	@Override
	public HDT doGenerateHDT(String rdfFileName, String baseURI, RDFNotation rdfNotation, HDTOptions spec, ProgressListener listener) throws IOException, ParserException {
		//choose the importer
		String loaderType = spec.get("loader.type");
		TempHDTImporter loader;
		if ("two-pass".equals(loaderType)) {
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
	protected TripleWriter doGetHDTWriter(OutputStream out, String baseURI, HDTOptions hdtFormat) throws IOException {
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

	@Override
	protected HDT doHDTCatTree(RDFFluxStop fluxStop, HDTSupplier supplier, String filename, String baseURI, RDFNotation rdfNotation, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException {
		try (InputStream is = IOUtil.getFileInputStream(filename)) {
			return doHDTCatTree(fluxStop, supplier, is, baseURI, rdfNotation, hdtFormat, listener);
		}
	}

	@Override
	protected HDT doHDTCatTree(RDFFluxStop fluxStop, HDTSupplier supplier, InputStream stream, String baseURI, RDFNotation rdfNotation, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException {
		RDFParserCallback parser = RDFParserFactory.getParserCallback(rdfNotation, useSimple(hdtFormat));
		Iterator<TripleString> iterator = RDFParserFactory.readAsIterator(parser, stream, baseURI, true, rdfNotation);
		return doHDTCatTree(fluxStop, supplier, iterator, baseURI, hdtFormat, listener);
	}

	@Override
	protected HDT doHDTCatTree(RDFFluxStop fluxStop, HDTSupplier supplier, Iterator<TripleString> iterator, String baseURI, HDTOptions hdtFormat, ProgressListener listener) throws IOException, ParserException {
		Path basePath;
		String baseNameOpt = hdtFormat.get("loader.cattree.location");

		if (baseNameOpt == null || baseNameOpt.isEmpty()) {
			basePath = Files.createTempDirectory("hdt-java-cat-tree");
		} else {
			basePath = Path.of(baseNameOpt);
		}

		Path futureHDTLocation = Optional.ofNullable(hdtFormat.get("loader.cattree.futureHDTLocation")).map(Path::of).orElse(null);

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
			ProgressListener il = PrefixListener.of("gen#" + gen, listener);
			Path hdtLocation = hdtStore.resolve("hdt-" + gen + ".hdt");
			supplier.doGenerateHDT(it, baseURI, hdtFormat, il, hdtLocation);

			nextFile = it.hasNextFlux();
			HDTFile hdtFile = new HDTFile(hdtLocation, 1);

			// merge the generated hdt with each block with enough size
			while (!files.isEmpty() && (!nextFile || (files.get(files.size() - 1)).getChunks() <= hdtFile.getChunks())) {
				HDTFile lastHDTFile = files.remove(files.size() - 1);
				cat++;
				ProgressListener ilc = PrefixListener.of("cat#" + cat, listener);
				Path hdtCatFileLocation = hdtStore.resolve("hdtcat-" + cat + ".hdt");
				try (HDT abcat = HDTManager.catHDT(
						hdtCatLocation,
						lastHDTFile.getHdtFile().toAbsolutePath().toString(),
						hdtFile.getHdtFile().toAbsolutePath().toString(),
						hdtFormat, ilc)) {
					abcat.saveToHDT(hdtCatFileLocation.toAbsolutePath().toString(), il);
				}
				// delete previous chunks
				Files.delete(lastHDTFile.getHdtFile());
				Files.delete(hdtFile.getHdtFile());
				// note the new hdt file and the number of chunks
				hdtFile = new HDTFile(hdtCatFileLocation, lastHDTFile.getChunks() + hdtFile.getChunks());
			}
			files.add(hdtFile);
		} while (nextFile);

		assert files.size() == 1;

		Path hdtFile = files.get(0).hdtFile;

		assert files.get(0).getChunks() == gen;
		assert cat < gen;

		// if a future HDT location has been asked, move to it and map the HDT
		if (futureHDTLocation != null) {
			Files.deleteIfExists(futureHDTLocation);
			Files.move(hdtFile, futureHDTLocation);
			return HDTManager.mapHDT(futureHDTLocation.toAbsolutePath().toString());
		}

		// if no future location has been asked, load the HDT and delete it after
		try {
			return HDTManager.loadHDT(hdtFile.toAbsolutePath().toString());
		} finally {
			Files.delete(hdtFile);
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
