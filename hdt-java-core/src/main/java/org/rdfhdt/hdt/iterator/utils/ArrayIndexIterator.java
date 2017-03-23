package org.rdfhdt.hdt.iterator.utils;

import java.util.Iterator;

public class ArrayIndexIterator implements Iterator<Integer> {

	int pos = 0;
	int [] arr;
	
	public ArrayIndexIterator(int [] arr) {
		this.arr = arr;
	}
	
	@Override
	public boolean hasNext() {
		return pos<arr.length;
	}

	@Override
	public Integer next() {
		return arr[pos++];
	}

	@Override
	public void remove() {
		
	}

}
