package org.rdfhdt.hdt.util.io.compress;

import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.hdt.impl.diskimport.CompressTripleMapper;
import org.rdfhdt.hdt.hdt.impl.diskimport.CompressionResult;
import org.rdfhdt.hdt.hdt.impl.diskimport.TripleCompressionResult;
import org.rdfhdt.hdt.hdt.impl.diskimport.TripleCompressionResultFile;
import org.rdfhdt.hdt.hdt.impl.diskimport.TripleCompressionResultPartial;
import org.rdfhdt.hdt.iterator.utils.ExceptionIterator;
import org.rdfhdt.hdt.iterator.utils.FileTripleIDIterator;
import org.rdfhdt.hdt.listener.MultiThreadListener;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleIDComparator;
import org.rdfhdt.hdt.util.ParallelSortableArrayList;
import org.rdfhdt.hdt.util.concurrent.TreeWorker;
import org.rdfhdt.hdt.util.io.CloseSuppressPath;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.listener.IntermediateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TreeWorkerObject implementation to map and merge tripleID from a compress triple file
 *
 * @author Antoine Willerval
 */
public class MapCompressTripleMerger implements TreeWorker.TreeWorkerObject<MapCompressTripleMerger.BufferedTriples, MapCompressTripleMerger.TripleFile> {
	private static final Logger log = LoggerFactory.getLogger(MapCompressTripleMerger.class);
	private final AtomicInteger FID = new AtomicInteger();
	private final CloseSuppressPath baseFileName;
	private final FileTripleIDIterator source;
	private final CompressTripleMapper mapper;
	private final MultiThreadListener listener;
	private final TripleComponentOrder order;
	private final int bufferSize;
	private boolean done;
	private long triplesCount = 0;

	public MapCompressTripleMerger(CloseSuppressPath baseFileName, FileTripleIDIterator it, CompressTripleMapper mapper, MultiThreadListener listener, TripleComponentOrder order, int bufferSize) {
		this.baseFileName = baseFileName;
		this.source = it;
		this.mapper = mapper;
		this.listener = listener;
		this.order = order;
		this.bufferSize = bufferSize;
	}

