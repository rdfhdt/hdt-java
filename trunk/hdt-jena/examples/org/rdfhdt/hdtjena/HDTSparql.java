package org.rdfhdt.hdtjena;
import org.rdfhdt.hdt.hdt.HDTFactory;
import org.rdfhdt.hdt.hdt.QueryableHDT;
import org.rdfhdt.hdtjena.HDTGraph;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.impl.ModelCom;


public class HDTSparql {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Throwable {
		String fileHDT = "../hdt-java/data/test.hdt";
		String sparqlQuery = "SELECT ?s ?p ?o WHERE { ?s ?p ?o . } LIMIT 100";
	
		// Create HDT handler and Jena Model on top.
		QueryableHDT hdt = HDTFactory.createQueryableHDT();
		hdt.loadFromHDT(fileHDT, null);
		hdt.loadOrCreateIndex(null);
		HDTGraph graph = new HDTGraph(hdt);
		Model model = new ModelCom(graph);

		// Use Jena ARQ to execute the query.
		Query query = QueryFactory.create(sparqlQuery);
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		ResultSet results = qe.execSelect();

		// Output query results	
		ResultSetFormatter.out(System.out, results, query);

		// Close
		qe.close();
	}

}
