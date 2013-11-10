package org.rdfhdt.hdtjena.cmd;


import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdtjena.HDTGraph;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * 
 * @author mario.arias
 *
 */

public class HDTSparql {
	/**
	 * HDTSparql, receives a SPARQL SELECT query and executes it against an HDT file.
	 * @param args
	 */
	public static void main(String[] args) throws Throwable {
		if (args.length != 2) {
			System.err.println("Usage: hdtsparql <hdt input> <SPARQL Query>.");
			System.exit(1);
		}

		String fileHDT = args[0];
		String sparqlQuery = args[1];

		// Create HDT
		HDT hdt = HDTManager.mapIndexedHDT(fileHDT, null);

		try {
			// Create Jena wrapper on top of HDT.
			HDTGraph graph = new HDTGraph(hdt);
			Model model = ModelFactory.createModelForGraph(graph);

			// Use Jena ARQ to execute the query.
			Query query = QueryFactory.create(sparqlQuery);
			QueryExecution qe = QueryExecutionFactory.create(query, model);

			try {
				// FIXME: Do ASK/DESCRIBE/CONSTRUCT 
				ResultSet results = qe.execSelect();

				/*while(results.hasNext()) {
				QuerySolution sol = results.nextSolution();
				System.out.println(sol.toString());
				}*/
				// Output query results	
				ResultSetFormatter.outputAsCSV(System.out, results);
			} finally {
				qe.close();				
			}
		} finally {			
			// Close
			hdt.close();
		}
	}
}
