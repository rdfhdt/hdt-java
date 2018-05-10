package org.rdfhdt.hdtjena.solver;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdtjena.HDTGraph;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.engine.Plan;

public class OptimizedCountTest {
	
	HDTGraph graph;
	DatasetGraph dataset;
	

	@Before
	public void setUp() throws Exception {
		HDT hdt = HDTManager.generateHDT(new DummyIteratorTripleString(), "http://www.rdfhdt.org", new HDTSpecification(), null);
		graph =new HDTGraph(hdt, true);
		dataset = DatasetGraphFactory.wrap(graph);
	}

	private Plan getPlan(String sparql) {
		try {
			System.out.println(sparql);
			Query query = QueryFactory.create(sparql, Syntax.syntaxARQ);
		return OptimizedCount.getPlan(null, query, dataset, null, null);
		} catch (Throwable e) {
			System.out.println("ERR");
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	@Test
	public void testDictionary() {
		assertNotNull(getPlan("SELECT COUNT(distinct ?s) WHERE { ?s ?p ?o }"));
		assertNotNull(getPlan("SELECT COUNT(distinct ?p) { ?s ?p ?o }"));
		assertNotNull(getPlan("SELECT COUNT(distinct ?o) { ?s ?p ?o }"));
	}
	
	@Test
	public void testAllTriples() {
		assertNotNull(getPlan("SELECT COUNT(*) { ?s ?p ?o }"));
		assertNotNull(getPlan("SELECT COUNT(?s) { ?s ?p ?o }"));
		assertNotNull(getPlan("SELECT COUNT(?p) { ?s ?p ?o }"));
		assertNotNull(getPlan("SELECT COUNT(?o) { ?s ?p ?o }"));
	}
	
	@Test
	public void testTriplePattern() {
		assertNotNull(getPlan("SELECT COUNT(*) { <hello> ?p ?o }"));
	}
	
	@Test
	public void testGraph() {
		assertNotNull(getPlan("SELECT COUNT(*) { graph <hello> { ?s ?p ?o }}"));
	}
	
	@Test
	public void testWrong() {
		assertNull("not count", getPlan("SELECT * { ?s ?p ?o }"));
		assertNull("distinct + Triple Pattern",getPlan("SELECT COUNT(distinct ?p) { <hello> ?p ?o }"));
		assertNull("unused variable", getPlan("SELECT COUNT(distinct ?NO) { ?s ?p ?o }"));
		assertNull("triple pattern join", getPlan("SELECT COUNT(distinct ?o) { ?s ?SAME ?SAME }"));
		assertNull("filter", getPlan("SELECT COUNT(*) { ?s ?p ?o FILTER (isLiteral(?o))}"));
		assertNull("having", getPlan("SELECT COUNT(*) { ?s ?p ?o } HAVING(?o>0)"));
		assertNull("limit", getPlan("SELECT COUNT(*) { ?s ?p ?o } LIMIT 0"));
		assertNull("offset", getPlan("SELECT COUNT(*) { ?s ?p ?o } OFFSET 10"));
	}


	

	private class DummyIteratorTripleString implements IteratorTripleString {

		boolean used;
		
		@Override
		public boolean hasNext() {
			return !used;
		}

		@Override
		public TripleString next() {
			used=true;
			return new TripleString("<http://www.example.org/foo>", "<http://www.example.org/bar>", "\"test\"");
		}

		@Override
		public void remove() {
		}

		@Override
		public void goToStart() {
			used = false;
		}

		@Override
		public long estimatedNumResults() {
			return 1;
		}

		@Override
		public ResultEstimationType numResultEstimation() {
			return ResultEstimationType.EXACT;
		}
		
	}
}
