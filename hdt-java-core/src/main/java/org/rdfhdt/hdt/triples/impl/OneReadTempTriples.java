package org.rdfhdt.hdt.triples.impl;

import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.dictionary.impl.DictionaryIDMapping;
import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.header.Header;
import org.rdfhdt.hdt.iterator.SuppliableIteratorTripleID;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInfo;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TempTriples;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.Triples;
import org.rdfhdt.hdt.util.io.CountInputStream;
import org.rdfhdt.hdt.util.io.compress.NoDuplicateTripleIDIterator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * {@link org.rdfhdt.hdt.triples.TempTriples} only readable once with the {@link #searchAll()} method with a predefined
 * order, trying to set another order will lead to an exception, trying to use any other method can lead to a
 * {@link org.rdfhdt.hdt.exceptions.NotImplementedException}.
 * @author Antoine Willerval
 */
public class OneReadTempTriples implements TempTriples {
	private IteratorTripleID iterator;
	private TripleComponentOrder order;

	public OneReadTempTriples(Iterator<TripleID> iterator, TripleComponentOrder order, long triples) {
		this.iterator = new SimpleIteratorTripleID(iterator, order, triples);
		this.order = order;
	}

	@Override
	public boolean insert(long subject, long predicate, long object) {
		throw new NotImplementedException();
	}

	@Override
	public boolean insert(long subject, long predicate, long object, long graph) {
		return this.insert(subject, predicate, object);
	}

	@Override
	public boolean insert(TripleID... triples) {
		throw new NotImplementedException();
	}

	@Override
	public boolean remove(TripleID... pattern) {
		throw new NotImplementedException();
	}

	@Override
	public void sort(ProgressListener listener) {
		// already sorted
	}

	@Override
	public void removeDuplicates(ProgressListener listener) {
		throw new NotImplementedException();
	}

	@Override
	public void setOrder(TripleComponentOrder order) {
		if (order != this.order) {
			throw new IllegalArgumentException("order asked by isn't the same as the set one!");
		}
	}

	@Override
	public void clear() {
		throw new NotImplementedException();
	}

	@Override
	public void load(Triples triples, ProgressListener listener) {
		throw new NotImplementedException();
	}

	@Override
	public void replaceAllIds(DictionaryIDMapping mapSubj, DictionaryIDMapping mapPred, DictionaryIDMapping mapObj) {
		throw new NotImplementedException();
	}
	@Override
	public void replaceAllIds(
		DictionaryIDMapping mapSubj,
		DictionaryIDMapping mapPred,
		DictionaryIDMapping mapObj,
		DictionaryIDMapping mapGraph
	) {
		throw new NotImplementedException();
	}

	@Override
	public void save(OutputStream output, ControlInfo ci, ProgressListener listener) throws IOException {
		throw new NotImplementedException();
	}

	@Override
	public SuppliableIteratorTripleID search(TripleID pattern) {
		throw new NotImplementedException();
	}

	@Override
	public void load(InputStream input, ControlInfo ci, ProgressListener listener) throws IOException {
		throw new NotImplementedException();
	}

	@Override
	public void mapFromFile(CountInputStream in, File f, ProgressListener listener) throws IOException {
		throw new NotImplementedException();
	}

	@Override
	public void generateIndex(ProgressListener listener, HDTOptions disk, Dictionary dictionary) {
		throw new NotImplementedException();
	}

	@Override
	public void loadIndex(InputStream input, ControlInfo ci, ProgressListener listener) {
		throw new NotImplementedException();
	}

	@Override
	public void mapIndex(CountInputStream input, File f, ControlInfo ci, ProgressListener listener) {
		throw new NotImplementedException();
	}

	@Override
	public void saveIndex(OutputStream output, ControlInfo ci, ProgressListener listener) {
		throw new NotImplementedException();
	}

	@Override
	public void load(TempTriples input, ProgressListener listener) {
		if (input instanceof OneReadTempTriples) {
			OneReadTempTriples input2 = (OneReadTempTriples) input;
			this.iterator = input2.iterator;
			this.order = input2.order;
		} else {
			throw new NotImplementedException();
		}
	}

	@Override
	public TripleComponentOrder getOrder() {
		return order;
	}

	@Override
	public IteratorTripleID searchAll() {
		return new NoDuplicateTripleIDIterator(iterator);
	}

	@Override
	public long getNumberOfElements() {
		return iterator.estimatedNumResults();
	}

	@Override
	public long size() {
		return iterator.estimatedNumResults();
	}

	@Override
	public void populateHeader(Header head, String rootNode) {
		throw new NotImplementedException();
	}

	@Override
	public String getType() {
		throw new NotImplementedException();
	}

	@Override
	public TripleID findTriple(long position) {
		throw new NotImplementedException();
	}

	@Override
	public void close() throws IOException {
		// nothing to do
	}

	private static class SimpleIteratorTripleID implements IteratorTripleID {
		private final Iterator<TripleID> it;
		private final TripleComponentOrder order;
		private final long tripleCount;

		public SimpleIteratorTripleID(Iterator<TripleID> it, TripleComponentOrder order, long tripleCount) {
			this.it = it;
			this.order = order;
			this.tripleCount = tripleCount;
		}

		@Override
		public boolean hasPrevious() {
			throw new NotImplementedException();
		}

		@Override
		public TripleID previous() {
			throw new NotImplementedException();
		}

		@Override
		public void goToStart() {
			throw new NotImplementedException();
		}

		@Override
		public boolean canGoTo() {
			throw new NotImplementedException();
		}

		@Override
		public void goTo(long pos) {
			throw new NotImplementedException();
		}

		@Override
		public long estimatedNumResults() {
			return tripleCount;
		}

		@Override
		public ResultEstimationType numResultEstimation() {
			return ResultEstimationType.UP_TO;
		}

		@Override
		public TripleComponentOrder getOrder() {
			return order;
		}

		@Override
		public long getLastTriplePosition() {
			return tripleCount;
		}

		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		@Override
		public TripleID next() {
			return it.next();
		}
	}
}
