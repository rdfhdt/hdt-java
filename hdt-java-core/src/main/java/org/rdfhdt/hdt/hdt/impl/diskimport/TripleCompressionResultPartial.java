package org.rdfhdt.hdt.hdt.impl.diskimport;

import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.iterator.utils.ExceptionIterator;
import org.rdfhdt.hdt.triples.TempTriples;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.impl.OneReadTempTriples;
import org.rdfhdt.hdt.util.io.CloseSuppressPath;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.io.compress.CompressTripleMergeIterator;
import org.rdfhdt.hdt.util.io.compress.CompressTripleReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link org.rdfhdt.hdt.hdt.impl.diskimport.TripleCompressionResult} for partial file reading
 * @author Antoine Willerval
 */
public class TripleCompressionResultPartial implements TripleCompressionResult {
	private final List<CompressTripleReader> files;
	private final TempTriples triples;
	private final long tripleCount;
	private final TripleComponentOrder order;

	public TripleCompressionResultPartial(List<CloseSuppressPath> files, long tripleCount, TripleComponentOrder order, int bufferSize) throws IOException {
		this.files = new ArrayList<>(files.size());
		this.tripleCount = tripleCount;
		this.order = order;
		this.triples = new OneReadTempTriples(createBTree(files, 0, files.size(), bufferSize).asIterator(), order, tripleCount);
	}

	private ExceptionIterator<TripleID, IOException> createBTree(List<CloseSuppressPath> files, int start, int end, int bufferSize) throws IOException {
		int size = end - start;
		if (size <= 0) {
			return ExceptionIterator.empty();
		}
		if (size == 1) {
			CompressTripleReader r = new CompressTripleReader(files.get(start).openInputStream(bufferSize));
			this.files.add(r);
			return r;
		}
		int mid = (start + end) / 2;
		ExceptionIterator<TripleID, IOException> left = createBTree(files, start, mid, bufferSize);
		ExceptionIterator<TripleID, IOException> right = createBTree(files, mid, end, bufferSize);
		return new CompressTripleMergeIterator(left, right, order);
	}

	@Override
	public TempTriples getTriples() {
		return triples;
	}

	@Override
	public long getTripleCount() {
		return tripleCount;
	}

	@Override
	public void close() throws IOException {
		IOUtil.closeAll(files);
	}
}
