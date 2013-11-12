package org.rdfhdt.hdt.iterator.utils;

import java.util.Iterator;

public class ArrayIteratorLong implements Iterator<Long> {
	long [] arr;
	int pos, max;
	
	public ArrayIteratorLong(long [] arr) {
		this.arr = arr;
		this.pos = 0;
		this.max = arr.length;
	}
	
	public ArrayIteratorLong(long [] arr, long min, long max) {
		if(min>Integer.MAX_VALUE || max>Integer.MAX_VALUE){
			throw new RuntimeException("More than 2G Entries not supported in Java arrays.");
		}
		this.arr = arr;
		this.pos = (int) min;
		this.max = (int) Math.min(arr.length, max);
	}
	
	@Override
	public boolean hasNext() {
		return pos<max;
	}

	@Override
	public Long next() {
		return arr[pos++];
	}

	@Override
	public void remove() {
		
	}

}
