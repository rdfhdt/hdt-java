package org.rdfhdt.hdt.iterator.utils;

import java.util.Iterator;

public class ArrayIteratorG<T> implements Iterator<T> {

	T [] arr;
	int pos, max;
	
	public ArrayIteratorG(T [] arr) {
		this.arr = arr;
		this.pos = 0;
		this.max = arr.length;
	}
	
	public ArrayIteratorG(T [] arr, int min, int max) {
		this.arr = arr;
		this.pos = min;
		this.max = Math.min(arr.length, max);
	}
	
	@Override
	public boolean hasNext() {
		return pos<max;
	}

	@Override
	public T next() {
		return arr[pos++];
	}

	@Override
	public void remove() {
		
	}

}
