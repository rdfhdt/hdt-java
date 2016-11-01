package org.rdfhdt.hdt.rdf.parsers;

import org.rdfhdt.hdt.rdf.RDFParserCallback;

public class RDFParserSimpleTest extends AbstractNTriplesParserTest {

    @Override
    protected RDFParserCallback createParser() {
        return new RDFParserSimple();
    }
}
