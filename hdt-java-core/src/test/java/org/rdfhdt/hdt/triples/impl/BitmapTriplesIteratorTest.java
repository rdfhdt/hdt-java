package org.rdfhdt.hdt.triples.impl;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;

import org.junit.Before;
import org.junit.Test;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.rdf.parsers.RDFParserRAR;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.util.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static junit.framework.TestCase.assertEquals;

public class BitmapTriplesIteratorTest {
	private static final Logger log = LoggerFactory.getLogger(BitmapTriplesIteratorTest.class);
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test() throws IOException {
//		HDT hdt = HDTManager.mapHDT("/Users/mck/hdt/swdf.hdt", null);
//
//		int t = (int) hdt.getTriples().getNumberOfElements();
//		BitmapTriplesIterator it = new BitmapTriplesIterator((BitmapTriples) hdt.getTriples(), t-10, t);
//
//		while(it.hasNext()) {
//			System.out.println(it.next());
//		}
	}
	@Test
	public void testFindByIndex() throws IOException {
		ClassLoader classLoader = getClass().getClassLoader();
		String file = Objects.requireNonNull(classLoader.getResource("dbpedia.hdt")).getFile();
		try {
			HDT hdt = HDTManager.mapHDT(new File(file).getAbsolutePath());
			IteratorTripleID it = hdt.getTriples().searchAll();
			int count = 1;
			while(it.hasNext()) {
				TripleID triple = it.next();
				assertEquals(triple,hdt.getTriples().find(count));
				count++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Test
	public void testIteratorsWithIndex() throws IOException {
		ClassLoader classLoader = getClass().getClassLoader();
		String file = Objects.requireNonNull(classLoader.getResource("dbpedia.hdt")).getFile();
		try {
			HDT hdt = HDTManager.mapHDT(new File(file).getAbsolutePath());
			System.out.println("Testing iterator with index, for patterns like ?P? ");
			// iterator for ? P ?
			IteratorTripleString it = hdt.searchWithId("","http://www.w3.org/1999/02/22-rdf-syntax-ns#type","");
			int count = 1;
			checkIteratorMatching(it,hdt);
			System.out.println("Testing iterator with index, for patterns like ?PO ");
			// iterator for ? P O
			it = hdt.searchWithId("","http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
					"http://dbpedia.org/ontology/Image");
			checkIteratorMatching(it,hdt);
			System.out.println("Testing iterator with index, for patterns like S?? or SP? or SPO");
			// iterator for S??
			it = hdt.searchWithId("http://commons.wikimedia.org/wiki/Special:FilePath/96ST1AVE.JPG",
					"", "");
			checkIteratorMatching(it,hdt);
			System.out.println("Testing iterator with extra objects index, for patterns like ?PO or ??O");
			// iterator for ?PO
			hdt = HDTManager.mapIndexedHDT(new File(file).getAbsolutePath());
			it = hdt.searchWithId("","http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
					"http://dbpedia.org/ontology/Image");
			checkIteratorMatching(it,hdt);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private void checkIteratorMatching(Iterator<TripleString> it,HDT hdt){
		while(it.hasNext()) {
			TripleString triple = it.next();
			// find the triple with the given index from the triple
			TripleID tripleID = hdt.getTriples().find(triple.getIndex());
			// compare the IDs of the triple found and the one coming from the iterator
			long subId = hdt.getDictionary().stringToId(triple.getSubject(), TripleComponentRole.SUBJECT);
			assertEquals(tripleID.getSubject(),subId);

			long predId = hdt.getDictionary().stringToId(triple.getPredicate(), TripleComponentRole.PREDICATE);
			assertEquals(tripleID.getPredicate(),predId);

			long objId = hdt.getDictionary().stringToId(triple.getObject(), TripleComponentRole.OBJECT);
			assertEquals(tripleID.getObject(),objId);
		}
	}

}