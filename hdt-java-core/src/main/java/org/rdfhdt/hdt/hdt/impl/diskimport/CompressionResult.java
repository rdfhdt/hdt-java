package org.rdfhdt.hdt.hdt.impl.diskimport;

import org.rdfhdt.hdt.iterator.utils.ExceptionIterator;
import org.rdfhdt.hdt.options.HDTOptionsKeys;
import org.rdfhdt.hdt.triples.IndexedNode;

import java.io.Closeable;
import java.io.IOException;

/**
 * Result for the {@link org.rdfhdt.hdt.hdt.impl.diskimport.SectionCompressor}
 * @author Antoine Willerval
 */
public interface CompressionResult extends Closeable {
	/**
	 * partial mode for config
	 * @see org.rdfhdt.hdt.hdt.impl.diskimport.CompressionResultPartial
	 */
	String COMPRESSION_MODE_PARTIAL = HDTOptionsKeys.LOADER_DISK_COMPRESSION_MODE_VALUE_PARTIAL;
	/**
	 * complete mode for config
	 * @see org.rdfhdt.hdt.hdt.impl.diskimport.CompressionResultFile
	 */
	String COMPRESSION_MODE_COMPLETE = HDTOptionsKeys.LOADER_DISK_COMPRESSION_MODE_VALUE_COMPLETE;

	/**
	 * @return the number of triple
	 */
	long getTripleCount();
	/**
	 * @return a sorted iterator of subject
	 */
	ExceptionIterator<IndexedNode, IOException> getSubjects();
	/**
	 * @return a sorted iterator of predicates
	 */
	ExceptionIterator<IndexedNode, IOException> getPredicates();
	/**
	 * @return a sorted iterator of objects
	 */
	ExceptionIterator<IndexedNode, IOException> getObjects();
	/**
	 * @return the count of subjects
	 */
	long getSubjectsCount();
	/**
	 * @return the count of predicates
	 */
	long getPredicatesCount();
	/**
	 * @return the count of objects
	 */
	long getObjectsCount();
	/**
	 * @return the count of shared
	 */
	long getSharedCount();

	/**
	 * @return the size of the origin file
	 */
	long getRawSize();
	/**
	 * delete data associated with this result
	 */
	void delete() throws IOException;
}
