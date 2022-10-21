package org.rdfhdt.hdt.hdt;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.dictionary.DictionarySection;
import org.rdfhdt.hdt.enums.CompressionType;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.impl.diskimport.CompressionResult;
import org.rdfhdt.hdt.iterator.utils.PipedCopyIterator;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTOptionsKeys;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.rdf.RDFFluxStop;
import org.rdfhdt.hdt.rdf.RDFParserFactory;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.triples.impl.utils.HDTTestUtils;
import org.rdfhdt.hdt.util.LargeFakeDataSetStreamSupplier;
import org.rdfhdt.hdt.util.StopWatch;
import org.rdfhdt.hdt.util.io.AbstractMapMemoryTest;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.io.compress.CompressTest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;

@RunWith(Suite.class)
@Suite.SuiteClasses({
		HDTManagerTest.DynamicDiskTest.class,
		HDTManagerTest.DynamicCatTreeTest.class,
		HDTManagerTest.StaticTest.class
})
public class HDTManagerTest {
	private static class HDTManagerTestBase extends AbstractMapMemoryTest implements ProgressListener {
		protected static final long SIZE = 1L << 16;
		@Rule
		public TemporaryFolder tempDir = new TemporaryFolder();
		protected HDTSpecification spec;

		@Before
		public void setupManager() throws IOException {
			spec = new HDTSpecification();
			spec.set("loader.disk.location", tempDir.newFolder().getAbsolutePath());
		}

		@Override
		public void notifyProgress(float level, String message) {
			//		System.out.println("[" + level + "] " + message);
		}

		protected void assertEqualsHDT(HDT expected, HDT actual) throws NotFoundException {
			assertEqualsHDT(expected, actual, 0);
		}
		protected void assertEqualsHDT(HDT expected, HDT actual, int ignoredHeader) throws NotFoundException {

			// test dictionary
			Dictionary ed = expected.getDictionary();
			Dictionary ad = actual.getDictionary();
			assertEqualsHDT("Subjects", ed.getSubjects(), ad.getSubjects());
			assertEqualsHDT("Predicates", ed.getPredicates(), ad.getPredicates());
			assertEqualsHDT("Objects", ed.getObjects(), ad.getObjects());
			assertEqualsHDT("Shared", ed.getShared(), ad.getShared());
			assertEquals(ed.getType(), ad.getType());

			// test triples
			IteratorTripleString actualIt = actual.search("", "", "");
			IteratorTripleString expectedIt = expected.search("", "", "");

			while (expectedIt.hasNext()) {
				assertTrue(actualIt.hasNext());

				TripleString expectedTriple = expectedIt.next();
				TripleString actualTriple = actualIt.next();
				assertEquals(expectedIt.getLastTriplePosition(), actualIt.getLastTriplePosition());
				assertEquals(expectedTriple, actualTriple);
			}
			assertFalse(actualIt.hasNext());

			// test header
			assertEquals(actual.getHeader().getBaseURI(), expected.getHeader().getBaseURI());
			if (expected.getHeader().getNumberOfElements() + ignoredHeader != actual.getHeader().getNumberOfElements()) {
				StringBuilder bld = new StringBuilder();

				bld.append("-------- Header excepted:");
				expected.getHeader().search(null, null, null).forEachRemaining(bld::append);
				bld.append("-------- Header actual:");
				actual.getHeader().search(null, null, null).forEachRemaining(bld::append);

				fail("Size of the header doesn't match " + bld + expected.getHeader().getNumberOfElements() + " + " + ignoredHeader + "!=" + actual.getHeader().getNumberOfElements());
			}
		}

		protected void assertEqualsHDT(String section, DictionarySection excepted, DictionarySection actual) {
			Iterator<? extends CharSequence> itEx = excepted.getSortedEntries();
			Iterator<? extends CharSequence> itAc = actual.getSortedEntries();

			while (itEx.hasNext()) {
				assertTrue(itAc.hasNext());
				CharSequence expectedTriple = itEx.next();
				CharSequence actualTriple = itAc.next();
				CompressTest.assertCharSequenceEquals(section + " section strings", expectedTriple, actualTriple);
			}
			assertFalse(itAc.hasNext());
			assertEquals(excepted.getNumberOfElements(), actual.getNumberOfElements());
		}
	}

	@RunWith(Parameterized.class)
	public static class DynamicDiskTest extends HDTManagerTestBase {

