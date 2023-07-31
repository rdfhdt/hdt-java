package org.rdfhdt.hdt.quads.impl;

import org.rdfhdt.hdt.compact.bitmap.Bitmap;
import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.iterator.SuppliableIteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.impl.BitmapTriples;
import org.rdfhdt.hdt.triples.impl.BitmapTriplesIteratorYFOQ;

public class BitmapQuadsIteratorYGFOQ implements SuppliableIteratorTripleID {
	
	// resolves ?P?G queries
	private final Bitmap bitmapGraph; // the bitmap of the requested graph
	private final BitmapTriplesIteratorYFOQ inIt;
	private TripleID nextRes = null;
	public BitmapQuadsIteratorYGFOQ(BitmapTriples triples, TripleID pattern) {
		this.inIt = new BitmapTriplesIteratorYFOQ(
			triples,
			pattern
		);
		this.bitmapGraph = triples
			.getQuadInfoAG()
			.get((int) pattern.getGraph() - 1);
		this.goToStart();
		this.calculateNext();
	}

	private boolean isValidZ() {
		return this.inIt.posZ != -1
			&& this.bitmapGraph.access(this.inIt.posZ - 1);
	}

	@Override
	public void goToStart() {
		this.inIt.goToStart();
	}
	
	@Override
	public boolean hasNext() {
		return this.nextRes != null;
	}

	private void calculateNext() {
		this.nextRes = null;
		while (this.inIt.hasNext()) {
			TripleID next = this.inIt.next().clone();
			if (!this.isValidZ())
				continue;
			this.nextRes = next;
			break;
		}
	}

	@Override
	public TripleID next() {
		TripleID res = this.nextRes.clone();
		this.calculateNext();
		return res;
	}
	
	@Override
	public boolean hasPrevious() {
		return this.inIt.hasPrevious();
	}

	@Override
	public TripleID previous() {
		return this.inIt.previous();
	}

	@Override
	public boolean canGoTo() {
		return this.inIt.canGoTo();
	}

	@Override
	public void goTo(long pos) {
		this.inIt.goTo(pos);
	}

	@Override
	public long estimatedNumResults() {
		return this.inIt.estimatedNumResults();
	}

	@Override
	public ResultEstimationType numResultEstimation() {
		return this.inIt.numResultEstimation();
	}

	@Override
	public TripleComponentOrder getOrder() {
		return this.inIt.getOrder();
	}

	@Override
	public long getLastTriplePosition() {
		return this.inIt.getLastTriplePosition();
	}
}
