
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.util.StopWatch;
import org.rdfhdt.hdt.util.io.DummyOutputStream;
import org.rdfhdt.hdtjena.HDTGraph;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.impl.ModelCom;

/**
 * @author mario.arias
 *
 */
public class JenaHDTTest {
	
	public static void iterateSubjects(Model model, String label) throws IOException {
		PrintWriter out = new PrintWriter(new FileOutputStream("subjects.txt"));
		
		StopWatch st = new StopWatch();
		
		ResIterator iter = model.listSubjects();
		while(iter.hasNext()) {
			Resource subject = iter.next();
			
			out.println(subject);
		}
		System.out.println(label+st.stopAndShow());
		out.close();
	}
	
	public static void iterateObjects(Model model, String label) throws IOException {
		PrintWriter out = new PrintWriter(new FileOutputStream("objects.txt"));
		
		StopWatch st = new StopWatch();
		
		NodeIterator iter = model.listObjects();
		while(iter.hasNext()) {
			RDFNode object = iter.next();
			
			out.println(object);
		}
		System.out.println(label+st.stopAndShow());
		out.close();
	}
	
	public static void findObjects(Model model, String label) throws IOException {
		try(BufferedReader reader = new BufferedReader(new FileReader(new File("objects2.txt")))) {
			PrintWriter out = new PrintWriter(DummyOutputStream.getInstance());

			StopWatch st = new StopWatch();

			String line;
			while((line = reader.readLine())!=null) {
				StmtIterator iter = model.listStatements(null, null, line);
				while (iter.hasNext()) {
					Statement stmt      = iter.nextStatement();  // get next statement
					//			    Resource  subject   = stmt.getSubject();     // get the subject
					//			    Property  predicate = stmt.getPredicate();   // get the predicate
					//			    RDFNode   object    = stmt.getObject();      // get the object

					out.println(stmt);
				} 
			}
			System.out.println(label+st.stopAndShow());
		}
	}
	
	public static void findSubjects(Model model, String label) throws IOException {
		try(BufferedReader reader = new BufferedReader(new FileReader(new File("subjects2.txt")))){
			PrintWriter out = new PrintWriter(DummyOutputStream.getInstance());

			StopWatch st = new StopWatch();

			String line;
			while((line = reader.readLine())!=null) {
				StmtIterator iter = model.listStatements(model.getResource(line), null, (RDFNode)null);
				while (iter.hasNext()) {
					Statement stmt      = iter.nextStatement();  // get next statement
					//			    Resource  subject   = stmt.getSubject();     // get the subject
					//			    Property  predicate = stmt.getPredicate();   // get the predicate
					//			    RDFNode   object    = stmt.getObject();      // get the object

					out.println(stmt);
				} 
			}
			System.out.println(label+st.stopAndShow());
		}
	}
	
	public static void iterateTriples(Model model, String label) {
		@SuppressWarnings("unused")
		PrintWriter out = new PrintWriter(DummyOutputStream.getInstance());
		
		// list the statements in the Model
		StopWatch st = new StopWatch();
		
		StmtIterator iter = model.listStatements();//(Resource)null, model.getProperty("http://www.geonames.org/ontology#alternateName"), (RDFNode)null);
		while (iter.hasNext()) {
		    @SuppressWarnings("unused")
			Statement stmt      = iter.nextStatement();  // get next statement
//		    Resource  subject   = stmt.getSubject();     // get the subject
//		    Property  predicate = stmt.getPredicate();   // get the predicate
//		    RDFNode   object    = stmt.getObject();      // get the object
		    
//		    out.println(stmt);
		} 
		
		System.out.println(label+st.stopAndShow());
	}
	
	public static void iterateHDT(HDT hdt, String label) throws Exception {
		PrintWriter out = new PrintWriter(DummyOutputStream.getInstance());
		
		StopWatch st = new StopWatch();
		IteratorTripleString it = hdt.search("", "", "");
		while(it.hasNext()) {
			TripleString triple = it.next();
			out.write(triple.toString());
		}
		System.out.println(label+st.stopAndShow());
	}
	
