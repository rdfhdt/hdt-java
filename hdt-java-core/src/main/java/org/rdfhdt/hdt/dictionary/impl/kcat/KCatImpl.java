package org.rdfhdt.hdt.dictionary.impl.kcat;

import org.rdfhdt.hdt.dictionary.DictionaryPrivate;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.hdt.HDTManagerImpl;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.hdt.impl.HDTBase;
import org.rdfhdt.hdt.hdt.impl.WriteHDTImpl;
import org.rdfhdt.hdt.header.HeaderFactory;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTOptionsKeys;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.Triples;
import org.rdfhdt.hdt.triples.impl.BitmapTriples;
import org.rdfhdt.hdt.triples.impl.OneReadTempTriples;
import org.rdfhdt.hdt.triples.impl.WriteBitmapTriples;
import org.rdfhdt.hdt.util.io.CloseSuppressPath;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.listener.IntermediateListener;
import org.rdfhdt.hdt.util.listener.ListenerUtil;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Implementation of HDTCat_K algorithm
 *
 * @author Antoine Willerval
 */
public class KCatImpl implements Closeable {
	private static TripleComponentOrder getOrder(HDT hdt) {
		Triples triples = hdt.getTriples();
		if (!(triples instanceof BitmapTriples)) {
			throw new IllegalArgumentException("HDT Triples can't be BitmapTriples");
		}

		BitmapTriples bt = (BitmapTriples) triples;

		return bt.getOrder();
	}

	private final String baseURI;
	final HDT[] hdts;
	private final CloseSuppressPath location;
	private final Path futureLocation;
	private final boolean futureMap;
	private final boolean clearLocation;
	private final ProgressListener listener;
	private final String dictionaryType;
	private final int bufferSize;
	private final HDTOptions hdtFormat;
	private final TripleComponentOrder order;
	private final long rawSize;

	/**
	 * Create implementation
	 *
	 * @param hdtFileNames the hdt files to cat
	 * @param hdtFormat    the format to config the cat
	 * @param listener     listener to get information from the cat
	 * @throws IOException io exception during loading
	 */
	public KCatImpl(List<String> hdtFileNames, HDTOptions hdtFormat, ProgressListener listener) throws IOException {
		this.listener = ListenerUtil.multiThreadListener(listener);

		hdts = new HDT[hdtFileNames.size()];
		this.hdtFormat = hdtFormat;

		long bufferSizeLong = hdtFormat.getInt(HDTOptionsKeys.LOADER_DISK_BUFFER_SIZE_KEY, CloseSuppressPath.BUFFER_SIZE);
		if (bufferSizeLong > Integer.MAX_VALUE - 5L || bufferSizeLong <= 0) {
			throw new IllegalArgumentException("Buffer size can't be negative or bigger than the size of an array!");
		} else {
			bufferSize = (int) bufferSizeLong;
		}

		try {
			ListIterator<String> it = hdtFileNames.listIterator();

			int firstIndex = it.nextIndex();
			String firstHDTFile = it.next();

			HDT firstHDT = HDTManagerImpl.loadOrMapHDT(firstHDTFile, listener, hdtFormat);
			hdts[firstIndex] = firstHDT;

			dictionaryType = firstHDT.getDictionary().getType();
			baseURI = firstHDT.getBaseURI();
			order = getOrder(firstHDT);

			long rawSize = HDTBase.getRawSize(firstHDT.getHeader());


			IntermediateListener iListener = new IntermediateListener(listener);
			iListener.setRange(0, 10);
			// map all the HDTs
			while (it.hasNext()) {
				int index = it.nextIndex();
				String hdtFile = it.next();

				iListener.notifyProgress(index * 100f / hdtFileNames.size(), "map hdt (" + (index + 1) + "/" + hdtFileNames.size() + ")");

				HDT hdt = HDTManagerImpl.loadOrMapHDT(hdtFile, listener, hdtFormat);

				rawSize = rawSize == -1 ? -1 : HDTBase.getRawSize(hdt.getHeader());

				hdts[index] = hdt;

				// try that all the HDTs have the same type
				if (!dictionaryType.equals(hdt.getDictionary().getType())) {
					throw new IllegalArgumentException("Trying to cat hdt with different type, type(hdt0) [" + dictionaryType + "] != type(hdt" + index + ") [" + hdt.getDictionary().getType() + "]");
				}
				TripleComponentOrder order = getOrder(hdt);

				if (!order.equals(this.order)) {
					throw new IllegalArgumentException("Trying to cat hdt with different order, order(hdt0) [" + this.order + "] != type(hdt" + index + ") [" + order + "]");
				}
			}

			this.rawSize = rawSize;

			String hdtcatLocationOpt = hdtFormat.get(HDTOptionsKeys.HDTCAT_LOCATION);
			if (hdtcatLocationOpt == null || hdtcatLocationOpt.isEmpty()) {
				location = CloseSuppressPath.of(Files.createTempDirectory("hdtCat"));
				clearLocation = true; // delete temp directory
			} else {
				location = CloseSuppressPath.of(hdtcatLocationOpt);
				Files.createDirectories(location);
				clearLocation = hdtFormat.getBoolean(HDTOptionsKeys.HDTCAT_DELETE_LOCATION, true);
			}

			String hdtcatFutureLocationOpt = hdtFormat.get(HDTOptionsKeys.HDTCAT_FUTURE_LOCATION);
			if (hdtcatFutureLocationOpt == null || hdtcatFutureLocationOpt.isEmpty()) {
				futureLocation = location.resolve("gen.hdt");
				futureMap = false;
			} else {
				futureLocation = Path.of(hdtcatFutureLocationOpt);
				futureMap = true;
			}

			location.closeWithDeleteRecurse();
		} catch (Throwable t) {
			for (HDT hdt : hdts) {
				IOUtil.closeQuietly(hdt);
			}
			throw t;
		}
	}

