package org.rdfhdt.hdt.hdt.writer;

import java.io.IOException;
import java.io.OutputStream;

import org.rdfhdt.hdt.rdf.TripleWriter;

public class TripleWriterFactory {
	public static TripleWriter getWriter(String outFile, boolean compress) throws IOException {
		return new TripleWriterNtriples(outFile, compress);
	}
	
	public static TripleWriter getWriter(OutputStream out) {
		return new TripleWriterNtriples(out);
	}
}
