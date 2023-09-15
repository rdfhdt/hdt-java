package org.rdfhdt.hdt.hdt;

import org.apache.commons.io.file.PathUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;
import org.rdfhdt.hdt.compact.bitmap.BitmapFactory;
import org.rdfhdt.hdt.compact.bitmap.ModifiableBitmap;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.dictionary.DictionarySection;
import org.rdfhdt.hdt.dictionary.impl.MultipleBaseDictionary;
import org.rdfhdt.hdt.enums.CompressionType;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.impl.diskimport.CompressionResult;
import org.rdfhdt.hdt.iterator.utils.FetcherIterator;
import org.rdfhdt.hdt.iterator.utils.PipedCopyIterator;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTOptionsKeys;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.rdf.RDFFluxStop;
import org.rdfhdt.hdt.rdf.RDFParserFactory;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.triples.impl.BitmapTriplesIteratorPositionTest;
import org.rdfhdt.hdt.triples.impl.utils.HDTTestUtils;
import org.rdfhdt.hdt.util.LargeFakeDataSetStreamSupplier;
import org.rdfhdt.hdt.util.StopWatch;
import org.rdfhdt.hdt.util.io.AbstractMapMemoryTest;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.io.compress.CompressTest;
import org.rdfhdt.hdt.util.string.ByteString;
import org.rdfhdt.hdt.util.string.CharSequenceComparator;
import org.rdfhdt.hdt.util.string.ReplazableString;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Suite.class)
@Suite.SuiteClasses({
		HDTManagerTest.DynamicDiskTest.class,
		HDTManagerTest.DynamicCatTreeTest.class,
		HDTManagerTest.FileDynamicTest.class,
		HDTManagerTest.StaticTest.class,
		HDTManagerTest.HDTQTest.class,
})
public class HDTManagerTest {
	public static class HDTManagerTestBase extends AbstractMapMemoryTest implements ProgressListener {
		protected static String[][] diskDict() {
			return new String[][]{
					{HDTOptionsKeys.DICTIONARY_TYPE_VALUE_MULTI_OBJECTS, HDTOptionsKeys.TEMP_DICTIONARY_IMPL_VALUE_MULT_HASH},
					{HDTOptionsKeys.DICTIONARY_TYPE_VALUE_FOUR_SECTION, HDTOptionsKeys.TEMP_DICTIONARY_IMPL_VALUE_HASH}
			};
		}

		/**
		 * disable string order consistency test <a href="https://github.com/rdfhdt/hdt-java/issues/177">GH#177</a>
		 */
		protected static final boolean ALLOW_STRING_CONSISTENCY_TEST = false;
		protected static final long SIZE_VALUE = 1L << 16;
		protected static final int SEED = 67;

		private HDTManagerTestBase() {
		}

		@Rule
		public TemporaryFolder tempDir = new TemporaryFolder();
		protected HDTSpecification spec;
		protected Path rootFolder;

		@Before
		public void setupManager() throws IOException {
			spec = new HDTSpecification();
			rootFolder = tempDir.newFolder().toPath();
			spec.set(HDTOptionsKeys.LOADER_DISK_LOCATION_KEY, rootFolder.toAbsolutePath().toString());
		}

		@After
		public void closeManager() throws IOException {
			if (Files.exists(rootFolder)) {
				try (Stream<Path> s = Files.list(rootFolder)) {
					// might be wrong with some OS hidden files?
					assertFalse("root folder not empty", s.findAny().isPresent());
				}
			}
		}

		@Override
		public void notifyProgress(float level, String message) {
			//		System.out.println("[" + level + "] " + message);
		}

		public static void assertEqualsHDT(HDT expected, HDT actual) throws NotFoundException {
			assertEquals("non matching sizes", expected.getTriples().getNumberOfElements(), actual.getTriples().getNumberOfElements());
			// test dictionary
			Dictionary ed = expected.getDictionary();
			Dictionary ad = actual.getDictionary();
			assertEqualsHDT("Subjects", ed.getSubjects(), ad.getSubjects());
			assertEqualsHDT("Predicates", ed.getPredicates(), ad.getPredicates());
			if (ed instanceof MultipleBaseDictionary) {
				assertTrue("ad not a MSD" + ad.getClass(), ad instanceof MultipleBaseDictionary);
				MultipleBaseDictionary edm = (MultipleBaseDictionary) ed;
				MultipleBaseDictionary adm = (MultipleBaseDictionary) ad;
				Map<? extends CharSequence, DictionarySection> keysE = edm.getAllObjects();
				Map<? extends CharSequence, DictionarySection> keysA = adm.getAllObjects();
				assertEquals(keysE.keySet(), keysA.keySet());
				keysE.forEach((key, dictE) -> {
					DictionarySection dictA = keysA.get(key);

					assertEqualsHDT(key.toString(), dictE, dictA);
				});
			} else {
				assertFalse("actual dictionary is of type MultipleBaseDictionary, but ed is, actual: " + ad.getClass() + ", excepted: " + ed.getClass(), ad instanceof MultipleBaseDictionary);
				assertEqualsHDT("Objects", ed.getObjects(), ad.getObjects());
			}
			assertEqualsHDT("Shared", ed.getShared(), ad.getShared());
			assertEquals(ed.getType(), ad.getType());

			// test triples
			IteratorTripleID actualIt = actual.getTriples().searchAll();
			IteratorTripleID expectedIt = expected.getTriples().searchAll();

			while (expectedIt.hasNext()) {
				assertTrue(actualIt.hasNext());

				TripleID expectedTriple = expectedIt.next();
				TripleID actualTriple = actualIt.next();
				long location = expectedIt.getLastTriplePosition();
				assertEquals("The tripleID location doesn't match", location, actualIt.getLastTriplePosition());
				assertEquals("The tripleID #" + location + " doesn't match", expectedTriple, actualTriple);
			}
			assertFalse(actualIt.hasNext());

			// test header
			assertEquals(actual.getHeader().getBaseURI(), expected.getHeader().getBaseURI());
			if (expected.getHeader().getNumberOfElements() != actual.getHeader().getNumberOfElements()) {
				StringBuilder bld = new StringBuilder();

				bld.append("-------- Header excepted:");
				expected.getHeader().search("", "", "").forEachRemaining(l -> bld.append(l).append('\n'));
				bld.append("-------- Header actual:");
				actual.getHeader().search("", "", "").forEachRemaining(l -> bld.append(l).append('\n'));

				fail("Size of the header doesn't match " + bld + expected.getHeader().getNumberOfElements() + "!=" + actual.getHeader().getNumberOfElements());
			}
		}