	/**
	 * @return a merger from the config
	 * @throws IOException io exception
	 */
	KCatMerger createMerger(ProgressListener listener) throws IOException {
		return new KCatMerger(hdts, location, listener, bufferSize, dictionaryType, hdtFormat);
	}

	/**
	 * @return a cat from the config HDTs
	 * @throws IOException io exception
	 */
	public HDT cat() throws IOException {
		IntermediateListener il = new IntermediateListener(listener);
		String futureLocationStr = futureLocation.toAbsolutePath().toString();
		il.setRange(0, 40);
		il.setPrefix("Merge Dict: ");
		try (KCatMerger merger = createMerger(il)) {
			// create the dictionary
			try (DictionaryPrivate dictionary = merger.buildDictionary()) {
				assert merger.assertReadCorrectly();
				// create a GROUP BY subject iterator to get the new ordered stream
				Iterator<TripleID> tripleIterator = GroupBySubjectMapIterator.fromHDTs(merger, hdts);
				try (WriteBitmapTriples triples = new WriteBitmapTriples(hdtFormat, location.resolve("triples"), bufferSize)) {
					long count = Arrays.stream(hdts).mapToLong(h -> h.getTriples().getNumberOfElements()).sum();

					il.setRange(40, 80);
					il.setPrefix("Merge triples: ");
					triples.load(new OneReadTempTriples(tripleIterator, order, count), il);

					WriteHDTImpl writeHDT = new WriteHDTImpl(hdtFormat, location, dictionary, triples, HeaderFactory.createHeader(hdtFormat));
					writeHDT.populateHeaderStructure(baseURI);
					// add a raw size from the previous values (if available)
					if (rawSize != -1) {
						writeHDT.getHeader().insert("_:statistics", HDTVocabulary.ORIGINAL_SIZE, String.valueOf(rawSize));
					}

					il.setRange(80, 90);
					il.setPrefix("Save HDT: ");
					writeHDT.saveToHDT(futureLocationStr, il);
				}
			}

		} catch (InterruptedException e) {
			throw new IOException("Interruption", e);
		}

		il.setRange(90, 100);
		HDT hdt;
		if (futureMap) {
			hdt = HDTManager.mapHDT(futureLocationStr, il);
		} else {
			hdt = HDTManager.loadHDT(futureLocationStr, il);
			Files.deleteIfExists(futureLocation);
		}
		il.notifyProgress(100, "cat done.");
		return hdt;
	}

	@Override
	public void close() throws IOException {
		try {
			IOUtil.closeAll(hdts);
		} finally {
			if (clearLocation) {
				location.close();
			}
		}
	}
}
