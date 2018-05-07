package org.rdfhdt.hdt.examples;

import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.rdf.TripleWriter;
import org.rdfhdt.hdt.triples.TripleString;

public class ExampleTripleWriter {
	public static void main(String[] args) throws Exception {
		// Create default Writer to a file using a baseURI
		try(TripleWriter writer = HDTManager.getHDTWriter("out.hdt","http://example.org", new HDTSpecification())){
			TripleString ts = new TripleString("http://example.org/hello", "http://example.org/mypred", "\"Myvalue\"");

			// Add triples
			writer.addTriple(ts);
		}
		// HDT Generated when the try-with-resources block automatically closes the writer
	}
}
