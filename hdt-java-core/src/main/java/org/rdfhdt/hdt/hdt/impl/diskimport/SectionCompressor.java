package org.rdfhdt.hdt.hdt.impl.diskimport;

import org.rdfhdt.hdt.iterator.utils.FileTripleIterator;
import org.rdfhdt.hdt.listener.MultiThreadListener;
import org.rdfhdt.hdt.triples.IndexedNode;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.util.ParallelSortableArrayList;
import org.rdfhdt.hdt.util.concurrent.ExceptionFunction;
import org.rdfhdt.hdt.util.concurrent.ExceptionSupplier;
import org.rdfhdt.hdt.util.concurrent.ExceptionThread;
import org.rdfhdt.hdt.util.concurrent.TreeWorker;
import org.rdfhdt.hdt.util.io.CloseSuppressPath;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.io.compress.CompressNodeMergeIterator;
import org.rdfhdt.hdt.util.io.compress.CompressNodeReader;
import org.rdfhdt.hdt.util.io.compress.CompressUtil;
import org.rdfhdt.hdt.util.listener.IntermediateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Tree worker object to compress the section of a triple stream into 3 sections (SPO) and a compress triple file
 *
 * @author Antoine Willerval
 */
public class SectionCompressor implements TreeWorker.TreeWorkerObject<SectionCompressor.BufferedSection, SectionCompressor.TripleFile> {
	private static final AtomicInteger ID_INC = new AtomicInteger();
	private static final Logger log = LoggerFactory.getLogger(SectionCompressor.class);

	private final CloseSuppressPath baseFileName;
	private final FileTripleIterator source;
	private boolean done;
	private final MultiThreadListener listener;
	private long triples = 0;
	private final int bufferSize;
	private final IdFetcher subjectIdFetcher = new IdFetcher();
	private final IdFetcher predicateIdFetcher = new IdFetcher();
	private final IdFetcher objectIdFetcher = new IdFetcher();

	public SectionCompressor(CloseSuppressPath baseFileName, FileTripleIterator source, MultiThreadListener listener, int bufferSize) {
		this.source = source;
		this.listener = listener;
		this.baseFileName = baseFileName;
		this.bufferSize = bufferSize;
	}

	/**
	 * @return the next file to merge
	 */
	@Override
	public SectionCompressor.BufferedSection get() {
		if (done || !source.hasNewFile()) {
			done = true;
			return null;
		}

		listener.notifyProgress(0, "start reading triples");

		BufferedSection buffer = new BufferedSection();
		ParallelSortableArrayList<IndexedNode> subjects = buffer.subjects;
		ParallelSortableArrayList<IndexedNode> predicates = buffer.predicates;
		ParallelSortableArrayList<IndexedNode> objects = buffer.objects;

		listener.notifyProgress(10, "reading triples " + triples);
		while (source.hasNext()) {
			// too much ram allowed?
			if (subjects.size() == Integer.MAX_VALUE - 5) {
				source.forceNewFile();
				continue;
			}
			TripleString next = source.next();

			// get indexed mapped char sequence
			IndexedNode subjectNode = new IndexedNode(
					convertSubject(next.getSubject()),
					subjectIdFetcher.getNodeId()
			);
			subjects.add(subjectNode);

			// get indexed mapped char sequence
			IndexedNode predicateNode = new IndexedNode(
					convertPredicate(next.getPredicate()),
					predicateIdFetcher.getNodeId()
			);
			predicates.add(predicateNode);

			// get indexed mapped char sequence
			IndexedNode objectNode = new IndexedNode(
					convertObject(next.getObject()),
					objectIdFetcher.getNodeId()
			);
			objects.add(objectNode);

			// load the map triple and write it in the writer
			triples++;

			if (triples % 100_000 == 0) {
				listener.notifyProgress(10, "reading triples " + triples);
			}
		}

		return buffer;
	}

