package org.rdfhdt.hdt.iterator.utils;

import org.rdfhdt.hdt.triples.TripleID;

import java.util.Iterator;

public class FileTripleIDIterator extends FileChunkIterator<TripleID> {
	/**
	 * create a file iterator from a stream and a max size
	 *
	 * @param it                 the iterator
	 * @param maxSize            the maximum size of each file, this size is estimated, so files can be bigger.
	 */
	public FileTripleIDIterator(Iterator<TripleID> it, long maxSize) {
		super(it, maxSize, tripleID -> 4L * Long.BYTES);
	}
}
