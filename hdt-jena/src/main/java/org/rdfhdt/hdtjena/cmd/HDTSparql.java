package org.rdfhdt.hdtjena.cmd;


import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdtjena.HDTGraph;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Lists;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.system.StreamOps;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;

/**
 * 
 * @author mario.arias
 *
 */

public class HDTSparql {
	@Parameter(description = "<HDT file> <SPARQL query>")
	public List<String> parameters = Lists.newArrayList();

	@Parameter(names="-stream", description="Stream CONSTRUCT query results, with a duplicate suppression window given as argument (0 disables streaming)")
	public int streamWindow = 0;

	public String fileHDT;
	public String sparqlQuery;

	public void execute() throws IOException {
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
				// Perform the query and output the results, depending on query type
				if (query.isSelectType()) {
					ResultSet results = qe.execSelect();
					ResultSetFormatter.outputAsCSV(System.out, results);
				} else if (query.isDescribeType()) {
					Model result = qe.execDescribe();
					result.write(System.out, "N-TRIPLES", null);
				} else if (query.isConstructType()) {
					if (streamWindow > 0) {
						System.err.println("using streaming mode with window " + streamWindow);
						Iterator<Triple> results = qe.execConstructTriples();
						StreamRDF writer = StreamRDFWriter.getWriterStream(System.out, Lang.NTRIPLES);
						writer.start();
						StreamOps.sendTriplesToStream(results, writer);
						writer.finish();
					} else {
						System.err.println("not streaming");
						Model result = qe.execConstruct();
						result.write(System.out, "N-TRIPLES", null);
					}
				} else if (query.isAskType()) {
					boolean b = qe.execAsk();
					System.out.println(b);
				}
			} finally {
				qe.close();				
			}
		} finally {			
			// Close
			hdt.close();
		}
	}

	/**
	 * HDTSparql, receives a SPARQL query and executes it against an HDT file.
	 * @param args
	 */
	public static void main(String[] args) throws Throwable {
		HDTSparql hdtSparql = new HDTSparql();
		JCommander com = new JCommander(hdtSparql, args);
		com.setProgramName("hdtsparql");

		if (hdtSparql.parameters.size() != 2) {
			com.usage();
			System.exit(1);
		}

		hdtSparql.fileHDT = hdtSparql.parameters.get(0);
		hdtSparql.sparqlQuery = hdtSparql.parameters.get(1);

		hdtSparql.execute();
	}
}
