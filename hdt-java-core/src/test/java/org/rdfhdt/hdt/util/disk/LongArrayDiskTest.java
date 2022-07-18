package org.rdfhdt.hdt.util.disk;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rdfhdt.hdt.util.io.AbstractMapMemoryTest;
import org.rdfhdt.hdt.util.io.CloseMappedByteBuffer;
import org.rdfhdt.hdt.util.io.IOUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LongArrayDiskTest  extends AbstractMapMemoryTest {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    public static String printByteBuff(ByteBuffer buffer) {
        return IntStream.range(0, buffer.capacity())
                        .mapToObj(buffer::get)
                        .map(b -> String.format("%02x", b))
                        .collect(Collectors.joining(" "));
    }
    @Test
    public void testClose() throws IOException {
        Path testDir = tempDir.newFolder().toPath();

        Path arraydisk = testDir.resolve("arraydisk");
        int size = 6;

        try (LongArrayDisk array = new LongArrayDisk(arraydisk, size)) {
            for (int i = 0; i < size; i++) {
                array.set(i, i * 2);
                Assert.assertEquals("index #" + i, i * 2, array.get(i));
            }
        }

        String f1;

        try (FileChannel channel = FileChannel.open(arraydisk, StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocate(size * 8);
            channel.read(buffer);
            f1 = printByteBuff(buffer);
        }

        String f2;

        try (FileChannel channel = FileChannel.open(arraydisk, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
             CloseMappedByteBuffer closeMappedByteBuffer = IOUtil.mapChannel(arraydisk.toAbsolutePath().toString(), channel, FileChannel.MapMode.READ_WRITE, 0, size * 8)) {
            ByteBuffer map = closeMappedByteBuffer.getInternalBuffer();
            try {
                f2 = printByteBuff(map);
            } finally {
                IOUtil.cleanBuffer(map);
            }
        }
        Assert.assertEquals(f1, f2);

        try (LongArrayDisk array = new LongArrayDisk(arraydisk, size, false)) {
            for (int i = 0; i < size; i++) {
                Assert.assertEquals("index #" + i, i * 2, array.get(i));
            }
        }

        Assert.assertTrue(Files.exists(arraydisk));
        Files.delete(arraydisk);
        Assert.assertFalse(Files.exists(arraydisk));
    }

}