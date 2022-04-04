package org.rdfhdt.hdt.dictionary.impl.section;

import org.rdfhdt.hdt.dictionary.TempDictionarySection;
import org.rdfhdt.hdt.exceptions.NotImplementedException;

import java.io.IOException;
import java.util.Iterator;

public class OneReadDictionarySection implements TempDictionarySection {
	private final Iterator<? extends CharSequence> reader;
	private final long size;

	public OneReadDictionarySection(Iterator<? extends CharSequence> reader, long size) {
		this.reader = reader;
		this.size = size;
	}

	@Override
	public long add(CharSequence str) {
		throw new NotImplementedException();
	}

	@Override
	public void remove(CharSequence str) {
		throw new NotImplementedException();
	}

	@Override
	public void sort() {
		throw new NotImplementedException();
	}

	@Override
	public void clear() {
		throw new NotImplementedException();
	}

	@Override
	public boolean isSorted() {
		return true;
	}

	@Override
	public Iterator<? extends CharSequence> getEntries() {
		return reader;
	}

	@Override
	public long locate(CharSequence s) {
		throw new NotImplementedException();
	}

	@Override
	public CharSequence extract(long pos) {
		throw new NotImplementedException();
	}

	@Override
	public long size() {
		return size;
	}

	@Override
	public long getNumberOfElements() {
		return size;
	}

	@Override
	public Iterator<? extends CharSequence> getSortedEntries() {
		return reader;
	}

	@Override
	public void close() throws IOException {
	}
}
