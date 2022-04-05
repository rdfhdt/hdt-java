package org.rdfhdt.hdt.util.io;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rdfhdt.hdt.compact.bitmap.BitmapFactory;
import org.rdfhdt.hdt.compact.bitmap.ModifiableBitmap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;

public class BigByteBufferTest {

	public static void assertArrayEquals(byte[] arr, int start, byte[] arr2, int start2, int length) {
		for (int i = 0; i < length; i++) {
			Assert.assertEquals("index diff " + i, arr[start + i], arr2[start2 + i]);
		}
	}

	private int oldSize;

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	@Before
	public void prepare() {
		// save the size if we want to update it
		oldSize = BigByteBuffer.maxBufferSize;
	}
	@After
	public void complete() {
		BigByteBuffer.maxBufferSize = oldSize;
	}

	@Test
	public void capacityBuffer() {
		long size = 1000;
		BigByteBuffer.maxBufferSize = (int) (size / 7);

		BigByteBuffer buffer = BigByteBuffer.allocate(size);
		Assert.assertEquals(8, buffer.getBuffers().size());
		Assert.assertEquals(size, buffer.size());
	}
	@Test
	@Ignore("large, should be run with at least 3G or ram -Xmx3G")
	public void capacityBufferLarge() {
		long size = Integer.MAX_VALUE * 12L / 10;
		System.out.println(Runtime.getRuntime().maxMemory());
		BigByteBuffer buffer = BigByteBuffer.allocate(size);
		Assert.assertEquals(2, buffer.getBuffers().size());
		Assert.assertEquals(size, buffer.size());
	}

	@Test
	public void get() {
		int size = 10000;
		BigByteBuffer.maxBufferSize = size / 7;

		BigByteBuffer buffer = BigByteBuffer.allocate(size);

		RandomEntryFluxSupplier supplier = new RandomEntryFluxSupplier(72);
		supplier.generate(size / 10, size, e -> buffer.set(e.index, e.value));
		supplier.reset();
		supplier.generate(size / 10, size, e -> Assert.assertEquals(e.value, buffer.get(e.index)));
	}
	@Test
	public void getArr() {
		int size = 10000;
		final byte[] real = new byte[size];
		BigByteBuffer.maxBufferSize = size * 2 / 3;

		BigByteBuffer buffer = BigByteBuffer.allocate(size);
		Assert.assertEquals(2, buffer.getBuffers().size());

		RandomEntryFluxSupplier supplier = new RandomEntryFluxSupplier(72);
		supplier.generate(size / 10, size, e -> {
			buffer.set(e.index, e.value);
			real[(int) e.index] = e.value;
		});

		byte[] test = new byte[size];
		buffer.get(test, 0, 0, size);

		Assert.assertArrayEquals(real, test);

		buffer.get(test, size / 2, 0, size / 2);

		assertArrayEquals(real, size / 2, test, 0, size / 2);

		buffer.get(test, size / 3, 0, size / 3);

		assertArrayEquals(real, size / 3, test, 0, size / 3);

		buffer.get(test, size * 2 / 3, 0, size / 3);

		assertArrayEquals(real, size * 2 / 3, test, 0, size / 3);
	}

	@Test
	public void readFileTest() throws IOException {
		final String rawFileName = Objects.requireNonNull(getClass().getClassLoader().getResource("dbpedia.hdt"), "can't find dbpedia hdt").getFile();

		Path path = Paths.get(rawFileName);

		long size = Files.size(path);

		BigByteBuffer.maxBufferSize = (int) (size * 2 / 3); // test with huge split

		BigByteBuffer buffer = BigByteBuffer.allocate(size);

		String file = Objects.requireNonNull(getClass().getClassLoader().getResource("dbpedia.hdt"), "Can't find dbpedia.hdt").getFile();

		try (InputStream stream = IOUtil.getFileInputStream(file)) {
			buffer.readStream(stream, 0, size);
		}

		byte[] real = Files.readAllBytes(Paths.get(file));
		byte[] test = new byte[(int) buffer.size()];

		int delta = (int) (size / 10);

		for (int i = 0; i < test.length; i += delta) {
			buffer.get(test, i, 0, test.length - i);
			assertArrayEquals(real, i, test, 0, test.length - i);
		}
	}
	@Test
	public void writeFileTest() throws IOException {
		int size = BigByteBuffer.BUFFER_SIZE * 10;
		BigByteBuffer.maxBufferSize = size * 2 / 3;

		BigByteBuffer buffer = BigByteBuffer.allocate(size);

		RandomEntryFluxSupplier supplier = new RandomEntryFluxSupplier(274);
		supplier.generate(size / 10, size, e -> buffer.set(e.index, e.value));
		supplier.reset();

		File f = tempDir.newFile();

		int deltaf = size / 10;
		for (int start = 0; start < size; start += deltaf) {
			try (OutputStream stream = new FileOutputStream(f)) {
				buffer.writeStream(stream, start, buffer.size() - start, null);
			}

			byte[] test = Files.readAllBytes(f.toPath());
			byte[] real = new byte[size];

			int delta = size / 10;

			for (int i = 0; i < buffer.size() - start; i += delta) {
				buffer.get(real, start + i, 0, (int) buffer.size() - start - i);
				assertArrayEquals(test, i, real, 0, (int) buffer.size() - start - i);
			}

			Files.deleteIfExists(f.toPath());
		}
	}


	private static class Entry {
		long index;
		byte value;

		public Entry(long index, byte value) {
			this.index = index;
			this.value = value;
		}
	}
	private static class RandomEntryFluxSupplier {
		private Random random;
		private final long seed;

		public RandomEntryFluxSupplier(long seed) {
			this.seed = seed;
			reset();
		}

		public void reset() {
			random = new Random(seed);
		}

		public void generate(long count, int max, Consumer<Entry> e) {
			ModifiableBitmap bitmap = BitmapFactory.createRWBitmap(max);
			for (long i = 0; i < count; i++) {
				long index = random.nextInt(max);
				byte value = (byte) (random.nextInt() & 255);
				if (bitmap.access(index)) {
					continue;
				}
				bitmap.set(index, true);
				e.accept(new Entry(index, value));
			}
		}
	}
}
