package org.rdfhdt.hdt.triples.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.util.io.CountInputStream;

public interface PredicateIndex {
	long getNumOcurrences(long pred);
	long getBase(long pred);
	long getOccurrence(long base, long occ);
	
	void load(InputStream in) throws IOException;
	
	void save(OutputStream in) throws IOException;
	
	void mapIndex(CountInputStream input, File f, ProgressListener listener) throws IOException;
	
	void generate(ProgressListener listener, HDTOptions spec);
	
	void close() throws IOException;
}
