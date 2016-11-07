package org.rdfhdt.hdtjena.solver;

import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitor;
import org.apache.jena.sparql.algebra.Transform;
import org.apache.jena.sparql.algebra.op.Op0;
import org.apache.jena.sparql.util.NodeIsomorphismMap;

public class HDTOptimizedOp extends Op0 {

	@Override
	public void visit(OpVisitor opVisitor) {
//		opVisitor.visit(this); 
	}

	@Override
	public String getName() {
		return "hdtOptimizedOp";
	}

	@Override
	public Op apply(Transform transform) {
		return this;
//		return transform.transform(this);
	}

	@Override
	public Op0 copy() {
		return new HDTOptimizedOp();
	}

	@Override
	public int hashCode() {
		return 0xCC;
	}

	@Override
	public boolean equalTo(Op other, NodeIsomorphismMap labelMap) {
		if(other instanceof HDTOptimizedOp)
			return true;
		return false;
	}

}