		public static void checkHDTConsistency(HDT hdt) {
			Dictionary dict = hdt.getDictionary();
			Map<CharSequence, DictionarySection> map;
			map = new HashMap<>();
			if (dict instanceof MultipleBaseDictionary) {
				map.putAll(dict.getAllObjects());
			} else {
				map.put("Objects", dict.getObjects());
			}
			map.put("Subjects", dict.getSubjects());
			map.put("Predicates", dict.getPredicates());
			map.put("Shared", dict.getShared());

			ReplazableString prev = new ReplazableString();
			Comparator<CharSequence> cmp = CharSequenceComparator.getInstance();
			map.forEach((name, section) -> {
				prev.clear();
				String prev2 = "";
				Iterator<? extends CharSequence> it = section.getSortedEntries();
				if (it.hasNext()) {
					prev.replace(it.next());
				}

				while (it.hasNext()) {
					CharSequence next = ByteString.of(it.next());

					int cmpV = cmp.compare(prev, next);
					if (cmpV >= 0) {
						System.out.print("Prev: ");
						printHex(prev);
						System.out.print("Next: ");
						printHex(next);
						System.out.print("Prev: ");
						printBin(prev);
						System.out.print("Next: ");
						printBin(next);

						if (cmpV == 0) {
							fail("[" + name + "] (BS) Duplicated elements! " + prev + " = " + next);
						}
						fail("[" + name + "] (BS) Bad order! " + prev + " > " + next);
					}

					if (ALLOW_STRING_CONSISTENCY_TEST) {
						String nextStr = next.toString();
						int cmpV2 = cmp.compare(prev2, nextStr);
						if (cmpV2 == 0) {
							fail("[" + name + "] (Str) Duplicated elements! " + prev2 + " = " + next);
						}
						if (cmpV2 > 0) {
							fail("[" + name + "] (Str) Bad order! " + prev2 + " > " + next);
						}

						assertEquals("str and byteStr compare aren't returning the same results", Math.signum(cmpV2), Math.signum(cmpV), 0.01);
						prev2 = nextStr;
					}
					prev.replace(next);
				}
			});
		}

		public static void assertEqualsHDT(String section, DictionarySection excepted, DictionarySection actual) {
			assertEquals("sizes of section " + section + " aren't the same!", excepted.getNumberOfElements(), actual.getNumberOfElements());
			Iterator<? extends CharSequence> itEx = excepted.getSortedEntries();
			Iterator<? extends CharSequence> itAc = actual.getSortedEntries();

			while (itEx.hasNext()) {
				assertTrue("dictionary section " + section + " is less big than excepted", itAc.hasNext());
				CharSequence expectedTriple = itEx.next();
				CharSequence actualTriple = itAc.next();
				CompressTest.assertCharSequenceEquals(section + " section strings", expectedTriple, actualTriple);
			}
			assertFalse("dictionary section " + section + " is bigger than excepted", itAc.hasNext());
		}

		protected static void printHex(CharSequence seq) {
			ByteString bs = ByteString.of(seq);
			byte[] buffer = bs.getBuffer();
			int len = bs.length();
			for (int i = 0; i < len; i++) {
				System.out.printf("%2x ", buffer[i] & 0xFF);
			}
			System.out.println();
		}

		protected static void printBin(CharSequence seq) {
			ByteString bs = ByteString.of(seq);
			byte[] buffer = bs.getBuffer();
			int len = bs.length();
			for (int i = 0; i < len; i++) {
				System.out.print(Integer.toBinaryString(buffer[i] & 0xFF) + " ");
			}
			System.out.println();
		}
	}

	@RunWith(Parameterized.class)
	public static class DynamicDiskTest extends HDTManagerTestBase {

		@Parameterized.Parameters(name = "{7} - {0}")
		public static Collection<Object[]> params() {
			List<Object[]> params = new ArrayList<>();
			for (String[] dict : diskDict()) {
				params.addAll(List.of(
						new Object[]{"slow-str1", 10, 2, 4, 2, HDTOptionsKeys.LOADER_DISK_COMPRESSION_MODE_VALUE_COMPLETE, false, dict[0], dict[1], 2, "debug.disk.slow.stream=true"},
						new Object[]{"slow-str2", 10, 2, 4, 2, HDTOptionsKeys.LOADER_DISK_COMPRESSION_MODE_VALUE_COMPLETE, false, dict[0], dict[1], 2, "debug.disk.slow.stream2=true"},
						new Object[]{"slow-cfsd", 10, 2, 4, 2, HDTOptionsKeys.LOADER_DISK_COMPRESSION_MODE_VALUE_COMPLETE, false, dict[0], dict[1], 2, "debug.disk.slow.pfsd=true"},
						new Object[]{"slow-kw-d", 10, 2, 4, 2, HDTOptionsKeys.LOADER_DISK_COMPRESSION_MODE_VALUE_COMPLETE, false, dict[0], dict[1], 2, "debug.disk.slow.kway.dict=true"},
						new Object[]{"slow-kw-t", 10, 2, 4, 2, HDTOptionsKeys.LOADER_DISK_COMPRESSION_MODE_VALUE_COMPLETE, false, dict[0], dict[1], 2, "debug.disk.slow.kway.triple=true"}
				));
				for (int threads : new int[]{
						// sync
						1,
						// async, low thread count
						2,
						// async, large thread count
						8
				}) {
					List<String> modes;
					if (threads > 1) {
						// async, no need for partial
						modes = List.of(
								HDTOptionsKeys.LOADER_DISK_COMPRESSION_MODE_VALUE_COMPLETE
						);
					} else {
						modes = List.of(
								HDTOptionsKeys.LOADER_DISK_COMPRESSION_MODE_VALUE_PARTIAL,
								HDTOptionsKeys.LOADER_DISK_COMPRESSION_MODE_VALUE_COMPLETE
						);
					}
					for (String mode : modes) {
						params.addAll(List.of(
								new Object[]{"base-w" + threads + "-" + mode, SIZE_VALUE * 8, 20, 50, threads, mode, false, dict[0], dict[1], SIZE_VALUE, ""},
								new Object[]{"duplicates-w" + threads + "-" + mode, SIZE_VALUE * 8, 10, 50, threads, mode, false, dict[0], dict[1], SIZE_VALUE, ""},
								new Object[]{"large-literals-w" + threads + "-" + mode, SIZE_VALUE * 2, 20, 250, threads, mode, false, dict[0], dict[1], SIZE_VALUE, ""},
								new Object[]{"quiet-w" + threads + "-" + mode, SIZE_VALUE * 8, 10, 50, threads, mode, false, dict[0], dict[1], SIZE_VALUE, ""}
						));
					}
				}
			}

			return params;
		}

