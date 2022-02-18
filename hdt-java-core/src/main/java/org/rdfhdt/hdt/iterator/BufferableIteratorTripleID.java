package org.rdfhdt.hdt.iterator;

import org.rdfhdt.hdt.triples.IteratorTripleID;

public interface BufferableIteratorTripleID extends IteratorTripleID {

    /**
     * @return the buffered triple position
     */
    default BufferedTriplePosition getBufferedLastTriplePosition() {
        return this::getLastTriplePosition;
    }
}
