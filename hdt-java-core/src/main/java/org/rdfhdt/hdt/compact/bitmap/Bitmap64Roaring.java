package org.rdfhdt.hdt.compact.bitmap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.roaringbitmap.longlong.Roaring64Bitmap;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.util.io.IOUtil;

public class Bitmap64Roaring implements ModifiableBitmap {
	private Roaring64Bitmap bitmap;
	public Bitmap64Roaring() {
		bitmap = new Roaring64Bitmap();
	}
	@Override
	public boolean access(long position) {
		return bitmap.contains(position);
	}
	@Override
	public long rank1(long position) {
		if (position >= 0)
			return bitmap.rankLong(position);
		return 0;
	}
	@Override
	public long rank0(long position) {
		throw new NotImplementedException("rank0 method is not implemented in Roaring Bitmaps");
	}
	@Override
	public long selectPrev1(long start) {
		return select1(rank1(start));
	}
	@Override
	public long selectNext1(long start) {
		long pos = rank1(start - 1);
		if (pos < bitmap.getLongCardinality())
			return select1(pos + 1);
		return -1;

	}
	@Override
	public long select0(long n) {
		throw new NotImplementedException();
	}
	@Override
	public long select1(long n) {
		long position = n - 1;
		if (position == -1)
			return -1;
		if (position < bitmap.getLongCardinality()) {
			return bitmap.select(position);
		} else {
			return bitmap.select(bitmap.getLongCardinality() - 1) + 1;
		}
	}
	@Override
	public long getNumBits() {
		return 0;
	}
	@Override
	public long countOnes() {
		return bitmap.getLongCardinality();
	}
	@Override
	public long countZeros() {
		throw new NotImplementedException("rank0 method is not implemented in Roaring Bitmaps");
	}
	@Override
	public long getSizeBytes() {
		return bitmap.serializedSizeInBytes();
	}
	@Override
	public void save(OutputStream output, ProgressListener listener) throws IOException {
		long size = getSizeBytes();
		IOUtil.writeLong(output, size);
		ByteBuffer b2 = ByteBuffer.allocate((int)size);
		bitmap.serialize(b2);
		output.write(b2.array());
	}
	@Override
	public void load(InputStream input, ProgressListener listener) throws IOException {
		long size = IOUtil.readLong(input);
		ByteBuffer b2 = ByteBuffer.allocate((int)size);
		input.read(b2.array());
		bitmap.deserialize(b2);
	}
	@Override
	public String getType() {
		return HDTVocabulary.BITMAP_TYPE_ROARING;
	}
	@Override
	public void set(long position, boolean value) {
		if (value) {
			bitmap.addLong(position);
		} else {
			bitmap.removeLong(position);
		}
	}
	@Override
	public void append(boolean value) {
		throw new NotImplementedException("append method is not implemented in Roaring Bitmaps");
	}
	@Override
	public String toString() {
		return bitmap.toString();
	}
}
