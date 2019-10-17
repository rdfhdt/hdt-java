package org.rdfhdt.hdt.iterator.utils;

import java.util.Iterator;

public class RangeIterator implements Iterator<Long> {

	long pos;
	long max;
	
	public RangeIterator(int min, int max) {
		this.pos = min;
		this.max = max;
	}
	
	@Override
	public boolean hasNext() {
		return pos<max;
	}

	@Override
	public Long next() {
		return pos++;
	}

	@Override
	public void remove() {
		
	}

}
