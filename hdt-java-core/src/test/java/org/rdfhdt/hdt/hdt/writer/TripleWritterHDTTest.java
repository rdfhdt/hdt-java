package org.rdfhdt.hdt.hdt.writer;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.rdf.RDFParserCallback;
import org.rdfhdt.hdt.rdf.RDFParserFactory;
import org.rdfhdt.hdt.triples.TripleString;

public class TripleWritterHDTTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test() {
		try {
			File file = new File("out.hdt");
			file.deleteOnExit();
			
			final TripleWriterHDT wr = new TripleWriterHDT("http://example.org", new HDTSpecification(), file.toString(), false);
			
			RDFParserCallback pars = RDFParserFactory.getParserCallback(RDFNotation.NTRIPLES);
			pars.doParse("data/test.nt", "http://example.org", RDFNotation.NTRIPLES, new RDFParserCallback.RDFCallback() {	
				@Override
				public void processTriple(TripleString triple, long pos) {				
					try {
						wr.addTriple(triple);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			wr.close();
			
			HDT hdt = HDTManager.loadHDT(file.toString());
			assertEquals("Successfully loaded HDT with same number of triples", 10, hdt.getTriples().getNumberOfElements());
			
			// Delete temp file
			file.delete();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
