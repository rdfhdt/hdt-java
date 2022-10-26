package org.rdfhdt.hdt.rdf;

import java.io.IOException;

import org.rdfhdt.hdt.triples.TripleString;

public interface TripleWriter extends AutoCloseable {
	void addTriple(TripleString str) throws IOException;
}
