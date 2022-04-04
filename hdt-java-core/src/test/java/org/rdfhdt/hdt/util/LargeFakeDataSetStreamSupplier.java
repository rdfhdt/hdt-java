package org.rdfhdt.hdt.util;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.rdfhdt.hdt.enums.CompressionType;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.util.concurrent.ExceptionThread;
import org.rdfhdt.hdt.util.string.ByteStringUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

public class LargeFakeDataSetStreamSupplier {

	private static final Charset DEFAULT_CHARSET = ByteStringUtil.STRING_ENCODING;

	/**
	 * create a lowercase name from a number, to create string without any number in it
	 *
	 * @param i id
	 * @return string
	 */
	public static String stringNameOfInt(int i) {
		String table = "abcdefghijklmnopqrstuvwxyz";
		StringBuilder out = new StringBuilder();
		int c = i;
		do {
			out.append(table.charAt(c % table.length()));
			c /= table.length();
		} while (c != 0);
		return out.toString();
	}

	/**
	 * estimate the size of a triple
	 *
	 * @param triple the triple
	 * @return the size in byte to store the triple
	 */
	public static long estimateTripleSize(TripleString triple) {
		try {
			return triple.asNtriple().toString().getBytes(DEFAULT_CHARSET).length;
		} catch (IOException e) {
			throw new RuntimeException("Can't estimate the size of the triple " + triple, e);
		}
	}

	public static LargeFakeDataSetStreamSupplier createSupplierWithMaxSize(long maxSize, long seed) {
		return new LargeFakeDataSetStreamSupplier(maxSize, Long.MAX_VALUE, seed);
	}

	public static LargeFakeDataSetStreamSupplier createSupplierWithMaxTriples(long maxTriples, long seed) {
		return new LargeFakeDataSetStreamSupplier(Long.MAX_VALUE, maxTriples, seed);
	}

	private final long seed;
	private Random random;
	private final long maxSize;
	private final long maxTriples;
	public int maxFakeType = 10;
	public int maxLiteralSize = 2;
	public int maxElementSplit = Integer.MAX_VALUE;

	private LargeFakeDataSetStreamSupplier(long maxSize, long maxTriples, long seed) {
		this.maxSize = maxSize;
		this.maxTriples = maxTriples;
		this.seed = seed;
		reset();
	}

	public void reset() {
		random = new Random(seed);
	}

	public Iterator<TripleString> createTripleStringStream() {
		return new FakeStatementIterator();
	}

	public void createNTFile(String file) throws IOException {
		try (FileWriter writer = new FileWriter(file)) {
			for (Iterator<TripleString> it = createTripleStringStream(); it.hasNext(); ) {
				it.next().dumpNtriple(writer);
			}
		}
	}

	public ThreadedStream createNTInputStream(CompressionType compressionType) throws IOException {
		PipedOutputStream pout = new PipedOutputStream();
		InputStream is = new PipedInputStream(pout);
		OutputStream out;

		if (compressionType != null) {
			switch (compressionType) {
				case NONE:
					out = pout;
					break;
				case XZ:
					out = new XZCompressorOutputStream(pout);
					break;
				case BZIP:
					out = new BZip2CompressorOutputStream(pout);
					break;
				case GZIP:
					out = new GZIPOutputStream(pout);
					break;
				default:
					throw new NotImplementedException(compressionType.name());
			}
		} else {
			out = pout;
		}

		ExceptionThread run = new ExceptionThread(() -> {
			try (PrintStream ps = new PrintStream(out, true)) {
				Iterator<TripleString> it = createTripleStringStream();
				while (it.hasNext()) {
					it.next().dumpNtriple(ps);
				}
			}
		},
				"ThreadedFakedStream");
		run.start();

		return new ThreadedStream(run, is);
	}

	public HDT createFakeHDTTwoPass(HDTOptions spec) throws ParserException, IOException {
		Path f = Path.of("tempNtFile.nt").toAbsolutePath();
		try {
			createNTFile(f.toString());
			spec.set("loader.type", "two-pass");
			return HDTManager.generateHDT(f.toString(), "http://w", RDFNotation.NTRIPLES, spec, null);
		} finally {
			Files.deleteIfExists(f);
		}
	}
	public HDT createFakeHDT(HDTOptions spec) throws ParserException, IOException {
		return HDTManager.generateHDT(createTripleStringStream(), "http://w", spec, null);
	}

	public void createAndSaveFakeHDT(HDTOptions spec, String location) throws ParserException, IOException {
		try (HDT hdt = createFakeHDT(spec)) {
			hdt.saveToHDT(location, null);
		}
	}
	public void createAndSaveFakeHDTTwoPass(HDTOptions spec, String location) throws ParserException, IOException {
		try (HDT hdt = createFakeHDTTwoPass(spec)) {
			hdt.saveToHDT(location, null);
		}
	}

	private CharSequence createSubject() {
		return createPredicate();
	}

	private CharSequence createPredicate() {
		return "http://w" + random.nextInt(maxElementSplit) + "i.test.org/#Obj" + random.nextInt(maxElementSplit);
	}

	private CharSequence createType() {
		return "http://wti.test.org/#Obj" + random.nextInt(maxFakeType);
	}

	private CharSequence createValue() {
		if (random.nextBoolean()) {
			return createPredicate();
		}
		int size = random.nextInt(maxLiteralSize);
		StringBuilder litText = new StringBuilder();
		for (int i = 0; i < size; i++) {
			litText.append(stringNameOfInt(random.nextInt(maxElementSplit))).append(" ");
		}
		String text = "\"" + litText + "\"";
		int litType = random.nextInt(3);
		if (litType == 1) {
			// language node
			return text + "@" + stringNameOfInt(random.nextInt(maxElementSplit));
		} else if (litType == 2) {
			// typed node
			return text + "^^<" + createType() + ">";
		} else {
			// no type/language node
			return text;
		}
	}

	private class FakeStatementIterator implements Iterator<TripleString> {
		private long size;
		private long count;
		private TripleString next;

		@Override
		public boolean hasNext() {
			if (size >= maxSize || count >= maxTriples) {
				return false;
			}
			if (next != null) {
				return true;
			}

			next = new TripleString(
					createSubject(),
					createPredicate(),
					createValue()
			);

			long estimation = estimateTripleSize(
					new TripleString(
							next.getSubject().toString(),
							next.getPredicate().toString(),
							next.getObject().toString()
					)
			);
			size += estimation;
			count++;

			return size < maxSize && count < maxTriples;
		}

		@Override
		public TripleString next() {
			if (!hasNext()) {
				return null;
			}
			TripleString next = this.next;
			this.next = null;
			return next;
		}
	}

	public LargeFakeDataSetStreamSupplier withMaxFakeType(int maxFakeType) {
		this.maxFakeType = maxFakeType;
		return this;
	}

	public LargeFakeDataSetStreamSupplier withMaxElementSplit(int maxElementSplit) {
		this.maxElementSplit = maxElementSplit;
		return this;
	}

	public LargeFakeDataSetStreamSupplier withMaxLiteralSize(int maxLiteralSize) {
		this.maxLiteralSize = maxLiteralSize;
		return this;

	}

	public static class ThreadedStream {
		private final ExceptionThread thread;
		private final InputStream stream;

		public ThreadedStream(ExceptionThread thread, InputStream stream) {
			this.thread = thread;
			this.stream = stream;
		}

		public ExceptionThread getThread() {
			return thread;
		}

		public InputStream getStream() {
			return stream;
		}
	}
}
