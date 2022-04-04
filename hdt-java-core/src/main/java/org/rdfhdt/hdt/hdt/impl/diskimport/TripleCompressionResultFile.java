package org.rdfhdt.hdt.hdt.impl.diskimport;

import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.triples.TempTriples;
import org.rdfhdt.hdt.triples.impl.OneReadTempTriples;
import org.rdfhdt.hdt.util.io.CloseSuppressPath;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.io.compress.CompressTripleReader;

import java.io.IOException;

/**
 * Implementation of {@link org.rdfhdt.hdt.hdt.impl.diskimport.TripleCompressionResult} for full file reading
 *
 * @author Antoine Willerval
 */
public class TripleCompressionResultFile implements TripleCompressionResult {
	private final long tripleCount;
	private final CompressTripleReader reader;
	private final TripleComponentOrder order;
	private final CloseSuppressPath triples;

	public TripleCompressionResultFile(long tripleCount, CloseSuppressPath triples, TripleComponentOrder order, int bufferSize) throws IOException {
		this.tripleCount = tripleCount;
		this.reader = new CompressTripleReader(triples.openInputStream(bufferSize));
		this.order = order;
		this.triples = triples;
	}

	@Override
	public TempTriples getTriples() {
		return new OneReadTempTriples(reader.asIterator(), order, tripleCount);
	}

	@Override
	public long getTripleCount() {
		return tripleCount;
	}

	@Override
	public void close() throws IOException {
		IOUtil.closeAll(reader, triples);
	}
}
