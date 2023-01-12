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

import org.apache.jena.ext.com.google.common.cache.Cache;
import org.apache.jena.ext.com.google.common.cache.CacheBuilder;
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

	@Parameter(names="--stream", description="Output CONSTRUCT/DESCRIBE query results directly as they are generated")
	public boolean streamMode = false;

	public String fileHDT;
	public String sparqlQuery;

	private final int DUP_WINDOW = 1000; // size of window used for eliminating duplicates while streaming

	public void execute() throws IOException {
		// Create HDT
		HDT hdt = HDTManager.mapIndexedHDT(fileHDT);

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
					if (streamMode) {
						Iterator<Triple> results = qe.execDescribeTriples();
						streamResults(results);
					} else {
						Model result = qe.execDescribe();
						result.write(System.out, "N-TRIPLES", null);
					}
				} else if (query.isConstructType()) {
					if (streamMode) {
						Iterator<Triple> results = qe.execConstructTriples();
						streamResults(results);
					} else {
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

	private void streamResults(Iterator<Triple> results) {
		StreamRDF writer = StreamRDFWriter.getWriterStream(System.out, Lang.NTRIPLES);
		Cache<Triple,Boolean> seenTriples = CacheBuilder.newBuilder()
				.maximumSize(DUP_WINDOW).build();

		writer.start();
		while (results.hasNext()) {
			Triple triple = results.next();
			if (seenTriples.getIfPresent(triple) != null) {
				// the triple has already been emitted
				continue;
			}
			seenTriples.put(triple, true);
			writer.triple(triple);
		}
		writer.finish();
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
