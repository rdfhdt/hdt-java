package org.rdfhdt.hdtjena.solver;

import java.util.List;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphMapLink;
import org.apache.jena.sparql.core.DatasetGraphOne;
import org.apache.jena.sparql.core.PathBlock;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.Plan;
import org.apache.jena.sparql.engine.PlanOp;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.iterator.QueryIterYieldN;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.aggregate.AggCount;
import org.apache.jena.sparql.expr.aggregate.AggCountDistinct;
import org.apache.jena.sparql.expr.aggregate.AggCountVar;
import org.apache.jena.sparql.expr.aggregate.AggCountVarDistinct;
import org.apache.jena.sparql.expr.aggregate.Aggregator;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.util.Context;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdtjena.HDTGraph;

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
	
	private OptimizedCount() {}
	
	public static Plan getPlan(HDTQueryEngine engine, Query query, DatasetGraph dataset, Binding input, Context context) {
		if(query.getAggregators().size()!=1)
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
		if(triple==null) {
			return null;
		}
	
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
		} else if(dataset instanceof DatasetGraphMapLink) {
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
			ExprList exprList = ag.getExprList();
			if(exprList.size()!=1) {
				return null;
			}
			
			Expr expr = exprList.get(0);
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
			if(ag instanceof AggCountVar) {
				ExprList exprList = ag.getExprList();
				if(exprList.size()!=1) {
					return null;
				}
				
				Expr expr = exprList.get(0);
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
			} else if(!patternID.isNoMatch()) {
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
			} else {
				count=0;
			}
		}
	
		Binding bindingResult = new BindingOne( varOutput,  NodeFactory.createLiteral(Long.toString(count), XSDDatatype.XSDinteger) );
		return new PlanOp(new HDTOptimizedOp(), engine, new QueryIterYieldN(1, bindingResult));
	}
}