	@Override
	public TripleFile construct(TripleFile[] tripleFiles, int count) {
		try {
			int fid = FID.incrementAndGet();
			CloseSuppressPath triplesFiles = baseFileName.resolve("triples" + fid + ".raw");
			long triples = 0;
			listener.notifyProgress(0, "merging triples " + triplesFiles.getFileName());
			CompressTripleReader[] readers = new CompressTripleReader[count];
			try {
				for (int i = 0; i < count; i++) {
					readers[i] = new CompressTripleReader(tripleFiles[i].path.openInputStream(bufferSize));
				}

				try (CompressTripleWriter w = new CompressTripleWriter(triplesFiles.openOutputStream(bufferSize))) {
					ExceptionIterator<TripleID, IOException> it = CompressTripleMergeIterator.buildOfTree(readers, order);
					while (it.hasNext()) {
						w.appendTriple(it.next());
						triples++;
					}
				}
			} finally {
				IOUtil.closeAll(readers);
			}
			listener.notifyProgress(100, "triples merged " + triplesFiles.getFileName());
			// delete old triples
			for (int i = 0; i < count; i++) {
				delete(tripleFiles[i]);
			}
			return new TripleFile(triples, triplesFiles);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void delete(TripleFile f) {
		try {
			f.close();
		} catch (IOException e) {
			log.warn("Can't delete triple file {}", f.path, e);
		}
	}


	@Override
	public BufferedTriples get() {
		if (done || !source.hasNewFile()) {
			done = true;
			return null;
		}
		BufferedTriples buffer = new BufferedTriples();
		ParallelSortableArrayList<TripleID> tripleIDS = buffer.triples;
		listener.notifyProgress(10, "reading triples part2  " + triplesCount);
		while (source.hasNext()) {
			if (tripleIDS.size() == Integer.MAX_VALUE - 5) {
				source.forceNewFile();
				continue;
			}
			TripleID next = source.next();
			TripleID mappedTriple = new TripleID(
					mapper.extractSubject(next.getSubject()),
					mapper.extractPredicate(next.getPredicate()),
					mapper.extractObjects(next.getObject())
			);
			assert mappedTriple.isValid();
			tripleIDS.add(mappedTriple);
			triplesCount++;
			if (triplesCount % 100_000 == 0) {
				listener.notifyProgress(10, "reading triples part2 " + triplesCount);
			}
		}

		return buffer;
	}

	@Override
	public TripleFile map(BufferedTriples buffer) {
		try {
			ParallelSortableArrayList<TripleID> tripleIDS = buffer.triples;
			tripleIDS.parallelSort(TripleIDComparator.getComparator(order));
			int fid = FID.incrementAndGet();
			CloseSuppressPath triplesFiles = baseFileName.resolve("triples" + fid + ".raw");
			long triples = 0;
			int count = 0;
			int block = tripleIDS.size() < 10 ? 1 : tripleIDS.size() / 10;
			IntermediateListener il = new IntermediateListener(listener);
			il.setRange(70, 100);
			il.setPrefix("writing triples " + triplesFiles.getFileName() + " ");
			try (CompressTripleWriter w = new CompressTripleWriter(triplesFiles.openOutputStream(bufferSize))) {
				il.notifyProgress(0, "creating file");
				TripleID prev = new TripleID(-1, -1, -1);
				for (TripleID triple : tripleIDS) {
					count++;
					if (count % block == 0) {
						il.notifyProgress(count / (block / 10f), "writing triples " + count + "/" + tripleIDS.size());
					}
					if (prev.match(triple)) {
						continue;
					}
					prev.setAll(triple.getSubject(), triple.getPredicate(), triple.getObject());
					w.appendTriple(triple);
					triples++;
				}
				listener.notifyProgress(100, "writing completed " + triplesCount + " " + triplesFiles.getFileName());
			}
			return new TripleFile(triples, triplesFiles);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * merge these triples into a file
	 *
	 * @param workers      number of worker
	 * @param nodePerMerge the number of node layer per merge
	 * @return result
	 * @throws TreeWorker.TreeWorkerException TreeWorker error
	 * @throws InterruptedException           thread interruption
	 * @throws IOException                    io error
	 */
	public TripleCompressionResult mergeToFile(int workers, int nodePerMerge) throws TreeWorker.TreeWorkerException, InterruptedException, IOException {
		// force to create the first file
		TreeWorker<BufferedTriples, TripleFile> treeWorker = new TreeWorker<>(this, TripleFile[]::new, workers, nodePerMerge);
		treeWorker.setListener(listener);
		treeWorker.start();
		// wait for the workers to merge the sections and create the triples
		CloseSuppressPath triples = treeWorker.waitToComplete().path;
		return new TripleCompressionResultFile(triplesCount, triples, order, bufferSize);
	}

	/**
	 * merge these triples while reading them, increase the memory usage
	 *
	 * @return result
	 * @throws IOException io error
	 */
	public TripleCompressionResult mergeToPartial() throws IOException {
		BufferedTriples triples;
		List<CloseSuppressPath> files = new ArrayList<>();
		try {
			while ((triples = get()) != null) {
				files.add(map(triples).path);
			}
		} catch (RuntimeException e) {
			IOUtil.closeAll(files);
			throw e;
		}
		return new TripleCompressionResultPartial(files, triplesCount, order, bufferSize);
	}

	/**
	 * merge the triples into a result
	 *
	 * @param workers      number of workers (complete mode)
	 * @param mode         the mode of merging
	 * @param nodePerMerge the number of node layer per merge
	 * @return result
	 * @throws TreeWorker.TreeWorkerException TreeWorker error (complete mode)
	 * @throws InterruptedException           thread interruption (complete mode)
	 * @throws IOException                    io error
	 */
	public TripleCompressionResult merge(int workers, int nodePerMerge, String mode) throws TreeWorker.TreeWorkerException, InterruptedException, IOException {
		if (mode == null) {
			mode = "";
		}
		switch (mode) {
			case "":
			case CompressionResult.COMPRESSION_MODE_COMPLETE:
				return mergeToFile(workers, nodePerMerge);
			case CompressionResult.COMPRESSION_MODE_PARTIAL:
				return mergeToPartial();
			default:
				throw new IllegalArgumentException("Unknown compression mode: " + mode);
		}
	}

	public static class TripleFile implements Closeable {
		long triples;
		CloseSuppressPath path;

		private TripleFile(long triples, CloseSuppressPath path) {
			this.triples = triples;
			this.path = path;
		}

		@Override
		public void close() throws IOException {
			path.close();
		}
	}

	public static class BufferedTriples {
		ParallelSortableArrayList<TripleID> triples = new ParallelSortableArrayList<>(TripleID[].class);

		private BufferedTriples() {
		}
	}
}
