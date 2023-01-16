package org.rdfhdt.hdt.rdf.parsers;

import org.rdfhdt.hdt.rdf.RDFParserCallback;
import org.rdfhdt.hdt.triples.TripleString;

import java.util.HashSet;
import java.util.Set;

public class RDFContainer implements RDFParserCallback.RDFCallback {
    private final Set<TripleString> triples = new HashSet<>();


    @Override
    public void processTriple(TripleString triple, long pos) {
        // clone the triple
        triples.add(triple.tripleToString());
    }

    public Set<TripleString> getTriples() {
        return triples;
    }
}
