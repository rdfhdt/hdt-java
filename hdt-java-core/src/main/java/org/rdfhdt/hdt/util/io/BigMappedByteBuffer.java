package org.rdfhdt.hdt.util.io;

import java.io.IOException;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BigMappedByteBuffer {
    static long maxBufferSize = Integer.MAX_VALUE;

    /**
     * create a BigMappedByteBuffer of multiple {@link FileChannel#map(FileChannel.MapMode, long, long)} call
     * @param ch the File channel
     * @param mode the mode
     * @param position the position in the file
     * @param size the size of the buffer, can be higher than {@link Integer#MAX_VALUE}
     * @return BigMappedByteBuffer
     * @throws IOException if we can't call {@link FileChannel#map(FileChannel.MapMode, long, long)}
     */
    public static BigMappedByteBuffer ofFileChannel(FileChannel ch, FileChannel.MapMode mode, long position, long size) throws IOException {
        int bufferCount = (int) ((size - 1) / maxBufferSize) + 1;
        BigMappedByteBuffer buffer = new BigMappedByteBuffer(new ArrayList<>());
        for (int i = 0; i < bufferCount; i++) {
            long mapSize;

            if (i == bufferCount - 1 && size % maxBufferSize != 0) {
                mapSize = size % maxBufferSize;
            } else {
                mapSize = maxBufferSize;
            }
            buffer.buffers.add(ch.map(mode, position + (long) i * maxBufferSize, mapSize));
        }
        return buffer;
    }

    private final List<ByteBuffer> buffers;

    /**
     * cat multiple buffers
     * @param buffers the buffers
     */
    private BigMappedByteBuffer(List<ByteBuffer> buffers) {
        this.buffers = buffers;
    }

    private BigMappedByteBuffer(BigMappedByteBuffer other, Function<ByteBuffer, ByteBuffer> map) {
        this(other.buffers.stream().map(map).collect(Collectors.toList()));
    }

    List<ByteBuffer> getBuffers() {
        return buffers;
    }

    /**
     * set the byte order of the buffer
     * @param order the order
     */
    public void order(ByteOrder order) {
        buffers.forEach(b -> b.order(order));
    }

    /**
     * @return the capacity of the big buffer
     */
    public long capacity() {
        return buffers.stream().mapToLong(ByteBuffer::capacity).sum();
    }

    private int getBufferOffset(long index) {
        return (int) (index % maxBufferSize);
    }

    private int getBufferIndex(long index) {
        return (int) (index / maxBufferSize);
    }

    /**
     * get a byte at a particular index
     * @param index the byte index
     * @return byte
     */
    public byte get(long index) {
        int buffer = getBufferIndex(index);
        int inBufferIndex = getBufferOffset(index);

        if (buffer < 0 || buffer >= buffers.size())
            throw new IndexOutOfBoundsException();

        return buffers.get(buffer).get(inBufferIndex);
    }

    /**
     * set the position of the buffer
     * @param position the position
     */
    public void position(long position) {
        int mid = getBufferIndex(position);
        for (int i = 0; i < mid; i++) {
            buffers.get(i).position((int) maxBufferSize);
        }
        buffers.get(mid).position(getBufferOffset(position));
        for (int i = mid + 1; i < buffers.size(); i++) {
            buffers.get(i).position(0);
        }
    }

    /**
     * @return the position
     */
    public long position() {
        long pos = 0;
        for (ByteBuffer b : buffers) {
            pos += b.position();
            if (b.hasRemaining())
                break;
        }
        return pos;
    }

    /**
     * @return duplicate the buffer
     */
    public BigMappedByteBuffer duplicate() {
        return new BigMappedByteBuffer(this, ByteBuffer::duplicate);
    }

    /**
     * @return if we have remaining byte(s) to read
     */
    public boolean hasRemaining() {
        return position() < capacity();
    }

    /**
     * @return a byte, update the position
     */
    public byte get() {
        return buffers.get(getBufferIndex(position())).get();
    }

    /**
     * rewind the buffer
     */
    public void rewind() {
        buffers.forEach(ByteBuffer::rewind);
    }

    /**
     * read a particular number of bytes in the buffer
     * @param dst the destination array
     * @param offset the offset in the offset
     * @param length the length to read
     */
    public void get(byte[] dst, int offset, int length) {
        final long position = position();
        int buffer1 = getBufferIndex(position);
        int buffer2 = getBufferIndex(position + length - 1);

        if (buffer1 == buffer2) {
            // all the bytes are in the same buffer
            ByteBuffer b = buffers.get(buffer1);
            b.get(dst, offset, length);
        } else {
            // we are using 2 buffers
            ByteBuffer b1 = buffers.get(buffer1);
            ByteBuffer b2 = buffers.get(buffer2);

            int toRead = b1.capacity() - getBufferOffset(position);

            b1.get(dst, offset, toRead);
            b2.get(dst, offset + toRead, length - toRead);
        }
    }
}
