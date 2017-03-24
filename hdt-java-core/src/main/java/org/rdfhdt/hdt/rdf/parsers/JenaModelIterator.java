package org.rdfhdt.hdt.rdf.parsers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;

public class JenaModelIterator implements IteratorTripleString {
	private final Model model;
	private StmtIterator iterator;
	
	public JenaModelIterator(Model model) {
		this.model = model;
		this.iterator = model.listStatements();
	}
	
	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public TripleString next() {
        Statement stm = iterator.nextStatement();

        return new TripleString(
                JenaNodeFormatter.format(stm.getSubject()),
                JenaNodeFormatter.format(stm.getPredicate()),
                JenaNodeFormatter.format(stm.getObject()));
	}

	@Override
	public void goToStart() {
		this.iterator = model.listStatements();
	}

	@Override
	public long estimatedNumResults() {
		return model.size();
	}

	@Override
	public ResultEstimationType numResultEstimation() {
		return ResultEstimationType.MORE_THAN;
	}


	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
