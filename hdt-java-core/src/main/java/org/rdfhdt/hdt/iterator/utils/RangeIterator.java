package org.rdfhdt.hdt.iterator.utils;

import java.util.Iterator;

public class RangeIterator implements Iterator<Integer> {

	int pos;
	int max;
	
	public RangeIterator(int min, int max) {
		this.pos = min;
		this.max = max;
	}
	
	@Override
	public boolean hasNext() {
		return pos<max;
	}

	@Override
	public Integer next() {
		return pos++;
	}

	@Override
	public void remove() {
		
	}

}
