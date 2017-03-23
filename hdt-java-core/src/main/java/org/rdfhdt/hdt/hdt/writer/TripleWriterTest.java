package org.rdfhdt.hdt.hdt.writer;

import java.io.IOException;

import org.rdfhdt.hdt.rdf.TripleWriter;
import org.rdfhdt.hdt.triples.TripleString;

public class TripleWriterTest {

	public static void main(String[] args) throws IOException {
//		TripleWriter writer = TripleWriterFactory.getWriter("out.nt.gz", true);
		TripleWriter writer = TripleWriterFactory.getWriter(System.out);
		
		TripleString ts = new TripleString("http://example.org/hello", "http://example.org/mypred", "\"Myvalue\"");
		
		writer.addTriple(ts);
		
		writer.close();
	}

}
