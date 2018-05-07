package org.rdfhdt.hdt.iterator.utils;

import java.util.Iterator;

public class ArrayIteratorCharSequence implements Iterator<CharSequence> {

	CharSequence [] arr;
	int pos, max;
	
	public ArrayIteratorCharSequence(CharSequence [] arr) {
		this.arr = arr;
		this.pos = 0;
		this.max = arr.length;
	}
	
	public ArrayIteratorCharSequence(CharSequence [] arr, int min, int max) {
		this.arr = arr;
		this.pos = min;
		this.max = Math.min(arr.length, max);
	}
	
	@Override
	public boolean hasNext() {
		return pos<max;
	}

	@Override
	public CharSequence next() {
		return arr[pos++];
	}

	@Override
	public void remove() {
		
	}

}
