package org.rdfhdt.hdt.rdf.parsers;

import org.junit.Test;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.rdf.RDFParserCallback.RDFCallback;
import org.rdfhdt.hdt.triples.TripleString;

public class TarParserTest implements RDFCallback {

	@Test
	public void test() throws ParserException {
		RDFParserTar parser = new RDFParserTar();
//		parser.doParse("/Users/mck/rdf/dataset/tgztest.tar.gz", "http://www.rdfhdt.org", RDFNotation.NTRIPLES, this);
	}

	@Override
	public void processTriple(TripleString triple, long pos) {

	}

}