	public static void sparql(Model model, String sparql) {
		StopWatch st = new StopWatch();
		
		Query query = QueryFactory.create(sparql);

		// Execute the query and obtain results
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		ResultSet results = qe.execSelect();

		// Output query results	
		ResultSetFormatter.out(System.out, results, query);

		// Important - free up resources used running the query
		qe.close();
		
		System.out.println("SPARQL time: "+st.stopAndShow());
	}
	
	public static void measureTriples(Model model) {
		PrintWriter out = new PrintWriter(DummyOutputStream.getInstance());
		
		// list the statements in the Model
		StopWatch st = new StopWatch();
		
		for(int i=0;i<5;i++) {
			StmtIterator iter = model.listStatements();//(Resource)null, model.getProperty("http://www.geonames.org/ontology#alternateName"), (RDFNode)null);
			while (iter.hasNext()) {
				Statement stmt      = iter.nextStatement();  // get next statement
				//		    Resource  subject   = stmt.getSubject();     // get the subject
				//		    Property  predicate = stmt.getPredicate();   // get the predicate
				//		    RDFNode   object    = stmt.getObject();      // get the object

				out.println(stmt);
			} 
		}
		
		System.out.println("Total time: "+st.stopAndGet());
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) throws Exception {
		
		StopWatch st = new StopWatch();
		
		String fileHDT = "data/univ10.hdt";
		String fileRDF = "file:///Users/mck/rdf/dataset/universities10.nt";
		String fileTDB = "data/jenatdbuniv10";		

//		String fileHDT = "/Users/mck/workspace-deri/hdt-java-test/data/nytimes.hdt";
//		String fileRDF = "file:///Users/mck/rdf/dataset/nytimes";
//		String fileTDB = "data/jenatdbnytimes";
		
		System.out.println("HDT");
		HDT hdt = HDTManager.loadIndexedHDT(fileHDT, null);
		HDTGraph graph = new HDTGraph(hdt);
		Model model = new ModelCom(graph);
			
//		System.out.println("Jena MEM");
//		Model model = ModelFactory.createDefaultModel();
//		model.read(fileRDF, "N-TRIPLE");
		
//		System.out.println("Jena TDB");
//		Dataset dataset = TDBFactory.createDataset(fileTDB);
//		Model model = dataset.getDefaultModel();
//		//FileManager.get().readModel( model, fileRDF, "N-TRIPLES" );
		
		System.out.println("Loaded in "+st.stopAndShow());

//		JenaHDTTest.iterateTriples(model, "Warming time ");
//		JenaHDTTest.measureTriples(model);
		
		
//		JenaHDTTest.iterateTriples(model, "First time ");
//		JenaHDTTest.iterateTriples(model, "Second time ");
//		JenaHDTTest.iterateTriples(model, "Third time ");
//		JenaHDTTest.iterateTriples(model, "Fourth time ");
//		JenaHDTTest.iterateTriples(model, "Fifth time ");
		
//		JenaHDTTest.iterateSubjects(model, "Subjects ");
//		JenaHDTTest.findSubjects(model, "Subjects First ");
//		JenaHDTTest.findSubjects(model, "Subjects Second ");
		
//		JenaHDTTest.findObjects(model, "Objects ");
		
//		JenaHDTTest.iterateHDT(hdt, "HDT Iterate first ");
//		JenaHDTTest.iterateHDT(hdt, "HDT Iterate second ");
//		JenaHDTTest.iterateHDT(hdt, "HDT Iterate third ");
		
		// Create a new query
		String query1 = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX ub: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#> SELECT ?X WHERE {?X rdf:type ub:GraduateStudent . ?X ub:takesCourse <http://www.Department0.University0.edu/GraduateCourse0>}";
		String query2 = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX ub: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#>"+ 
		"SELECT ?X WHERE {?X rdf:type ub:Publication . ?X ub:publicationAuthor <http://www.Department0.University0.edu/AssistantProfessor0>}";
		
		JenaHDTTest.sparql(model, query1);
		JenaHDTTest.sparql(model, query2);
		
//		System.out.println("Num searches: "+graph.getNumSearches());
	}
}