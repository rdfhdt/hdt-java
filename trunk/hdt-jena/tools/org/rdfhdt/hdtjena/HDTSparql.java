package org.rdfhdt.hdtjena;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;

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
	 * HDTSparql, receives a SPARQL SELECT query and executes it against an HDT file.
	 * @param args
	 */
	public static void main(String[] args) throws Throwable {
		if (args.length == 2) {
			String fileHDT = args[0];
			String sparqlQuery = args[1];

			// Create HDT
			HDT hdt = HDTManager.mapIndexedHDT(fileHDT, null);
			
			// Create Jena wrapper on top of HDT.
			HDTGraph graph = new HDTGraph(hdt);
			Model model = new ModelCom(graph);

			// Use Jena ARQ to execute the query.
			Query query = QueryFactory.create(sparqlQuery);
			QueryExecution qe = QueryExecutionFactory.create(query, model);
			ResultSet results = qe.execSelect();

			/*while(results.hasNext()) {
				QuerySolution sol = results.nextSolution();
				System.out.println(sol.toString());
			}*/
			// Output query results	
			ResultSetFormatter.outputAsCSV(System.out, results);
			
			// Close
			qe.close();
		} else {
			System.err.println("Must have two arguments, the HDT input file path and the SPARQL query string.");
			System.exit(1);
		}
	}
}
