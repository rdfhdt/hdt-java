package org.rdfhdt.hdtjena.solver;

import java.util.List;

import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdtjena.HDTGraph;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.DatasetGraphMaker;
import com.hp.hpl.jena.sparql.core.DatasetGraphOne;
import com.hp.hpl.jena.sparql.core.PathBlock;
import com.hp.hpl.jena.sparql.core.TriplePath;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.Plan;
import com.hp.hpl.jena.sparql.engine.PlanOp;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterYieldN;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprVar;
import com.hp.hpl.jena.sparql.expr.aggregate.AggCount;
import com.hp.hpl.jena.sparql.expr.aggregate.AggCountDistinct;
import com.hp.hpl.jena.sparql.expr.aggregate.AggCountVar;
import com.hp.hpl.jena.sparql.expr.aggregate.AggCountVarDistinct;
import com.hp.hpl.jena.sparql.expr.aggregate.Aggregator;
import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.ElementNamedGraph;
import com.hp.hpl.jena.sparql.syntax.ElementPathBlock;
import com.hp.hpl.jena.sparql.util.Context;


/* 
 * Optimizes queries with count() and one triple pattern without filters.
 * They typically count number of appearances of triple patterns, dictionary entries, or total number of triples.
 * 
// Count dictionary entries
SELECT count(distinct ?s) { ?s ?p ?o }
SELECT count(distinct ?p) { ?s ?p ?o }
SELECT count(distinct ?o) { ?s ?p ?o }

// Count total triples
SELECT count(*) { ?s ?p ?o }
SELECT count(?s) { ?s ?p ?o }
SELECT count(?p) { ?s ?p ?o }
SELECT count(?o) { ?s ?p ?o }

// Count triple pattern
SELECT count(*) {  A ?p ?o }
SELECT count(?p) {  A ?p ?o }

// Forbidden cases (must be executed with full plan)
SELECT count(A) {  A ?p ?o }				// The counted variable must be one of the triple pattern.
SELECT count(?NO) {  A ?p ?o }				// Variable does not appear
SELECT count(*) {  A ?SAME ?SAME }			// Internal Join
SELECT count(*) { ?s ?p ?o FILTER(xxx) }	// Filters not allowed
SELECT count(distinct ?o) { A ?p ?o }		// Distinct only allowed for ? ? ?

*/
public class OptimizedCount {
	
	public static Plan getPlan(HDTQueryEngine engine, Query query, DatasetGraph dataset, Binding input, Context context) {
		if( (query.getAggregators().size()!=1) )
			return null;	
			
		// Must be count aggregator without "having" nor "group by"
		Aggregator ag = query.getAggregators().get(0).getAggregator();
		if(ag==null || !query.getHavingExprs().isEmpty() || !query.getGroupBy().isEmpty() || query.hasLimit() || query.hasOffset() || !( (ag instanceof AggCount) || (ag instanceof AggCountVar) || (ag instanceof AggCountDistinct) || (ag instanceof AggCountVarDistinct)) ) {
			return null;
		}
		
		// Must have one element only
		ElementGroup el = (ElementGroup) query.getQueryPattern();
		List<Element> list = el.getElements();
		if(list.size()!=1) {
			return null;
		}
		Element ele = list.get(0);
		
		// Extract graph <?g> { }
		Node graphName=null;
		if(ele instanceof ElementNamedGraph) {
			graphName = ((ElementNamedGraph) ele).getGraphNameNode();
			if(graphName.isVariable()) {
				return null;
			}
			
			ele = ((ElementNamedGraph) ele).getElement();
			
			if(ele instanceof ElementGroup) {
				ElementGroup elGroup = (ElementGroup) ele;
				if(elGroup.getElements().size()!=1) {
					return null;
				}
				ele = elGroup.getElements().get(0);
			}
		}
		
		// Must be a BGP
		if(!(ele instanceof ElementPathBlock)) {
			return null;
		}
		
		// With only one pattern
		PathBlock pb = ((ElementPathBlock) ele).getPattern();
		if(pb.size()!=1) {
			return null;
		}
		TriplePath tp = pb.get(0);
		Triple triple= tp.asTriple();
	
		// Every two components must not be equal to each other. (Forbid Joins)
		if(triple.getSubject().equals(triple.getPredicate()) ||
			triple.getPredicate().equals(triple.getObject()) ||
			triple.getSubject().equals(triple.getObject())   ) {
			return null;
		}
		
		// The output variable must be only one.
		if(query.getProjectVars().size()!=1) {
			return null;
		}
		Var varOutput= query.getProjectVars().get(0);

		// Extract selected graph from dataset.
		Graph g=null;
		if(dataset instanceof DatasetGraphOne ) {
			g = dataset.getDefaultGraph();
		} else if(dataset instanceof DatasetGraphMaker) {
			if(graphName!=null) {
				g = dataset.getGraph(graphName);
			} else {
				g = dataset.getDefaultGraph();
			}
		}
		if((g==null) || !(g instanceof HDTGraph)) {
			return null;
		}
		HDTGraph hdtg = (HDTGraph) g;
		
		long count;
		if(ag instanceof AggCountVarDistinct) {
			// SELECT count(distinct ?s) { ?s ?p ?o }
			// Count dictionary entries
			
			// Only one output var
			Expr expr = ag.getExpr();
			if(!(expr instanceof ExprVar)) {
				return null;
			}
			Var countVar = expr.asVar();
			
			// Only if triple pattern is ? ? ?
			if(!triple.getSubject().isVariable() || !triple.getPredicate().isVariable() || ! triple.getObject().isVariable()) {
				return null;
			}
			
			// Get number
			Dictionary dictionary = hdtg.getHDT().getDictionary();
			if(countVar.equals(triple.getSubject())) {
				count = dictionary.getNsubjects();
			} else if(countVar.equals(triple.getPredicate())) {
				count = dictionary.getNpredicates();
			} else if(countVar.equals(triple.getObject())) {
				count = dictionary.getNobjects();
			} else {
				// Output variable does not appear
				return null;
			}
		} else {
			// SELECT count(*) { <pattern> }
			// SELECT count(distinct *) { <pattern> }
			
			// SELECT count(?s) { ?s ?p ?o }
			// At least one variable must be the output
			if( (ag instanceof AggCountVar) ) {
				Expr expr = ag.getExpr();
				if(!(expr instanceof ExprVar)) {
					return null;
				}
				Var countVar = expr.asVar();
				if( !(triple.getSubject().equals(countVar) ||				
					triple.getPredicate().equals(countVar) || 
					triple.getObject().equals(countVar)) ) {
					return null;
				}
			}

			TripleID patternID = hdtg.getNodeDictionary().getTriplePatID(triple);
			if(patternID.isEmpty()) {
				// All results
				count = hdtg.getHDT().getTriples().getNumberOfElements();
			} else {
				// Search triple pattern
				IteratorTripleID it = hdtg.getHDT().getTriples().search(patternID);
				if(it.numResultEstimation()==ResultEstimationType.EXACT) {
					count = it.estimatedNumResults();
				} else {
					count = 0;
					while(it.hasNext()) {
						it.next();
						count++;
					}
				}
			}
		}
	
		Binding bindingResult = new BindingOne( varOutput,  NodeFactory.createLiteral(Long.toString(count)) );	
		return new PlanOp(new HDTOptimizeddOp(), engine, new QueryIterYieldN(1, bindingResult));
	}
}