		@Parameterized.Parameter
		public String name;
		@Parameterized.Parameter(1)
		public long maxSize;
		@Parameterized.Parameter(2)
		public int maxElementSplit;
		@Parameterized.Parameter(3)
		public int maxLiteralSize;
		@Parameterized.Parameter(4)
		public int threads;
		@Parameterized.Parameter(5)
		public String compressMode;
		@Parameterized.Parameter(6)
		public boolean quiet;
		@Parameterized.Parameter(7)
		public String dictionaryType;
		@Parameterized.Parameter(8)
		public String tempDictionaryType;
		@Parameterized.Parameter(9)
		public long size;
		@Parameterized.Parameter(10)
		public String addedSpecs;

		@Before
		public void setupSpecs() {
			spec.setOptions(addedSpecs);
			spec.set(HDTOptionsKeys.LOADER_DISK_COMPRESSION_WORKER_KEY, String.valueOf(threads));
			spec.set(HDTOptionsKeys.LOADER_DISK_COMPRESSION_MODE_KEY, compressMode);
			spec.set(HDTOptionsKeys.DICTIONARY_TYPE_KEY, dictionaryType);
			spec.set(HDTOptionsKeys.TEMP_DICTIONARY_IMPL_KEY, tempDictionaryType);
			spec.set(HDTOptionsKeys.LOADER_DISK_NO_COPY_ITERATOR_KEY, true);
		}

		private void generateDiskTest() throws IOException, ParserException, NotFoundException, InterruptedException {
			LargeFakeDataSetStreamSupplier supplier =
					LargeFakeDataSetStreamSupplier
							.createSupplierWithMaxSize(maxSize, SEED)
							.withMaxElementSplit(maxElementSplit)
							.withMaxLiteralSize(maxLiteralSize)
							.withSameTripleString(true)
							.withUnicode(true);

			if (spec.getBoolean("debug.disk.slow.stream")) {
				supplier.withSlowStream(25);
			}

			// create DISK HDT
			LargeFakeDataSetStreamSupplier.ThreadedStream genActual = supplier.createNTInputStream(CompressionType.GZIP);
			HDT actual = null;
			try {
				actual = HDTManager.generateHDTDisk(
						genActual.getStream(),
						HDTTestUtils.BASE_URI,
						RDFNotation.NTRIPLES,
						CompressionType.GZIP,
						spec,
						quiet ? null : this
				);
				checkHDTConsistency(actual);
			} finally {
				if (actual == null) {
					genActual.getThread().interrupt();
				}
			}
			genActual.getThread().joinAndCrashIfRequired();

			supplier.reset();

			LargeFakeDataSetStreamSupplier.ThreadedStream genExpected = supplier.createNTInputStream(CompressionType.GZIP);
			// create MEMORY HDT
			HDT expected = null;
			try {
				expected = HDTManager.generateHDT(
						genExpected.getStream(),
						HDTTestUtils.BASE_URI,
						RDFNotation.NTRIPLES,
						CompressionType.GZIP,
						spec,
						null
				);
				checkHDTConsistency(expected);
			} finally {
				if (expected == null) {
					genExpected.getThread().interrupt();
				}
			}
			genExpected.getThread().joinAndCrashIfRequired();

			// happy compiler, should throw before
			assertNotNull(expected);
			assertNotNull(actual);
			try {
				assertEqualsHDT(expected, actual);
			} finally {
				IOUtil.closeAll(expected, actual);
			}
		}

		@Test
		public void generateSaveLoadMapTest() throws IOException, ParserException, NotFoundException {
			LargeFakeDataSetStreamSupplier supplier =
					LargeFakeDataSetStreamSupplier
							.createSupplierWithMaxSize(maxSize, SEED)
							.withMaxElementSplit(maxElementSplit)
							.withMaxLiteralSize(maxLiteralSize)
							.withUnicode(true);

			// create MEMORY HDT

			try (HDT expected = HDTManager.generateHDT(
					supplier.createTripleStringStream(),
					HDTTestUtils.BASE_URI,
					spec,
					quiet ? null : this
			)) {
				String tmp = tempDir.newFile().getAbsolutePath();
				expected.saveToHDT(tmp, null);

				try (HDT mapExcepted = HDTManager.mapHDT(tmp, quiet ? null : this)) {
					assertEqualsHDT(expected, mapExcepted);
				}

				try (HDT loadExcepted = HDTManager.loadHDT(tmp, quiet ? null : this)) {
					assertEqualsHDT(expected, loadExcepted);
				}
			}

		}

		@Test
		public void generateDiskMemTest() throws IOException, ParserException, NotFoundException, InterruptedException {
			spec.set(HDTOptionsKeys.LOADER_DISK_CHUNK_SIZE_KEY, size);
			spec.set("debug.disk.build", true);
			generateDiskTest();
		}

		@Test
		public void generateDiskMapTest() throws IOException, ParserException, NotFoundException, InterruptedException {
			spec.set(HDTOptionsKeys.LOADER_DISK_CHUNK_SIZE_KEY, size);
			spec.set("debug.disk.build", true);
			Path mapHDT = tempDir.newFile("mapHDTTest.hdt").toPath();
			spec.set(HDTOptionsKeys.LOADER_DISK_FUTURE_HDT_LOCATION_KEY, mapHDT.toAbsolutePath());
			generateDiskTest();
			Files.deleteIfExists(mapHDT);
		}

