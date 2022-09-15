package org.rdfhdt.hdt.hdtCat;

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
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.hdt.HDTSupplier;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.rdf.RDFFluxStop;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.triples.impl.utils.HDTTestUtils;
import org.rdfhdt.hdt.util.LargeFakeDataSetStreamSupplier;
import org.rdfhdt.hdt.util.StopWatch;
import org.rdfhdt.hdt.util.io.AbstractMapMemoryTest;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.string.CharSequenceComparator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(Suite.class)
@Suite.SuiteClasses({
		HDTCatTreeTest.DynamicTest.class
})
public class HDTCatTreeTest {
	private static class HDTManagerTestBase extends AbstractMapMemoryTest implements ProgressListener {
		protected static final long SIZE = 1L << 15;
		@Rule
		public TemporaryFolder tempDir = new TemporaryFolder();
		protected Path workDir;
		protected HDTSpecification spec;

		@Before
		public void setupManager() throws IOException {
			spec = new HDTSpecification();
			workDir = tempDir.newFolder().toPath();
			spec.set("loader.cattree.location", workDir.toAbsolutePath().toString());
		}

		@Override
		public void notifyProgress(float level, String message) {
			//		System.out.println("[" + level + "] " + message);
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
			assertEquals(expected.getHeader().getBaseURI(), actual.getHeader().getBaseURI());
			assertEquals(expected.getHeader().getNumberOfElements() + ignoredHeader, actual.getHeader().getNumberOfElements());
		}

		protected void assertEqualsHDT(String section, DictionarySection excepted, DictionarySection actual) {
			Iterator<? extends CharSequence> itEx = excepted.getSortedEntries();
			Iterator<? extends CharSequence> itAc = actual.getSortedEntries();
			Comparator<CharSequence> csc = CharSequenceComparator.getInstance();

			while (itEx.hasNext()) {
				assertTrue(itAc.hasNext());
				CharSequence expectedTriple = itEx.next();
				CharSequence actualTriple = itAc.next();
				assertEquals(section + " section strings", 0, csc.compare(expectedTriple, actualTriple));
			}
			assertFalse(itAc.hasNext());
			assertEquals(excepted.getNumberOfElements(), actual.getNumberOfElements());
		}
	}

	@RunWith(Parameterized.class)
	public static class DynamicTest extends HDTManagerTestBase {

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
			LargeFakeDataSetStreamSupplier.ThreadedStream genActual = supplier.createNTInputStream();
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

	}

	@Ignore("handTests")
	public static class HandTest extends HDTManagerTestBase {
		@Test
		public void bigTest() throws ParserException, IOException {
			LargeFakeDataSetStreamSupplier supplier = LargeFakeDataSetStreamSupplier
					.createSupplierWithMaxSize(10_000_000_000L, 94);

			HDTOptions spec = new HDTSpecification();
			StopWatch watch = new StopWatch();
			watch.reset();
			try (HDT hdt = HDTManager.catTree(RDFFluxStop.sizeLimit(1_000_000_000), HDTSupplier.memory(),
					supplier.createTripleStringStream(), HDTTestUtils.BASE_URI, spec,
					(level, message) -> System.out.println("[" + level + "] " + message)
			)) {
				System.out.println(watch.stopAndShow());
				System.out.println(hdt.getTriples().getNumberOfElements());
			}
		}
	}
}
