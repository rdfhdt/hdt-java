package org.rdfhdt.hdt.triples.impl;

import java.util.Iterator;
import java.util.Set;

import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;

/**
 * Iterator implementation to iterate over a TriplesList object.
 * Supports on next and hasNext
 * 
 * @author mario.arias
 *
 */
public class TriplesSetIterator implements IteratorTripleID {

	private Set<TripleID> triples;
	private TripleComponentOrder order;
	private long numElem;
	
	private Iterator<TripleID> iterator;

	public TriplesSetIterator(Set<TripleID> triples, TripleComponentOrder order, long numElem) {
		this.triples = triples;
		this.order = order;
		this.numElem = numElem;
		this.iterator = triples.iterator();
	}
	

	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public TripleID next() {
		return iterator.next();
	}

	/**
	 * Not implemented in this implementation of TempTriples because not available for TreeSet
	 */
	@Override
	public boolean hasPrevious() {
		throw new NotImplementedException();
	}

	/**
	 * Not implemented in this implementation of TempTriples because not available for TreeSet
	 */
	@Override
	public TripleID previous() {
		throw new NotImplementedException();
	}

	@Override
	public void goToStart() {
		iterator = triples.iterator();
	}

	@Override
	public long estimatedNumResults() {
		return numElem;
	}

	@Override
	public ResultEstimationType numResultEstimation() {
		return ResultEstimationType.EXACT;
	}

	@Override
	public boolean canGoTo() {
		return false;
	}

	/**
	 * Not implemented in this implementation of TempTriples because not available for TreeSet
	 */
	@Override
	public void goTo(long pos) {
		throw new NotImplementedException();
	}

	@Override
	public TripleComponentOrder getOrder() {
		return order;
	}

	@Override
	public void remove() {
		iterator.remove();
	}
}