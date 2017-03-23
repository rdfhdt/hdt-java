package org.rdfhdt.hdt.rdf;

import java.io.IOException;

import org.rdfhdt.hdt.triples.TripleString;

public interface TripleWriter {

	public void addTriple(TripleString str) throws IOException;
	
	public void close() throws IOException;
}
