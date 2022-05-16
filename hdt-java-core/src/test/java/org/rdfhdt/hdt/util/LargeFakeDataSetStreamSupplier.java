package org.rdfhdt.hdt.util;

import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.triples.TripleString;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Random;

public class LargeFakeDataSetStreamSupplier {

	private static final Charset DEFAULT_CHARSET = Charset.defaultCharset();

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

	public HDT createFakeHDTTwoPass(HDTOptions spec) throws ParserException, IOException {
		Path f = Paths.get("tempNtFile.nt").toAbsolutePath();
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
		HDT hdt = createFakeHDT(spec);
		hdt.saveToHDT(location, null);
		hdt.close();
	}
	public void createAndSaveFakeHDTTwoPass(HDTOptions spec, String location) throws ParserException, IOException {
		HDT hdt = createFakeHDTTwoPass(spec);
		hdt.saveToHDT(location, null);
		hdt.close();
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

		String text = "\"" + stringNameOfInt(random.nextInt(maxElementSplit)) + "\"";
		if (random.nextBoolean()) {
			// language node
			return text + "@" + stringNameOfInt(random.nextInt(maxElementSplit));
		} else {
			// typed node
			return text + "^^<" + createType() + ">";
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

}