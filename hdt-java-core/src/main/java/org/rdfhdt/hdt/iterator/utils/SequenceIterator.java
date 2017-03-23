package org.rdfhdt.hdt.iterator.utils;

import java.util.Iterator;

import org.rdfhdt.hdt.compact.sequence.Sequence;

public class SequenceIterator implements Iterator<Long> {

	Sequence seq;
	long pos, max;
	
	public SequenceIterator(Sequence seq) {
		this.seq = seq;
		this.pos = 0;
		this.max = seq.getNumberOfElements();
	}
	
	public SequenceIterator(Sequence seq, long min, long max) {
		this.pos = min;
		this.max = Math.min(seq.getNumberOfElements(), max);
	}
	
	@Override
	public boolean hasNext() {
		return pos<max;
	}

	@Override
	public Long next() {
		return seq.get(pos++);
	}

	@Override
	public void remove() {
		
	}

}