		@Test
		public void catTreeTest() throws IOException, ParserException, NotFoundException, InterruptedException {
			LargeFakeDataSetStreamSupplier supplier =
					LargeFakeDataSetStreamSupplier
							.createSupplierWithMaxSize(maxSize, SEED)
							.withMaxElementSplit(maxElementSplit)
							.withMaxLiteralSize(maxLiteralSize)
							.withUnicode(true);

			// create DISK HDT
			LargeFakeDataSetStreamSupplier.ThreadedStream genActual = supplier.createNTInputStream(CompressionType.NONE);
			HDT actual = null;
			try {
				actual = HDTManager.catTree(
						RDFFluxStop.sizeLimit(size),
						HDTSupplier.memory(),
						genActual.getStream(),
						HDTTestUtils.BASE_URI,
						RDFNotation.NTRIPLES,
						spec,
						quiet ? null : this
				);
			} finally {
				if (actual == null) {
					genActual.getThread().interrupt();
				}
			}
			genActual.getThread().joinAndCrashIfRequired();

			supplier.reset();

			Iterator<TripleString> genExpected = supplier.createTripleStringStream();
			// create MEMORY HDT
			HDT expected = HDTManager.generateHDT(
					genExpected,
					HDTTestUtils.BASE_URI,
					spec,
					null
			);

			// happy compiler, should throw before
			assertNotNull(expected);
			assertNotNull(actual);
			try {
				assertEqualsHDT(expected, actual); // -1 for the original size ignored by hdtcat
			} finally {
				IOUtil.closeAll(expected, actual);
			}
		}

		@Test
		public void catTreeDiskTest() throws IOException, ParserException, NotFoundException, InterruptedException {
			LargeFakeDataSetStreamSupplier supplier =
					LargeFakeDataSetStreamSupplier
							.createSupplierWithMaxSize(maxSize, SEED)
							.withMaxElementSplit(maxElementSplit)
							.withMaxLiteralSize(maxLiteralSize)
							.withUnicode(true);

			spec.set("debug.disk.build", true);

			// create DISK HDT
			LargeFakeDataSetStreamSupplier.ThreadedStream genActual = supplier.createNTInputStream(CompressionType.NONE);
			HDT actual = null;
			try {
				actual = HDTManager.catTree(
						RDFFluxStop.sizeLimit(size),
						HDTSupplier.disk(),
						genActual.getStream(),
						HDTTestUtils.BASE_URI,
						RDFNotation.NTRIPLES,
						spec,
						quiet ? null : this
				);
			} finally {
				if (actual == null) {
					genActual.getThread().interrupt();
				}
			}
			genActual.getThread().joinAndCrashIfRequired();

			supplier.reset();

			Iterator<TripleString> genExpected = supplier.createTripleStringStream();
			// create MEMORY HDT
			HDT expected = HDTManager.generateHDT(
					genExpected,
					HDTTestUtils.BASE_URI,
					spec,
					null
			);

			// happy compiler, should throw before
			assertNotNull(expected);
			assertNotNull(actual);
			try {
				assertEqualsHDT(expected, actual); // -1 for the original size ignored by hdtcat
			} finally {
				IOUtil.closeAll(expected, actual);
			}
		}
	}

	@RunWith(Parameterized.class)
	public static class DynamicCatTreeTest extends HDTManagerTestBase {

		@Parameterized.Parameters(name = "{5} - {0} kcat: {8}(async:{9})")
		public static Collection<Object[]> params() {
			List<Object[]> params = new ArrayList<>();
			for (String[] dict : diskDict()) {
				for (boolean async : new boolean[]{false, true}) {
					for (long kcat : new long[]{2, 10, 0}) {
						params.add(new Object[]{"base", SIZE_VALUE * 16, 20, 50, false, dict[0], dict[1], SIZE_VALUE, kcat, async});
						params.add(new Object[]{"duplicates", SIZE_VALUE * 16, 10, 50, false, dict[0], dict[1], SIZE_VALUE, kcat, async});
						params.add(new Object[]{"large-literals", SIZE_VALUE * 4, 20, 250, false, dict[0], dict[1], SIZE_VALUE, kcat, async});
						params.add(new Object[]{"quiet", SIZE_VALUE * 16, 10, 50, false, dict[0], dict[1], SIZE_VALUE, kcat, async});
					}
				}
			}
			return params;
		}

		@Parameterized.Parameter
		public String name;
		@Parameterized.Parameter(1)
		public long maxSize;
		@Parameterized.Parameter(2)
		public int maxElementSplit;
		@Parameterized.Parameter(3)
		public int maxLiteralSize;
		@Parameterized.Parameter(4)
		public boolean quiet;
		@Parameterized.Parameter(5)
		public String dictionaryType;
		@Parameterized.Parameter(6)
		public String tempDictionaryType;
		@Parameterized.Parameter(7)
		public long size;
		@Parameterized.Parameter(8)
		public long kCat;
		@Parameterized.Parameter(9)
		public boolean async;


		@Before
		public void setupSpecs() {
			spec.set(HDTOptionsKeys.DICTIONARY_TYPE_KEY, dictionaryType);
			spec.set(HDTOptionsKeys.TEMP_DICTIONARY_IMPL_KEY, tempDictionaryType);

			if (kCat != 0) {
				spec.set(HDTOptionsKeys.LOADER_CATTREE_KCAT, kCat);
			}
			if (async) {
				spec.set(HDTOptionsKeys.LOADER_CATTREE_ASYNC_KEY, true);
			}
		}

		@Test
		public void catTreeTest() throws IOException, ParserException, NotFoundException, InterruptedException {
			LargeFakeDataSetStreamSupplier supplier =
					LargeFakeDataSetStreamSupplier
							.createSupplierWithMaxSize(maxSize, SEED)
							.withMaxElementSplit(maxElementSplit)
							.withMaxLiteralSize(maxLiteralSize)
							.withUnicode(true);

			// create DISK HDT
			LargeFakeDataSetStreamSupplier.ThreadedStream genActual = supplier.createNTInputStream(CompressionType.NONE);
			HDT actual = null;
			HDT expected = null;
			try {
				try {
					actual = HDTManager.catTree(
							RDFFluxStop.sizeLimit(size),
							HDTSupplier.memory(),
							genActual.getStream(),
							HDTTestUtils.BASE_URI,
							RDFNotation.NTRIPLES,
							spec,
							quiet ? null : this
					);
				} finally {
					if (actual == null) {
						genActual.getThread().interrupt();
					}
				}
				genActual.getThread().joinAndCrashIfRequired();

				supplier.reset();

				Iterator<TripleString> genExpected = supplier.createTripleStringStream();
				// create MEMORY HDT
				expected = HDTManager.generateHDT(
						genExpected,
						HDTTestUtils.BASE_URI,
						spec,
						null
				);

				// happy compiler, should throw before
				assertNotNull(expected);
				assertNotNull(actual);
				assertEqualsHDT(expected, actual); // -1 for the original size ignored by hdtcat
			} finally {
				IOUtil.closeAll(expected, actual);
			}
		}

