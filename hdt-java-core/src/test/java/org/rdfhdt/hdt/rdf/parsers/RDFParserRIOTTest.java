package org.rdfhdt.hdt.rdf.parsers;

import org.rdfhdt.hdt.rdf.RDFParserCallback;

public class RDFParserRIOTTest extends AbstractNTriplesParserTest {

    @Override
    protected RDFParserCallback createParser() {
        return new RDFParserRIOT();
    }
}
