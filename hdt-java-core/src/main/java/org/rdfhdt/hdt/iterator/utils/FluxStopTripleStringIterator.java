package org.rdfhdt.hdt.iterator.utils;

import org.rdfhdt.hdt.rdf.RDFFluxStop;
import org.rdfhdt.hdt.triples.TripleString;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class FluxStopTripleStringIterator implements Iterator<TripleString> {
	private TripleString next;
	private final Iterator<TripleString> iterator;
	private final RDFFluxStop fluxStop;
	private boolean stop;

	public FluxStopTripleStringIterator(Iterator<TripleString> iterator, RDFFluxStop fluxStop) {
		this.iterator = iterator;
		this.fluxStop = fluxStop;
	}

	@Override
	public boolean hasNext() {
		if (stop) {
			return false;
		}
		if (next != null) {
			return true;
		}

		if (!iterator.hasNext()) {
			return false;
		}

		next = iterator.next();

		if (!fluxStop.canHandle(next)) {
			stop = true;
			return false;
		}

		return true;
	}

	/**
	 * @return if a new flux can be extracted, will restart the flux
	 */
	public boolean hasNextFlux() {
		stop = false;
		fluxStop.restart();
		return hasNext();
	}

	@Override
	public TripleString next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		try {
			return next;
		} finally {
			next = null;
		}
	}


}
