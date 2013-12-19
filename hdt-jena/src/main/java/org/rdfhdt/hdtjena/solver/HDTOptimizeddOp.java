package org.rdfhdt.hdtjena.solver;

import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.OpVisitor;
import com.hp.hpl.jena.sparql.algebra.Transform;
import com.hp.hpl.jena.sparql.algebra.op.Op0;
import com.hp.hpl.jena.sparql.util.NodeIsomorphismMap;

public class HDTOptimizeddOp extends Op0 {

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
		return new HDTOptimizeddOp();
	}

	@Override
	public int hashCode() {
		return 0xCC;
	}

	@Override
	public boolean equalTo(Op other, NodeIsomorphismMap labelMap) {
		if(other instanceof HDTOptimizeddOp)
			return true;
		return false;
	}

}
