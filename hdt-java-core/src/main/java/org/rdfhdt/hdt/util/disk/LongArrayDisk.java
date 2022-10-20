/**
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 3.0 of the License.
 * <p>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * <p>
 * Contacting the authors:
 * Dennis Diefenbach:         dennis.diefenbach@univ-st-etienne.fr
 * Jose Gimenez Garcia:       jose.gimenez.garcia@univ-st-etienne.fr
 */

package org.rdfhdt.hdt.util.disk;

import org.rdfhdt.hdt.util.io.CloseMappedByteBuffer;
import org.rdfhdt.hdt.util.io.IOUtil;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

//Implementing an array of longs that is backed up on disk. Following this: http://vanillajava.blogspot.fr/2011/12/using-memory-mapped-file-for-huge.html

public class LongArrayDisk implements Closeable, LongArray {
    private static final long MAPPING_SIZE = 1 << 30;
    private FileChannel channel;
    private CloseMappedByteBuffer[] mappings;
    private long size;
    private final Path location;

    public LongArrayDisk(String location, long size) {
        this(Path.of(location), size);
    }

    public LongArrayDisk(String location, long size, boolean overwrite) {
        this(Path.of(location), size, overwrite);
    }
    public LongArrayDisk(Path location, long size) {
        this(location, size, true);
    }


    public LongArrayDisk(Path location, long size, boolean overwrite) {
        this.location = location;
        try {
            this.size = size;
            this.channel = FileChannel.open(
                    location,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE
            );
            long sizeBits = getSizeBits();
            int blocks = (int) Math.ceil((double) sizeBits / MAPPING_SIZE);
            mappings = new CloseMappedByteBuffer[blocks];
            for (int block = 0; block < blocks; block++) {
                long sizeMapping;
                if (block + 1 == blocks && sizeBits % MAPPING_SIZE != 0) {
                    sizeMapping = Math.min(MAPPING_SIZE, sizeBits % MAPPING_SIZE);
                } else {
                    sizeMapping = MAPPING_SIZE;
                }
                mappings[block] = IOUtil.mapChannel(location.toAbsolutePath().toString(), channel, FileChannel.MapMode.READ_WRITE, block * MAPPING_SIZE, sizeMapping);
            }
            if (overwrite) {
                set0(0, this.size);
            }
        } catch (IOException e) {
            try {
                try {
                    if (mappings != null) {
                        IOUtil.closeAll(mappings);
                    }
                } finally {
                    if (channel != null) {
                        channel.close();
                    }
                }
            } catch (IOException e1) {
                e.addSuppressed(e1);
            }
            throw new RuntimeException("can't create LongArrayDisk!", e);
        }
    }

    /**
     * Allows the {@link RandomAccessFile} and the array of {@link MappedByteBuffer} held by the instance to be
     * garbage-collected.
     */
    @Override
    public void close() throws IOException {
        IOUtil.closeAll(mappings);
        channel.close();
        mappings = null;
        channel = null;
    }

    @Override
    public long get(long x) {
        long p = x * 8;
        int block = (int) (p / MAPPING_SIZE);
        int offset = (int) (p % MAPPING_SIZE);
        return mappings[block].getLong(offset);
    }

    public long getLong(long x) {
        return this.get(x);
    }

    @Override
    public void set(long index, long value) {
        if (index >= size || index < 0) {
            throw new IndexOutOfBoundsException();
        }
        long p = index * 8;
        int block = (int) (p / MAPPING_SIZE);
        int offset = (int) (p % MAPPING_SIZE);
        mappings[block].putLong(offset, value);
    }

    @Override
    public long length() {
        return size;
    }

    public void set0(long start, long end) {
        if (start >= end) {
            // TODO: trim the file after end
            return;
        }
        int startBlock = (int) (start / MAPPING_SIZE);
        int endBlock = (int) ((end - 1) / MAPPING_SIZE);

        // 0 buffer
        byte[] zeros = new byte[4096];

        int c = 0;
        int lastBlock = startBlock;
        for (int i = startBlock; i <= endBlock; i++) {
            CloseMappedByteBuffer mapping = mappings[i];
            int capacity = mapping.capacity();


            int startBuffer;
            int endBuffer;

            if (i == startBlock) {
                startBuffer = (int) (start % MAPPING_SIZE);
            } else {
                startBuffer = 0;
            }
            if (i == endBlock) {
                endBuffer = (int) (end % MAPPING_SIZE);
            } else {
                endBuffer = capacity;
            }

            int toWrite = (endBuffer - startBuffer) * 8;

            while (toWrite > 0) {
                mapping.position(startBuffer * 8);
                int w = Math.min(toWrite, zeros.length);

                mapping.put(zeros, 0, w);

                toWrite -= w;
                c += w;
                if (c > 10000000) {
                    c = 0;
                    for (int j = lastBlock; j < i; j++) {
                        mappings[j].force();
                    }
                    lastBlock = i;
                }
            }
        }
        for (int j = lastBlock; j <= endBlock; j++) {
            mappings[j].force();
        }
    }

    public void resize(long newSize) throws IOException {
        if (this.size == newSize) {
            return;
        }
        long oldSize = this.size;
        this.size = newSize;
        long sizeBit = getSizeBits();

        int blocks = (int) Math.ceil((double) sizeBit / MAPPING_SIZE);
        CloseMappedByteBuffer[] mappings = new CloseMappedByteBuffer[blocks];
        try {
            for (CloseMappedByteBuffer mapping : mappings) {
                if (mapping != null) {
                    mapping.force();
                }
            }
            IOUtil.closeAll(this.mappings);
            this.mappings = null;

            for (int block = 0; block < blocks; block++) {
                long sizeMapping;
                if (block + 1 == blocks && sizeBit % MAPPING_SIZE != 0) {
                    sizeMapping = Math.min(MAPPING_SIZE, sizeBit % MAPPING_SIZE);
                } else {
                    sizeMapping = MAPPING_SIZE;
                }
                mappings[block] = IOUtil.mapChannel(location.toAbsolutePath().toString(), channel, FileChannel.MapMode.READ_WRITE, block * MAPPING_SIZE, sizeMapping);
            }
            // close previous mapping
            this.mappings = mappings;

            set0(oldSize, newSize);
        } catch (Throwable e) {
            try {
                throw e;
            } finally {
                IOUtil.closeAll(mappings);
            }
        }
    }

    public long getSize() {
        return size;
    }

    public long getSizeBits() {
        return size * 8L;
    }

}