		@Parameterized.Parameters(name = "{0}")
		public static Collection<Object[]> params() {
			List<Object[]> params = new ArrayList<>();
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
							new Object[]{"base-w" + threads + "-" + mode, SIZE * 8, 20, 50, threads, mode, false},
							new Object[]{"duplicates-w" + threads + "-" + mode, SIZE * 8, 10, 50, threads, mode, false},
							new Object[]{"large-literals-w" + threads + "-" + mode, SIZE * 2, 20, 250, threads, mode, false},
							new Object[]{"quiet-w" + threads + "-" + mode, SIZE * 8, 10, 50, threads, mode, false}
					));
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

		@Before
		public void setupSpecs() {
			spec.set(HDTOptionsKeys.LOADER_DISK_COMPRESSION_WORKER_KEY, String.valueOf(threads));
			spec.set("loader.disk.compressMode", compressMode);
		}

		private void generateDiskTest() throws IOException, ParserException, NotFoundException, InterruptedException {
			LargeFakeDataSetStreamSupplier supplier =
					LargeFakeDataSetStreamSupplier
							.createSupplierWithMaxSize(maxSize, 34)
							.withMaxElementSplit(maxElementSplit)
							.withMaxLiteralSize(maxLiteralSize);

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
							.createSupplierWithMaxSize(maxSize, 34)
							.withMaxElementSplit(maxElementSplit)
							.withMaxLiteralSize(maxLiteralSize);

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
			spec.setInt("loader.disk.chunkSize", SIZE);
			generateDiskTest();
		}

		@Test
		public void generateDiskMapTest() throws IOException, ParserException, NotFoundException, InterruptedException {
			spec.setInt("loader.disk.chunkSize", SIZE);
			File mapHDT = tempDir.newFile("mapHDTTest.hdt");
			spec.set(HDTOptionsKeys.LOADER_DISK_FUTURE_HDT_LOCATION_KEY, mapHDT.getAbsolutePath());
			generateDiskTest();
			Files.deleteIfExists(mapHDT.toPath());
		}

