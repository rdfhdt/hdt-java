package org.rdfhdt.hdt.hdt.impl.diskimport;

import org.rdfhdt.hdt.triples.TempTriples;

import java.io.Closeable;

/**
 * Result for the {@link org.rdfhdt.hdt.util.io.compress.MapCompressTripleMerger}
 * @author Antoine Willerval
 */
public interface TripleCompressionResult extends Closeable {
	/**
	 * @return a sorted iterator of subject
	 */
	TempTriples getTriples();
	/**
	 * @return the number of triples
	 */
	long getTripleCount();
}