		@Test
		public void catTreeDiskTest() throws IOException, ParserException, NotFoundException, InterruptedException {
			LargeFakeDataSetStreamSupplier supplier =
					LargeFakeDataSetStreamSupplier
							.createSupplierWithMaxSize(maxSize, SEED)
							.withMaxElementSplit(maxElementSplit)
							.withMaxLiteralSize(maxLiteralSize)
							.withUnicode(true);

			// create DISK HDT
			LargeFakeDataSetStreamSupplier.ThreadedStream genActual = supplier.createNTInputStream(CompressionType.NONE);
			HDT actual = null;
			try {
				actual = HDTManager.catTree(
						RDFFluxStop.sizeLimit(size),
						HDTSupplier.disk(),
						genActual.getStream(),
						HDTTestUtils.BASE_URI,
						RDFNotation.NTRIPLES,
						spec,
						quiet ? null : this
				);
			} finally {
				if (actual == null) {
					genActual.getThread().interrupt();
				}
			}
			genActual.getThread().joinAndCrashIfRequired();

			supplier.reset();

			Iterator<TripleString> genExpected = supplier.createTripleStringStream();
			// create MEMORY HDT
			HDT expected = HDTManager.generateHDT(
					genExpected,
					HDTTestUtils.BASE_URI,
					spec,
					null
			);

			// happy compiler, should throw before
			assertNotNull(expected);
			assertNotNull(actual);
			try {
				checkHDTConsistency(expected);
				checkHDTConsistency(actual);
				assertEqualsHDT(expected, actual); // -1 for the original size ignored by hdtcat
			} finally {
				IOUtil.closeAll(expected, actual);
			}
		}
	}

	@RunWith(Parameterized.class)
	public static class FileDynamicTest extends HDTManagerTestBase {
		@Parameterized.Parameters(name = "{0}")
		public static Collection<Object[]> params() {
			return List.of(
					new Object[]{"hdtGenDisk/unicode_disk_encode.nt", true, SIZE_VALUE},
					new Object[]{"unicodeTest.nt", true, SIZE_VALUE}
			);
		}

		@Parameterized.Parameter
		public String file;
		@Parameterized.Parameter(1)
		public boolean quiet;
		@Parameterized.Parameter(2)
		public long size;


		private void generateDiskTest() throws IOException, ParserException, NotFoundException {
			String ntFile = Objects.requireNonNull(getClass().getClassLoader().getResource(file), "Can't find " + file).getFile();
			// create DISK HDT
			try (HDT actual = HDTManager.generateHDTDisk(
					ntFile,
					HDTTestUtils.BASE_URI,
					RDFNotation.NTRIPLES,
					spec,
					quiet ? null : this
			)) {
				// create MEMORY HDT
				try (HDT expected = HDTManager.generateHDT(
						ntFile,
						HDTTestUtils.BASE_URI,
						RDFNotation.NTRIPLES,
						spec,
						null
				)) {
					checkHDTConsistency(actual);
					checkHDTConsistency(expected);
					assertEqualsHDT(expected, actual);
				}
			}
		}

		@Test
		public void generateDiskCompleteTest() throws IOException, ParserException, NotFoundException {
			spec.set(HDTOptionsKeys.LOADER_DISK_COMPRESSION_MODE_KEY, CompressionResult.COMPRESSION_MODE_COMPLETE);
			spec.set(HDTOptionsKeys.LOADER_DISK_CHUNK_SIZE_KEY, size);
			spec.set("debug.disk.build", true);
			generateDiskTest();
		}

		@Test
		public void generateDiskPartialTest() throws IOException, ParserException, NotFoundException {
			spec.set(HDTOptionsKeys.LOADER_DISK_COMPRESSION_MODE_KEY, CompressionResult.COMPRESSION_MODE_PARTIAL);
			spec.set(HDTOptionsKeys.LOADER_DISK_CHUNK_SIZE_KEY, size);
			spec.set("debug.disk.build", true);
			generateDiskTest();
		}

		@Test
		public void generateDiskCompleteMapTest() throws IOException, ParserException, NotFoundException {
			spec.set(HDTOptionsKeys.LOADER_DISK_COMPRESSION_MODE_KEY, CompressionResult.COMPRESSION_MODE_COMPLETE);
			spec.set(HDTOptionsKeys.LOADER_DISK_CHUNK_SIZE_KEY, size);
			File mapHDT = tempDir.newFile("mapHDTTest.hdt");
			spec.set(HDTOptionsKeys.LOADER_DISK_FUTURE_HDT_LOCATION_KEY, mapHDT.getAbsolutePath());
			spec.set("debug.disk.build", true);
			generateDiskTest();
			Files.deleteIfExists(mapHDT.toPath());
		}

		@Test
		public void generateDiskPartialMapTest() throws IOException, ParserException, NotFoundException {
			spec.set(HDTOptionsKeys.LOADER_DISK_COMPRESSION_MODE_KEY, CompressionResult.COMPRESSION_MODE_PARTIAL);
			spec.set(HDTOptionsKeys.LOADER_DISK_CHUNK_SIZE_KEY, size);
			File mapHDT = tempDir.newFile("mapHDTTest.hdt");
			spec.set(HDTOptionsKeys.LOADER_DISK_FUTURE_HDT_LOCATION_KEY, mapHDT.getAbsolutePath());
			generateDiskTest();
			Files.deleteIfExists(mapHDT.toPath());
		}

		@Test
		public void generateTest() throws IOException, ParserException, NotFoundException {
			String ntFile = Objects.requireNonNull(getClass().getClassLoader().getResource(file), "Can't find " + file).getFile();
			// create DISK HDT
			try (InputStream in = IOUtil.getFileInputStream(ntFile)) {
				try (PipedCopyIterator<TripleString> it = RDFParserFactory.readAsIterator(
						RDFParserFactory.getParserCallback(RDFNotation.NTRIPLES, HDTOptions.of(
								Map.of(HDTOptionsKeys.NT_SIMPLE_PARSER_KEY, "true")
						)),
						in, HDTTestUtils.BASE_URI, true, RDFNotation.NTRIPLES
				)) {
					try (HDT expected = HDTManager.generateHDT(
							it,
							HDTTestUtils.BASE_URI,
							spec,
							quiet ? null : this
					)) {
						String testCopy = tempDir.newFile().getAbsolutePath();
						expected.saveToHDT(testCopy, null);

						// create MEMORY HDT
						try (HDT actual = HDTManager.loadHDT(testCopy)) {
							assertEqualsHDT(expected, actual);
						}
					}
				}
			}
		}
	}

