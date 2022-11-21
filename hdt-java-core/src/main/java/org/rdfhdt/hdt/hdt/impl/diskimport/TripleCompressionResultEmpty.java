package org.rdfhdt.hdt.hdt.impl.diskimport;

import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.triples.TempTriples;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.impl.OneReadTempTriples;

import java.io.IOException;
import java.util.Iterator;

public class TripleCompressionResultEmpty implements TripleCompressionResult {
    private final TripleComponentOrder order;

    public TripleCompressionResultEmpty(TripleComponentOrder order) {
        this.order = order;
    }

    @Override
    public TempTriples getTriples() {
        return new OneReadTempTriples(new Iterator<>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public TripleID next() {
                return null;
            }
        }, order, 0);
    }

    @Override
    public long getTripleCount() {
        return 0;
    }

    @Override
    public void close() throws IOException {

    }
}
