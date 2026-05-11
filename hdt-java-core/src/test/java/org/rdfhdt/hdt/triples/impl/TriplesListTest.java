package org.rdfhdt.hdt.triples.impl;

import org.rdfhdt.hdt.options.HDTOptions;

public class TriplesListTest extends AbstractTriplesListTest<TriplesList> {

	@Override
	protected TriplesList newList(HDTOptions spec) {
		return new TriplesList(spec);
	}

	@Override
	protected boolean reallocateIfEmpty(TriplesList list, int n) {
		return list.reallocateIfEmpty(n);
	}
}
