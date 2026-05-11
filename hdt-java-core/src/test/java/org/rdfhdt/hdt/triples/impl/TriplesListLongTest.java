package org.rdfhdt.hdt.triples.impl;

import org.rdfhdt.hdt.options.HDTOptions;

public class TriplesListLongTest extends AbstractTriplesListTest<TriplesListLong> {

	@Override
	protected TriplesListLong newList(HDTOptions spec) {
		return new TriplesListLong(spec);
	}

	@Override
	protected boolean reallocateIfEmpty(TriplesListLong list, int n) {
		return list.reallocateIfEmpty(n);
	}
}