		@Test
		public void catTreeTest() throws IOException, ParserException, NotFoundException, InterruptedException {
			LargeFakeDataSetStreamSupplier supplier =
					LargeFakeDataSetStreamSupplier
							.createSupplierWithMaxSize(maxSize, 34)
							.withMaxElementSplit(maxElementSplit)
							.withMaxLiteralSize(maxLiteralSize);

			// create DISK HDT
			LargeFakeDataSetStreamSupplier.ThreadedStream genActual = supplier.createNTInputStream(CompressionType.NONE);
			HDT actual = null;
			try {
				actual = HDTManager.catTree(
						RDFFluxStop.sizeLimit(SIZE),
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
				assertEqualsHDT(expected, actual, -1); // -1 for the original size ignored by hdtcat
			} finally {
				IOUtil.closeAll(expected, actual);
			}
		}
		@Test
		public void catTreeDiskTest() throws IOException, ParserException, NotFoundException, InterruptedException {
			LargeFakeDataSetStreamSupplier supplier =
					LargeFakeDataSetStreamSupplier
							.createSupplierWithMaxSize(maxSize, 34)
							.withMaxElementSplit(maxElementSplit)
							.withMaxLiteralSize(maxLiteralSize);

			// create DISK HDT
			LargeFakeDataSetStreamSupplier.ThreadedStream genActual = supplier.createNTInputStream(CompressionType.NONE);
			HDT actual = null;
			try {
				actual = HDTManager.catTree(
						RDFFluxStop.sizeLimit(SIZE),
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
				assertEqualsHDT(expected, actual, -1); // -1 for the original size ignored by hdtcat
			} finally {
				IOUtil.closeAll(expected, actual);
			}
		}
	}

	@RunWith(Parameterized.class)
	public static class DynamicCatTreeTest extends HDTManagerTestBase {

		@Parameterized.Parameters(name = "{0}")
		public static Collection<Object[]> params() {
			return List.of(
					new Object[]{"base", SIZE * 16, 20, 50, false},
					new Object[]{"duplicates", SIZE * 16, 10, 50, false},
					new Object[]{"large-literals", SIZE * 4, 20, 250, false},
					new Object[]{"quiet", SIZE * 16, 10, 50, false}
			);
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

		@Test
		public void catTreeTest() throws IOException, ParserException, NotFoundException, InterruptedException {
			LargeFakeDataSetStreamSupplier supplier =
					LargeFakeDataSetStreamSupplier
							.createSupplierWithMaxSize(maxSize, 34)
							.withMaxElementSplit(maxElementSplit)
							.withMaxLiteralSize(maxLiteralSize);

			// create DISK HDT
			LargeFakeDataSetStreamSupplier.ThreadedStream genActual = supplier.createNTInputStream(CompressionType.NONE);
			HDT actual = null;
			HDT expected = null;
			try {
				try {
					actual = HDTManager.catTree(
							RDFFluxStop.sizeLimit(SIZE),
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
				assertEqualsHDT(expected, actual, -1); // -1 for the original size ignored by hdtcat
			} finally {
				IOUtil.closeAll(expected, actual);
			}
		}
		@Test
		public void catTreeDiskTest() throws IOException, ParserException, NotFoundException, InterruptedException {
			LargeFakeDataSetStreamSupplier supplier =
					LargeFakeDataSetStreamSupplier
							.createSupplierWithMaxSize(maxSize, 34)
							.withMaxElementSplit(maxElementSplit)
							.withMaxLiteralSize(maxLiteralSize);

			// create DISK HDT
			LargeFakeDataSetStreamSupplier.ThreadedStream genActual = supplier.createNTInputStream(CompressionType.NONE);
			HDT actual = null;
			try {
				actual = HDTManager.catTree(
						RDFFluxStop.sizeLimit(SIZE),
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
				assertEqualsHDT(expected, actual, -1); // -1 for the original size ignored by hdtcat
			} finally {
				IOUtil.closeAll(expected, actual);
			}
		}
	}
	@RunWith(Parameterized.class)
	public static class StaticTest extends HDTManagerTestBase {
		@Parameterized.Parameters(name = "{0}")
		public static Collection<Object[]> params() {
			return List.<Object[]>of(
					new Object[]{"hdtGenDisk/unicode_disk_encode.nt", true}
			);
		}

		@Parameterized.Parameter
		public String file;
		@Parameterized.Parameter(1)
		public boolean quiet;


		private void generateDiskTest() throws IOException, ParserException, NotFoundException {
			String ntFile = Objects.requireNonNull(getClass().getClassLoader().getResource(file), "Can't find " + file).getFile();
			// create DISK HDT
			HDT actual = HDTManager.generateHDTDisk(
					ntFile,
					HDTTestUtils.BASE_URI,
					RDFNotation.NTRIPLES,
					spec,
					quiet ? null : this
			);

			// create MEMORY HDT
			HDT expected = HDTManager.generateHDT(
					ntFile,
					HDTTestUtils.BASE_URI,
					RDFNotation.NTRIPLES,
					spec,
					null
			);

			try {
				assertEqualsHDT(expected, actual);
			} finally {
				IOUtil.closeAll(expected, actual);
			}
		}

		@Test
		public void generateDiskCompleteTest() throws IOException, ParserException, NotFoundException {
			spec.set("loader.disk.compressMode", CompressionResult.COMPRESSION_MODE_COMPLETE);
			spec.setInt("loader.disk.chunkSize", SIZE);
			generateDiskTest();
		}

		@Test
		public void generateDiskPartialTest() throws IOException, ParserException, NotFoundException {
			spec.set("loader.disk.compressMode", CompressionResult.COMPRESSION_MODE_PARTIAL);
			spec.setInt("loader.disk.chunkSize", SIZE);
			generateDiskTest();
		}

		@Test
		public void generateDiskCompleteMapTest() throws IOException, ParserException, NotFoundException {
			spec.set("loader.disk.compressMode", CompressionResult.COMPRESSION_MODE_COMPLETE);
			spec.setInt("loader.disk.chunkSize", SIZE);
			File mapHDT = tempDir.newFile("mapHDTTest.hdt");
			spec.set(HDTOptionsKeys.LOADER_DISK_FUTURE_HDT_LOCATION_KEY, mapHDT.getAbsolutePath());
			generateDiskTest();
			Files.deleteIfExists(mapHDT.toPath());
		}

		@Test
		public void generateDiskPartialMapTest() throws IOException, ParserException, NotFoundException {
			spec.set("loader.disk.compressMode", CompressionResult.COMPRESSION_MODE_PARTIAL);
			spec.setInt("loader.disk.chunkSize", SIZE);
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
						RDFParserFactory.getParserCallback(RDFNotation.NTRIPLES, true),
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

	@Ignore("handTests")
	public static class HandTest extends HDTManagerTestBase {
		@Test
		public void bigDiskTest() throws ParserException, IOException {
			LargeFakeDataSetStreamSupplier supplier = LargeFakeDataSetStreamSupplier
					.createSupplierWithMaxSize(10_000_000_000L, 94);

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
