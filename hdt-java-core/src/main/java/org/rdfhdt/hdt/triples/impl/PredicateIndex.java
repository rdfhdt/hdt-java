package org.rdfhdt.hdt.triples.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.util.io.CountInputStream;

public interface PredicateIndex {
	public long getNumOcurrences(int pred);
	public long getBase(int pred);
	public long getOccurrence(long base, long occ);
	
	public void load(InputStream in) throws IOException;
	
	public void save(OutputStream in) throws IOException;
	
	public void mapIndex(CountInputStream input, File f, ProgressListener listener) throws IOException;
	
	void generate(ProgressListener listener);
	
	void close() throws IOException;
}
