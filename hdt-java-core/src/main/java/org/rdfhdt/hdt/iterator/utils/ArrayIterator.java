package org.rdfhdt.hdt.iterator.utils;

import java.util.Iterator;

public class ArrayIterator implements Iterator<Integer> {

	int [] arr;
	int pos, max;
	
	public ArrayIterator(int [] arr) {
		this.arr = arr;
		this.pos = 0;
		this.max = arr.length;
	}
	
	public ArrayIterator(int [] arr, int min, int max) {
		this.arr = arr;
		this.pos = min;
		this.max = Math.min(arr.length, max);
	}
	
	@Override
	public boolean hasNext() {
		return pos<max;
	}

	@Override
	public Integer next() {
		return arr[pos++];
	}

	@Override
	public void remove() {
		
	}

}
