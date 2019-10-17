package org.rdfhdt.hdt.iterator.utils;

import java.util.Iterator;

public class RangeIteratorLong implements Iterator<Long> {
	long pos;
	long max;
	
	public RangeIteratorLong(long min, long max) {
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