	public static class StaticTest extends HDTManagerTestBase {
		@Test
		public void multiSectionTest() throws ParserException, IOException, NotFoundException {
			Path root = tempDir.getRoot().toPath();
			Path hdtFile = root.resolve("testhdt.hdt");
			LargeFakeDataSetStreamSupplier supplier = LargeFakeDataSetStreamSupplier
					.createSupplierWithMaxTriples(10_000, 32)
					.withMaxLiteralSize(30)
					.withUnicode(true);

			// set MultiSectionDictionary type
			spec.set(HDTOptionsKeys.TEMP_DICTIONARY_IMPL_KEY, HDTOptionsKeys.TEMP_DICTIONARY_IMPL_VALUE_MULT_HASH);
			spec.set(HDTOptionsKeys.DICTIONARY_TYPE_KEY, HDTOptionsKeys.DICTIONARY_TYPE_VALUE_MULTI_OBJECTS);

			try (HDT hdt = HDTManager.generateHDT(
					supplier.createTripleStringStream(),
					HDTTestUtils.BASE_URI, spec,
					null
			)) {
				assertTrue(hdt.getDictionary() instanceof MultipleBaseDictionary);
				String testHdt = hdtFile.toString();
				hdt.saveToHDT(
						testHdt,
						null
				);

				// test mapping
				try (HDT hdt2 = HDTManager.mapHDT(testHdt)) {
					assertTrue(hdt2.getDictionary() instanceof MultipleBaseDictionary);
					assertEqualsHDT(hdt, hdt2);
				}
				// test loading
				try (HDT hdt2 = HDTManager.loadHDT(testHdt)) {
					assertTrue(hdt2.getDictionary() instanceof MultipleBaseDictionary);
					assertEqualsHDT(hdt, hdt2);
				}
			} finally {
				Files.deleteIfExists(hdtFile);
			}
			Path fakeNt = root.resolve("fake.nt");
			try {
				supplier.createNTFile(fakeNt);
				try (HDT hdt = HDTManager.generateHDT(
						fakeNt.toString(),
						HDTTestUtils.BASE_URI,
						RDFNotation.NTRIPLES,
						spec,
						null
				)) {
					String testHdt = hdtFile.toString();

					hdt.saveToHDT(testHdt, null);

					// test mapping
					try (HDT hdt2 = HDTManager.mapHDT(testHdt)) {
						assertTrue(hdt2.getDictionary() instanceof MultipleBaseDictionary);
						assertEqualsHDT(hdt, hdt2);
					}
					// test loading
					try (HDT hdt2 = HDTManager.loadHDT(testHdt)) {
						assertTrue(hdt2.getDictionary() instanceof MultipleBaseDictionary);
						assertEqualsHDT(hdt, hdt2);
					}
				}
			} finally {
				try {
					Files.deleteIfExists(fakeNt);
				} finally {
					Files.deleteIfExists(hdtFile);
				}
			}
		}

		@Test
		public void diffMultiSectTest() throws ParserException, IOException, NotFoundException {
			Path root = tempDir.getRoot().toPath();
			Path hdtFile = root.resolve("testhdt.hdt");
			Path diffLocation = root.resolve("diff");
			Files.createDirectories(diffLocation);
			LargeFakeDataSetStreamSupplier supplier = LargeFakeDataSetStreamSupplier
					.createSupplierWithMaxTriples(10_000, 32)
					.withMaxLiteralSize(30)
					.withUnicode(true);

			// set MultiSectionDictionary type
			spec.set(HDTOptionsKeys.TEMP_DICTIONARY_IMPL_KEY, "multHash");
			spec.set(HDTOptionsKeys.DICTIONARY_TYPE_KEY, "dictionaryMultiObj");

			try (HDT hdt = HDTManager.generateHDT(
					supplier.createTripleStringStream(),
					HDTTestUtils.BASE_URI, spec,
					null
			)) {
				assertTrue(hdt.getDictionary() instanceof MultipleBaseDictionary);
				String testHdt = hdtFile.toString();
				hdt.saveToHDT(
						testHdt,
						null
				);

				ModifiableBitmap bitmap;

				// test mapping
				long n;
				try (HDT hdt2 = HDTManager.mapHDT(testHdt)) {
					bitmap = BitmapFactory.createRWBitmap(hdt2.getTriples().getNumberOfElements());
					assertTrue(hdt2.getDictionary() instanceof MultipleBaseDictionary);
					assertEqualsHDT(hdt, hdt2);

					n = hdt2.getTriples().getNumberOfElements();
				}

				Random rnd = new Random(SEED);
				for (long i = 0; i < n / 24; i++) {
					bitmap.set(Math.abs(rnd.nextLong()) % n, true);
				}

				try (HDT hdtDiff = HDTManager.diffHDTBit(
						diffLocation.toAbsolutePath().toString(),
						testHdt,
						bitmap,
						spec,
						null
				)) {
					assertEquals(hdt.getTriples().getNumberOfElements() - bitmap.countOnes(), hdtDiff.getTriples().getNumberOfElements());
				}
			} finally {
				try {
					Files.deleteIfExists(hdtFile);
				} finally {
					PathUtils.deleteDirectory(diffLocation);
				}
			}
		}
	}

	@RunWith(Parameterized.class)
	public static class HDTQTest extends HDTManagerTestBase {
		@Parameterized.Parameters(name = "default graph:{0}")
		public static Collection<Object> params() {
			return List.of(true, false);
		}

		@Parameterized.Parameter
		public boolean useDefaultGraph;

		private LargeFakeDataSetStreamSupplier createSupplier() {
			// fake data generation
			return LargeFakeDataSetStreamSupplier
					.createSupplierWithMaxTriples(10000, 42)
					.withNoDefaultGraph(!useDefaultGraph)
					.withQuads(true);
		}