	@Override
	public TripleFile map(BufferedSection buffer) {
		ParallelSortableArrayList<IndexedNode> subjects = buffer.subjects;
		ParallelSortableArrayList<IndexedNode> predicates = buffer.predicates;
		ParallelSortableArrayList<IndexedNode> objects = buffer.objects;
		try {
			int fid = ID_INC.incrementAndGet();
			TripleFile sections = new TripleFile(baseFileName.resolve("section" + fid + ".raw"));
			try {
				IntermediateListener il = new IntermediateListener(listener);
				il.setRange(70, 80);
				il.setPrefix("creating subjects section " + sections.root.getFileName() + ": ");
				il.notifyProgress(0, "sorting");
				try (OutputStream stream = sections.openWSubject()) {
					subjects.parallelSort(IndexedNode::compareTo);
					CompressUtil.writeCompressedSection(subjects, stream, il);
				}
				il.setRange(80, 90);
				il.setPrefix("creating predicates section " + sections.root.getFileName() + ": ");
				il.notifyProgress(0, "sorting");
				try (OutputStream stream = sections.openWPredicate()) {
					predicates.parallelSort(IndexedNode::compareTo);
					CompressUtil.writeCompressedSection(predicates, stream, il);
				}
				il.setRange(90, 100);
				il.setPrefix("creating objects section " + sections.root.getFileName() + ": ");
				il.notifyProgress(0, "sorting");
				try (OutputStream stream = sections.openWObject()) {
					objects.parallelSort(IndexedNode::compareTo);
					CompressUtil.writeCompressedSection(objects, stream, il);
				}
			} finally {
				subjects.clear();
				predicates.clear();
				objects.clear();
				listener.notifyProgress(100, "section completed" + sections.root.getFileName().toString());
			}
			return sections;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public SectionCompressor.TripleFile construct(SectionCompressor.TripleFile[] triples, int count) {
		int fid = ID_INC.incrementAndGet();
		TripleFile sections;
		try {
			sections = new TripleFile(baseFileName.resolve("section" + fid + ".raw"));
			sections.compute(triples, count, false);
			listener.notifyProgress(100, "sections merged " + sections.root.getFileName());
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
		// delete old sections
		for (int i = 0; i < count; i++) {
			delete(triples[i]);
		}
		return sections;
	}

	/**
	 * delete the file f if it exists or warn
	 *
	 * @param f the file to delete
	 */
	@Override
	public void delete(SectionCompressor.TripleFile f) {
		f.close();
	}

	/*
	 * FIXME: create a factory and override these methods with the hdt spec
	 */

	/**
	 * mapping method for the subject of the triple, this method should copy the sequence!
	 *
	 * @param seq the subject (before)
	 * @return the subject mapped
	 */
	protected CharSequence convertSubject(CharSequence seq) {
		return seq.toString();
	}

	/**
	 * mapping method for the predicate of the triple, this method should copy the sequence!
	 *
	 * @param seq the predicate (before)
	 * @return the predicate mapped
	 */
	protected CharSequence convertPredicate(CharSequence seq) {
		return seq.toString();
	}

	/**
	 * mapping method for the object of the triple, this method should copy the sequence!
	 *
	 * @param seq the object (before)
	 * @return the object mapped
	 */
	protected CharSequence convertObject(CharSequence seq) {
		return seq.toString();
	}

	/**
	 * Compress the stream into complete pre-sections files
	 *
	 * @param workers      the number of workers
	 * @param nodePerMerge the number of node layer per merge
	 * @return compression result
	 * @throws IOException                    io exception
	 * @throws InterruptedException           if the thread is interrupted
	 * @throws TreeWorker.TreeWorkerException exception with the tree working
	 * @see #compressPartial()
	 * @see #compress(int, int, String)
	 */
	public CompressionResult compressToFile(int workers, int nodePerMerge) throws IOException, InterruptedException, TreeWorker.TreeWorkerException {
		// force to create the first file
		TreeWorker<BufferedSection, TripleFile> treeWorker = new TreeWorker<>(this, TripleFile[]::new, workers, nodePerMerge);
		treeWorker.setListener(listener);
		treeWorker.start();
		// wait for the workers to merge the sections and create the triples
		TripleFile sections = treeWorker.waitToComplete();
		return new CompressionResultFile(triples, sections);
	}

	/**
	 * Compress the stream into multiple pre-sections files and merge them on the fly
	 *
	 * @return compression result
	 * @throws IOException io exception
	 * @see #compressToFile(int, int)
	 * @see #compress(int, int, String)
	 */
	public CompressionResult compressPartial() throws IOException {
		BufferedSection section;
		List<TripleFile> files = new ArrayList<>();
		try {
			while ((section = get()) != null) {
				files.add(map(section));
			}
		} catch (RuntimeException e) {
			IOUtil.closeAll(files);
			throw e;
		}
		return new CompressionResultPartial(files, triples);
	}

	/**
	 * compress the sections/triples with a particular mode
	 *
	 * @param workers      the worker required
	 * @param mode         the mode to compress, can be {@link org.rdfhdt.hdt.hdt.impl.diskimport.CompressionResult#COMPRESSION_MODE_COMPLETE} (default), {@link org.rdfhdt.hdt.hdt.impl.diskimport.CompressionResult#COMPRESSION_MODE_PARTIAL} or null/"" for default
	 * @param nodePerMerge the number of node layer per merge
	 * @return the compression result
	 * @throws TreeWorker.TreeWorkerException tree working exception
	 * @throws IOException                    io exception
	 * @throws InterruptedException           thread interruption
	 * @see #compressToFile(int, int)
	 * @see #compressPartial()
	 */
	public CompressionResult compress(int workers, int nodePerMerge, String mode) throws TreeWorker.TreeWorkerException, IOException, InterruptedException {
		if (mode == null) {
			mode = "";
		}
		switch (mode) {
			case "":
			case CompressionResult.COMPRESSION_MODE_COMPLETE:
				return compressToFile(workers, nodePerMerge);
			case CompressionResult.COMPRESSION_MODE_PARTIAL:
				return compressPartial();
			default:
				throw new IllegalArgumentException("Unknown compression mode: " + mode);
		}
	}

	/**
	 * A triple directory, contains 3 files, subject, predicate and object
	 *
	 * @author Antoine Willerval
	 */
	public class TripleFile implements Closeable {
		private final CloseSuppressPath root;
		private final CloseSuppressPath s;
		private final CloseSuppressPath p;
		private final CloseSuppressPath o;

		private TripleFile(CloseSuppressPath root) throws IOException {
			this.root = root;
			this.s = root.resolve("subject");
			this.p = root.resolve("predicate");
			this.o = root.resolve("object");

			root.closeWithDeleteRecurse();
			root.mkdirs();
		}

		/**
		 * delete the directory
		 */
		@Override
		public void close() {
			try {
				root.close();
			} catch (IOException e) {
				log.warn("Can't delete sections {}: {}", root, e);
			}
		}

		/**
		 * @return open a write stream to the subject file
		 * @throws IOException can't open the stream
		 */
		public OutputStream openWSubject() throws IOException {
			return s.openOutputStream(bufferSize);
		}

		/**
		 * @return open a write stream to the predicate file
		 * @throws IOException can't open the stream
		 */
		public OutputStream openWPredicate() throws IOException {
			return p.openOutputStream(bufferSize);
		}

		/**
		 * @return open a write stream to the object file
		 * @throws IOException can't open the stream
		 */
		public OutputStream openWObject() throws IOException {
			return o.openOutputStream(bufferSize);
		}

		/**
		 * @return open a read stream to the subject file
		 * @throws IOException can't open the stream
		 */
		public InputStream openRSubject() throws IOException {
			return s.openInputStream(bufferSize);
		}

		/**
		 * @return open a read stream to the predicate file
		 * @throws IOException can't open the stream
		 */
		public InputStream openRPredicate() throws IOException {
			return p.openInputStream(bufferSize);
		}

		/**
		 * @return open a read stream to the object file
		 * @throws IOException can't open the stream
		 */
		public InputStream openRObject() throws IOException {
			return o.openInputStream(bufferSize);
		}

		/**
		 * @return the path to the subject file
		 */
		public CloseSuppressPath getSubjectPath() {
			return s;
		}

		/**
		 * @return the path to the predicate file
		 */
		public CloseSuppressPath getPredicatePath() {
			return p;
		}

		/**
		 * @return the path to the object file
		 */
		public CloseSuppressPath getObjectPath() {
			return o;
		}

		/**
		 * compute this triple file from multiple triples files
		 *
		 * @param triples triples files container
		 * @param count   length of the container (index start at 0)
		 * @param async   if the method should load all the files asynchronously or not
		 * @throws IOException          io exception while reading/writing
		 * @throws InterruptedException interruption while waiting for the async thread
		 */
		public void compute(SectionCompressor.TripleFile[] triples, int count, boolean async) throws IOException, InterruptedException {
			if (!async) {
				computeSubject(triples, count, false);
				computePredicate(triples, count, false);
				computeObject(triples, count, false);
			} else {
				ExceptionThread.async("SectionMerger" + root.getFileName(),
						() -> computeSubject(triples, count, true),
						() -> computePredicate(triples, count, true),
						() -> computeObject(triples, count, true)
				).joinAndCrashIfRequired();
			}
		}

		private void computeSubject(SectionCompressor.TripleFile[] triples, int count, boolean async) throws IOException {
			computeSection(triples, count, "subject", 0, 33, this::openWSubject, TripleFile::openRSubject, TripleFile::getSubjectPath, async);
		}

		private void computePredicate(SectionCompressor.TripleFile[] triples, int count, boolean async) throws IOException {
			computeSection(triples, count, "predicate", 33, 66, this::openWPredicate, TripleFile::openRPredicate, TripleFile::getPredicatePath, async);
		}

		private void computeObject(SectionCompressor.TripleFile[] triples, int count, boolean async) throws IOException {
			computeSection(triples, count, "object", 66, 100, this::openWObject, TripleFile::openRObject, TripleFile::getObjectPath, async);
		}

		private void computeSection(SectionCompressor.TripleFile[] triples, int count, String section, int start, int end, ExceptionSupplier<OutputStream, IOException> openW, ExceptionFunction<TripleFile, InputStream, IOException> openR, Function<TripleFile, Closeable> fileDelete, boolean async) throws IOException {
			IntermediateListener il = new IntermediateListener(listener);
			if (async) {
				listener.registerThread(Thread.currentThread().getName());
			} else {
				il.setRange(start, end);
			}
			il.setPrefix("merging " + section + " section " + root.getFileName() + ": ");
			il.notifyProgress(0, "merging section");
			CompressNodeReader[] readers = new CompressNodeReader[count];
			Closeable[] fileDeletes = new Closeable[count];
			try {
				long size = 0L;
				for (int i = 0; i < count; i++) {
					CompressNodeReader reader = new CompressNodeReader(openR.apply(triples[i]));
					size += reader.getSize();
					readers[i] = reader;
					fileDeletes[i] = fileDelete.apply(triples[i]);
				}

				// section
				try (OutputStream output = openW.get()) {
					CompressUtil.writeCompressedSection(CompressNodeMergeIterator.buildOfTree(readers), size, output, il);
				}
			} finally {
				if (async) {
					listener.unregisterThread(Thread.currentThread().getName());
				}
				try {
					IOUtil.closeAll(readers);
				} finally {
					IOUtil.closeAll(fileDeletes);
				}
			}
		}
	}

	private static class IdFetcher {
		private long id = 0;

		public long getNodeId() {
			return ++id;
		}

		public long getCount() {
			return id;
		}
	}

	public static class BufferedSection {
		private final ParallelSortableArrayList<IndexedNode> subjects = new ParallelSortableArrayList<>(IndexedNode[].class);
		private final ParallelSortableArrayList<IndexedNode> predicates = new ParallelSortableArrayList<>(IndexedNode[].class);
		private final ParallelSortableArrayList<IndexedNode> objects = new ParallelSortableArrayList<>(IndexedNode[].class);

		private BufferedSection() {
		}
	}
}
