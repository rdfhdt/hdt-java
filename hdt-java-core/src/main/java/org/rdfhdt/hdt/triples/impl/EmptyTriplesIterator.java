package org.rdfhdt.hdt.triples.impl;

import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;

import java.util.NoSuchElementException;

public class EmptyTriplesIterator implements IteratorTripleID {
    private final TripleComponentOrder order;

    public EmptyTriplesIterator(TripleComponentOrder order) {
        this.order = order;
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public TripleID next() {
        throw new NoSuchElementException();
    }

    @Override
    public boolean hasPrevious() {
        return false;
    }

    @Override
    public TripleID previous() {
        throw new NoSuchElementException();
    }

    @Override
    public void goToStart() {
        // Do nothing
    }

    @Override
    public boolean canGoTo() {
        return false;
    }

    @Override
    public void goTo(long pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long estimatedNumResults() {
        return 0;
    }

    @Override
    public ResultEstimationType numResultEstimation() {
        return ResultEstimationType.EXACT;
    }

    @Override
    public TripleComponentOrder getOrder() {
        return order;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