		private void hdtqTesd(LargeFakeDataSetStreamSupplier supplier, Path d) throws NotFoundException, IOException {
			// run test
			Comparator<CharSequence> csc = CharSequenceComparator.getInstance();
			try (HDT h = HDTManager.mapIndexedHDT(d)) {
				Path indexFile = d.resolveSibling(d.getFileName() + HDTVersion.get_index_suffix("-"));
				assertTrue("can't find " + indexFile, Files.exists(indexFile));
				supplier.reset();
				Iterator<TripleString> it = supplier.createTripleStringStream();
				Set<TripleString> dataset = new HashSet<>();
				while (it.hasNext()) {
					dataset.add(it.next().tripleToString());
				}

				supplier.reset();
				long count = 0;
				for (TripleString ts : (Iterable<TripleString>)supplier::createTripleStringStream) {
					count++;
					TripleString tsstr = ts.tripleToString();
					assertTrue("can't find " + tsstr, dataset.contains(tsstr));
					CharSequence graph = ts.getGraph();
					if (graph.length() == 0) {
						IteratorTripleString it2 = h.search(ts.getSubject(), ts.getPredicate(), ts.getObject());

						// search until we have no graph
						while (true) {
							assertTrue(it2.hasNext());
							TripleString ts2 = it2.next();
							if (ts2.getGraph().length() == 0) {
								assertEquals(ts, ts2);
								break;
							}
						}
					} else {
						IteratorTripleString it2 = h.search(ts.getSubject(), ts.getPredicate(), ts.getObject(), graph);
						assertTrue(it2.hasNext());
						TripleString ts2 = it2.next();
						assertEquals(ts, ts2);
						assertFalse(it2.hasNext());

						// empty search to check wildcard
						IteratorTripleString it3 = h.search(ts.getSubject(), ts.getPredicate(), ts.getObject(), "");
						while (true) {
							assertTrue(it3.hasNext());
							TripleString ts3 = it3.next();
							if (csc.compare(ts3.getGraph(), graph) == 0) {
								assertEquals(ts, ts3);
								break;
							}
						}
					}
				}

				assertEquals(dataset.size(), count);

				{
					IteratorTripleString itSearch = h.search("", "", "", "");
					long count2 = 0;
					while (itSearch.hasNext()) {
						count2++;
						TripleString ts = itSearch.next();
						TripleString tsstr = ts.tripleToString();
						assertTrue("can't find " + tsstr, dataset.contains(tsstr));

					}
					assertEquals(dataset.size(), count2);
				}

				// FOQ INDEX TEST

				StringBuilder roleDesc = new StringBuilder();
				for (TripleComponentRole role : TripleComponentRole.values()) {
					Set<TripleString> dataset2 = new HashSet<>(dataset);
					roleDesc.append(",").append(role);

					Iterator<? extends CharSequence> roleIt;
					switch (role) {
						case OBJECT: {
							Iterator<? extends CharSequence> sh = h.getDictionary().getShared().getSortedEntries();
							Iterator<? extends CharSequence> ob = h.getDictionary().getObjects().getSortedEntries();
							roleIt = new FetcherIterator<>() {
								@Override
								protected CharSequence getNext() {
									if (sh.hasNext()) {
										return sh.next();
									}
									if (ob.hasNext()) {
										return ob.next();
									}
									return null;
								}
							};
						}
						break;
						case SUBJECT: {
							Iterator<? extends CharSequence> sh = h.getDictionary().getShared().getSortedEntries();
							Iterator<? extends CharSequence> su = h.getDictionary().getSubjects().getSortedEntries();
							roleIt = new FetcherIterator<>() {
								@Override
								protected CharSequence getNext() {
									if (sh.hasNext()) {
										return sh.next();
									}
									if (su.hasNext()) {
										return su.next();
									}
									return null;
								}
							};
						}
						break;
						case PREDICATE:
							roleIt = h.getDictionary().getPredicates().getSortedEntries();
							break;
						case GRAPH:
							roleIt = h.getDictionary().getGraphs().getSortedEntries();
							break;
						default:
							throw new AssertionError();
					}

					long componentId = 0;
					Set<String> components = new HashSet<>();
					while (roleIt.hasNext()) {
						CharSequence component = roleIt.next();
						String str = component.toString();
						components.add(component.toString());
						long cid = componentId++;

						Iterator<TripleString> eid;
						switch (role) {
							case OBJECT:
								eid = h.search("", "", component, "");
								break;
							case SUBJECT:
								eid = h.search(component, "", "", "");
								break;
							case PREDICATE:
								eid = h.search("", component, "", "");
								break;
							case GRAPH:
								eid = h.search("", "", "", component);
								break;
							default:
								throw new AssertionError();
						}

						while (eid.hasNext()) {
							TripleString tsstr = eid.next().tripleToString();
							if (role == TripleComponentRole.GRAPH && !tsstr.getGraph().equals(str)) {
								// the default graph "" is searching all the graphs, so we need
								// to check that we are using the right one.
								continue;
							}
							if (!dataset2.remove(tsstr)) {
								BitmapTriplesIteratorPositionTest.printIterator(eid);
								fail("can't remove " + tsstr
										+ "\nfor " + role + "=" + component + "(" + cid + ")"
										+ "\ndone: " + roleDesc.substring(1)
										+ "\n" + String.join(",", components)
								);
							}
						}
					}
					assertTrue(dataset2.isEmpty());
				}


			}
		}

		@Test
		public void iteratorStreamGenerationTest() throws IOException, ParserException, NotFoundException {
			LargeFakeDataSetStreamSupplier supplier = createSupplier();
			Iterator<TripleString> it = supplier.createTripleStringStream();

			HDTOptions spec = HDTOptions.of(
					HDTOptionsKeys.TEMP_DICTIONARY_IMPL_KEY, HDTOptionsKeys.TEMP_DICTIONARY_IMPL_VALUE_HASH_QUAD,
					HDTOptionsKeys.DICTIONARY_TYPE_KEY, HDTOptionsKeys.DICTIONARY_TYPE_VALUE_FOUR_QUAD_SECTION
			);
			Path root = tempDir.newFolder().toPath();
			try {
				Path d = root.resolve("d.hdt");
				try (HDT hdt = HDTManager.generateHDT(it, HDTTestUtils.BASE_URI, spec, ProgressListener.ignore())) {
					hdt.saveToHDT(d.toAbsolutePath().toString(), ProgressListener.ignore());
				}
				hdtqTesd(supplier, d);
			} finally {
				PathUtils.deleteDirectory(root);
			}
		}

