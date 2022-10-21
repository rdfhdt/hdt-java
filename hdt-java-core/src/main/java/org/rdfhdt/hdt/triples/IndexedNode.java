package org.rdfhdt.hdt.triples;

import org.rdfhdt.hdt.util.string.CharSequenceComparator;

import java.util.Comparator;


public class IndexedNode implements Comparable<IndexedNode> {
	private static final Comparator<CharSequence> NODE_COMPARATOR = CharSequenceComparator.getInstance();
	private CharSequence node;
	private long index;

	public IndexedNode(CharSequence node, long index) {
		this.node = node;
		this.index = index;
	}
	public IndexedNode() {
	}

	public CharSequence getNode() {
		return node;
	}

	public long getIndex() {
		return index;
	}

	public void setIndex(long index) {
		this.index = index;
	}

	public void setNode(CharSequence node) {
		this.node = node;
	}

	@Override
	public int compareTo(IndexedNode o) {
		return NODE_COMPARATOR.compare(node, o.getNode());
	}
}
