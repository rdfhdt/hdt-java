package org.rdfhdt.hdt.compact.bitmap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.roaringbitmap.longlong.Roaring64Bitmap;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.listener.ProgressListener;

public class Bitmap64Roaring implements ModifiableBitmap {
	private Roaring64Bitmap bitmap;
	private long numBits;
	public Bitmap64Roaring() {
		bitmap = new Roaring64Bitmap();
		numBits = 0;
	}
	@Override
	public boolean access(long position) {
		return bitmap.contains(position);
	}
	@Override
	public long rank1(long position) {
		return bitmap.rankLong(position);
	}
	@Override
	public long rank0(long position) {
		return position - rank1(position);
	}
	@Override
	public long selectPrev1(long start) {
		throw new NotImplementedException();
	}
	@Override
	public long selectNext1(long start) {
		throw new NotImplementedException();
	}
	@Override
	public long select0(long n) {
		throw new NotImplementedException();
	}
	@Override
	public long select1(long n) {
		return bitmap.select(n);
	}
	@Override
	public long getNumBits() {
		return numBits;
	}
	@Override
	public long countOnes() {
		return bitmap.getLongCardinality();
	}
	@Override
	public long countZeros() {
		return numBits - countOnes();
	}
	@Override
	public long getSizeBytes() {
		return bitmap.serializedSizeInBytes();
	}
	@Override
	public void save(OutputStream output, ProgressListener listener) throws IOException {
		long size = getSizeBytes();
		// 
		ByteBuffer b1 = ByteBuffer.allocate(Long.BYTES * 2);
		b1.putLong(numBits);
		b1.putLong(size);
		output.write(b1.array());
		// 
		ByteBuffer b2 = ByteBuffer.allocate((int)size);
		bitmap.serialize(b2);
		output.write(b2.array());
	}
	@Override
	public void load(InputStream input, ProgressListener listener) throws IOException {
		ByteBuffer b1 = ByteBuffer.allocate(Long.BYTES * 2);
		input.read(b1.array());
		numBits = b1.getLong();
		long size = b1.getLong();
		//
		ByteBuffer b2 = ByteBuffer.allocate((int)size);
		input.read(b2.array());
		bitmap.deserialize(b2);
	}
	@Override
	public String getType() {
		return HDTVocabulary.BITMAP_TYPE_PLAIN;
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
		numBits++;
		set(numBits - 1, value);
	}
}
