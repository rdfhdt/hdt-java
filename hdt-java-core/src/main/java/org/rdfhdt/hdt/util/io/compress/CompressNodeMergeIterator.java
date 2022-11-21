package org.rdfhdt.hdt.util.io.compress;

import org.rdfhdt.hdt.iterator.utils.ExceptionIterator;
import org.rdfhdt.hdt.iterator.utils.MergeExceptionIterator;
import org.rdfhdt.hdt.triples.IndexedNode;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Version of {@link org.rdfhdt.hdt.iterator.utils.MergeExceptionIterator} with {@link org.rdfhdt.hdt.triples.IndexedNode}
 * @author Antoine Willerval
 */
public class CompressNodeMergeIterator extends MergeExceptionIterator<IndexedNode, IOException> {

	public CompressNodeMergeIterator(ExceptionIterator<IndexedNode, IOException> in1, ExceptionIterator<IndexedNode, IOException> in2) {
		super(in1, in2, IndexedNode::compareTo);
	}

	public static <T extends ExceptionIterator<IndexedNode, IOException>> ExceptionIterator<IndexedNode, IOException> buildOfTree(
			T[] lst) {
		return buildOfTree(it -> it, IndexedNode::compareTo, lst, 0, lst.length);
	}
}