		@Test
		public void fileReadGenerationTest() throws IOException, ParserException, NotFoundException {
			LargeFakeDataSetStreamSupplier supplier = createSupplier();
			Iterator<TripleString> it = supplier.createTripleStringStream();

			HDTOptions spec = HDTOptions.of(
					HDTOptionsKeys.TEMP_DICTIONARY_IMPL_KEY, HDTOptionsKeys.TEMP_DICTIONARY_IMPL_VALUE_HASH_QUAD,
					HDTOptionsKeys.DICTIONARY_TYPE_KEY, HDTOptionsKeys.DICTIONARY_TYPE_VALUE_FOUR_QUAD_SECTION
			);
			Path root = tempDir.newFolder().toPath();
			try {
				Path nq = root.resolve("d.nq");
				try (BufferedWriter writer = Files.newBufferedWriter(nq)) {
					while (it.hasNext()) {
						it.next().dumpNtriple(writer);
					}
					writer.flush();
				}
				Path d = root.resolve("d.hdt");
				try (HDT hdt = HDTManager.generateHDT(nq.toAbsolutePath().toString(),
						HDTTestUtils.BASE_URI, RDFNotation.NQUAD, spec, ProgressListener.ignore())) {
					hdt.saveToHDT(d.toAbsolutePath().toString(), ProgressListener.ignore());
				}
				hdtqTesd(supplier, d);
			} finally {
				PathUtils.deleteDirectory(root);
			}
		}
	}

	@Ignore("handTests")
	public static class HandTest extends HDTManagerTestBase {
		@Test
		public void qzdqzdTest() throws ParserException, IOException {
			String path = "/Users/ate/workspace/qacompany/hdt-java-ate47/hdt-java-package/target/hdt-java-package-3.0.5-distribution/hdt-java-package-3.0.5/bin/shit.nt.gz";

			HDTSpecification spec = new HDTSpecification();
			spec.load("/Users/ate/workspace/qacompany/hdt-java-ate47/hdt-java-package/target/hdt-java-package-3.0.5-distribution/hdt-java-package-3.0.5/bin/option.hdtspec");


			try (HDT hdt = HDTManager.generateHDTDisk(path, "http://ex.ogr/#", spec,
					(level, message) -> System.out.println("[" + level + "] " + message)
			)) {
				System.out.println(hdt.getTriples().getNumberOfElements());
			}

		}

		@Test
		public void bigDiskTest() throws ParserException, IOException {
			LargeFakeDataSetStreamSupplier supplier = LargeFakeDataSetStreamSupplier
					.createSupplierWithMaxSize(100_000_000L, 94);

			Path output = tempDir.newFolder().toPath();

			HDTOptions spec = new HDTSpecification();
			spec.set(HDTOptionsKeys.LOADER_DISK_FUTURE_HDT_LOCATION_KEY, output.resolve("future.hdt").toAbsolutePath().toString());
			spec.set(HDTOptionsKeys.LOADER_DISK_LOCATION_KEY, output.resolve("gen_dir").toAbsolutePath().toString());
			spec.set(HDTOptionsKeys.NT_SIMPLE_PARSER_KEY, "true");
			spec.set(HDTOptionsKeys.PROFILER_KEY, "true");
			StopWatch watch = new StopWatch();
			watch.reset();
			try (HDT hdt = HDTManager.generateHDTDisk(supplier.createTripleStringStream(), "http://ex.ogr/#", spec,
					(level, message) -> System.out.println("[" + level + "] " + message)
			)) {
				System.out.println(watch.stopAndShow());
				System.out.println(hdt.getTriples().getNumberOfElements());
			}
		}

		@Test
		public void bigCatTreeDiskTest() throws ParserException, IOException {
			HDTOptions spec = new HDTSpecification();
			StopWatch watch = new StopWatch();
			spec.set(HDTOptionsKeys.LOADER_CATTREE_LOCATION_KEY, "C:\\WIKI\\CATTREE\\WORKING");
			spec.set(HDTOptionsKeys.LOADER_CATTREE_FUTURE_HDT_LOCATION_KEY, "C:\\WIKI\\CATTREE\\future.hdt");
			spec.set(HDTOptionsKeys.LOADER_DISK_LOCATION_KEY, "C:\\WIKI\\CATTREE\\WORKING_HDTDISK");
			spec.set(HDTOptionsKeys.LOADER_DISK_COMPRESSION_WORKER_KEY, "12");
			spec.set(HDTOptionsKeys.NT_SIMPLE_PARSER_KEY, "true");
			spec.set(HDTOptionsKeys.PROFILER_KEY, "true");
			watch.reset();
			try (HDT hdt = HDTManager.catTree(
					RDFFluxStop.sizeLimit(100_000_000_000L) // 300GB free
							.and(RDFFluxStop.countLimit(700_000_000L) // ~9GB maps
							), HDTSupplier.disk(),
					"M:\\WIKI\\latest-all.nt.bz2", HDTTestUtils.BASE_URI, RDFNotation.NTRIPLES, spec,
					(level, message) -> System.out.println("[" + level + "] " + message)
			)) {
				System.out.println(watch.stopAndShow());
				System.out.println(hdt.getTriples().getNumberOfElements());
			}
		}

		@Test
		public void bigGenCatTreeDiskTest() throws ParserException, IOException {
			LargeFakeDataSetStreamSupplier supplier = LargeFakeDataSetStreamSupplier
					.createSupplierWithMaxSize(10_000_000_000L, 94);
			HDTOptions spec = new HDTSpecification();
			StopWatch watch = new StopWatch();
			spec.set(HDTOptionsKeys.LOADER_CATTREE_LOCATION_KEY, "C:\\WIKI\\CATTREE\\WORKING");
			spec.set(HDTOptionsKeys.LOADER_CATTREE_FUTURE_HDT_LOCATION_KEY, "C:\\WIKI\\CATTREE\\future.hdt");
			spec.set(HDTOptionsKeys.LOADER_DISK_LOCATION_KEY, "C:\\WIKI\\CATTREE\\WORKING_HDTDISK");
			spec.set(HDTOptionsKeys.LOADER_DISK_COMPRESSION_WORKER_KEY, "12");
			spec.set(HDTOptionsKeys.NT_SIMPLE_PARSER_KEY, "true");
			spec.set(HDTOptionsKeys.PROFILER_KEY, "true");
			watch.reset();
			try (HDT hdt = HDTManager.catTree(
					RDFFluxStop.sizeLimit(100_000_000_000L) // 300GB free
							.and(RDFFluxStop.countLimit(700_000_000L) // ~9GB maps
							), HDTSupplier.disk(),
					supplier.createTripleStringStream(), HDTTestUtils.BASE_URI, spec,
					(level, message) -> System.out.println("[" + level + "] " + message)
			)) {
				System.out.println(watch.stopAndShow());
				System.out.println(hdt.getTriples().getNumberOfElements());
			}
		}
	}

}
