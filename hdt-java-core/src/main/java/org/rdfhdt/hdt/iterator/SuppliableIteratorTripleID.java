package org.rdfhdt.hdt.iterator;

import org.rdfhdt.hdt.triples.IteratorTripleID;

public interface SuppliableIteratorTripleID extends IteratorTripleID {

    /**
     * create a supplier for the position (if compute the position isn't made in O(1), the implementation
     * should override this method)
     * @return the supplier
     */
    default TriplePositionSupplier getLastTriplePositionSupplier() {
        return TriplePositionSupplier.of(getLastTriplePosition());
    }
}
