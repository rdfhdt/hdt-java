package org.rdfhdt.hdt.hdt.writer;

import org.rdfhdt.hdt.rdf.TripleWriter;
import org.rdfhdt.hdt.triples.TripleString;

public class TripleWriterTest {

	public static void main(String[] args) throws Exception {
//		TripleWriter writer = TripleWriterFactory.getWriter("out.nt.gz", true);
		try(TripleWriter writer = TripleWriterFactory.getWriter(System.out)){
			TripleString ts = new TripleString("http://example.org/hello", "http://example.org/mypred", "\"Myvalue\"");

			writer.addTriple(ts);
		}
	}

}
