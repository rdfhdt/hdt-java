package org.rdfhdt.hdt.triples.impl;

import org.apache.commons.io.file.PathUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rdfhdt.hdt.compact.bitmap.Bitmap;
import org.rdfhdt.hdt.compact.sequence.Sequence;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTOptionsKeys;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.Triples;
import org.rdfhdt.hdt.util.LargeFakeDataSetStreamSupplier;
import org.rdfhdt.hdt.util.io.AbstractMapMemoryTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BitmapTriplesTest extends AbstractMapMemoryTest {
	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	private HDT loadOrMap(Path path, HDTOptions spec, boolean map) throws IOException {
		String location = path.toAbsolutePath().toString();
		if (map) {
			return HDTManager.mapIndexedHDT(location, spec, null);
		} else {
			return HDTManager.loadIndexedHDT(location, null, spec);
		}
	}

	private void assertEqualsSeq(String name, Sequence seqE, Sequence seqA) {
		assertEquals(seqE.getNumberOfElements(), seqA.getNumberOfElements());

		for (long i = 0; i < seqE.getNumberOfElements(); i++) {
			long e = seqE.get(i);
			long a = seqA.get(i);

			assertEquals(name + " | longs #" + i + "/" + seqE.getNumberOfElements() + " aren't the same", e, a);
		}
	}

	private void assertEqualsBM(String name, Bitmap seqE, Bitmap seqA) {
		assertEquals(seqE.getNumBits(), seqA.getNumBits());

		for (long i = 0; i < seqE.getNumBits(); i++) {
			boolean e = seqE.access(i);
			boolean a = seqA.access(i);

			assertEquals(name + " | bits #" + i + "/" + seqE.getNumBits() + " aren't the same", e, a);
		}
	}

	private void assertBitmapTriplesEquals(BitmapTriples excepted, BitmapTriples actual) {
		// maps
		assertEqualsSeq("seqY", excepted.getSeqY(), actual.getSeqY());
		assertEqualsSeq("seqZ", excepted.getSeqZ(), actual.getSeqZ());
		assertEqualsBM("bitmapY", excepted.getBitmapY(), actual.getBitmapY());
		assertEqualsBM("bitmapZ", excepted.getBitmapZ(), actual.getBitmapZ());
		// INDEX
		assertEqualsBM("bitmapIndexZ", excepted.getBitmapIndex(), actual.getBitmapIndex());
		assertEqualsSeq("indexZ", excepted.getIndexZ(), actual.getIndexZ());
		assertEqualsSeq("predicateCount", excepted.getPredicateCount(), actual.getPredicateCount());
	}

	public void diskBitmapIndexTest(boolean map, boolean disk) throws IOException, ParserException {
		Path root = tempDir.newFolder().toPath();

		Path hdt1Path = root.resolve("hdt1.hdt");
		Path hdt2Path = root.resolve("hdt2.hdt");

		try {
			// create 1 one fake HDT
			try (HDT hdt = LargeFakeDataSetStreamSupplier
					.createSupplierWithMaxTriples(10_000L, 42)
					.createFakeHDT(new HDTSpecification())) {
				hdt.saveToHDT(hdt1Path.toAbsolutePath().toString(), null);
			}
			// copy this fake HDT to another file (faster than creating the same twice)
			Files.copy(hdt1Path, hdt2Path);

			// hdt1 = DISK, hdt2 = OLD IMPLEMENTATION
			HDTOptions optDisk = new HDTSpecification();
			HDTOptions optDefault = new HDTSpecification();

			// set config
			if (disk) {
				optDisk.set(HDTOptionsKeys.BITMAPTRIPLES_SEQUENCE_DISK, true);
				optDisk.set(HDTOptionsKeys.BITMAPTRIPLES_SEQUENCE_DISK_SUBINDEX, true);
				optDisk.set(HDTOptionsKeys.BITMAPTRIPLES_SEQUENCE_DISK_LOCATION, root.resolve("indexdir").toAbsolutePath());
			}

			try (
					ByteArrayOutputStream indexDisk = new ByteArrayOutputStream();
					ByteArrayOutputStream indexDefault = new ByteArrayOutputStream()
			) {

				// create the index for both HDTs
				try (HDT hdt = loadOrMap(hdt1Path, optDisk, map && disk)) {
					Triples triples = hdt.getTriples();

					assertTrue(triples instanceof BitmapTriples);

					BitmapTriples bitmapTriples = (BitmapTriples) triples;

					if (disk) {
						assertNotNull(bitmapTriples.diskSequenceLocation);
						assertTrue(bitmapTriples.diskSequence);
					}

					try (HDT hdt2 = loadOrMap(hdt2Path, optDefault, map)) {
						Triples triples2 = hdt2.getTriples();

						assertTrue(triples2 instanceof BitmapTriples);

						BitmapTriples bitmapTriples2 = (BitmapTriples) triples2;

						if (disk) {
							assertNull(bitmapTriples2.diskSequenceLocation);
							assertFalse(bitmapTriples2.diskSequence);
						}
						bitmapTriples2.saveIndex(indexDefault, new ControlInformation(), null);

						assertBitmapTriplesEquals(bitmapTriples2, bitmapTriples);
					}

					bitmapTriples.saveIndex(indexDisk, new ControlInformation(), null);
				}

				// read and compare indexes
				byte[] indexDiskArr = indexDisk.toByteArray();
				byte[] indexDefaultArr = indexDefault.toByteArray();

				assertArrayEquals("index not equals", indexDefaultArr, indexDiskArr);
			}
		} finally {
			PathUtils.deleteDirectory(root);
		}
	}

	@Test
	public void diskBitmapMapIndexedTest() throws IOException, ParserException {
		diskBitmapIndexTest(false, true);
	}

	@Test
	public void diskBitmapLoadIndexedTest() throws IOException, ParserException {
		diskBitmapIndexTest(false, true);
	}

	@Test
	public void memBitmapMapIndexedTest() throws IOException, ParserException {
		diskBitmapIndexTest(false, false);
	}

	@Test
	public void memBitmapLoadIndexedTest() throws IOException, ParserException {
		diskBitmapIndexTest(false, false);
	}
}
