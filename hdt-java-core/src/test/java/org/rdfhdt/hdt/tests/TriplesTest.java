package org.rdfhdt.hdt.tests;

import java.util.Iterator;

import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.impl.BitmapTriples;
import org.rdfhdt.hdt.triples.impl.BitmapTriplesIteratorY;
import org.rdfhdt.hdt.triples.impl.BitmapTriplesIteratorYFOQ;
import org.rdfhdt.hdt.util.StopWatch;

public class TriplesTest {

	private static void measure(Iterator<TripleID> it) {
		StopWatch st = new StopWatch();
		long count=0;
		while (it.hasNext()) {
			TripleID tripleID = it.next();

			if((count%1000000)==0) {
				System.out.println((count/1000000)+"M");
			}
			count++;
		}
		System.out.println(st.stopAndShow());
	}
	
	private static void compare(Iterator<TripleID> itA, Iterator<TripleID> itB) {
		long count=0;
		while (itA.hasNext() && itB.hasNext()) {
			TripleID a = itA.next();
			TripleID b = itB.next();
			
			if(!a.equals(b)) {
				System.out.println("DIFFERENT: "+a+" => "+b);
			}
		
			if((count%1000000)==0) {
				System.out.println((count/1000000)+"M");
			}
			count++;
		}
		
		while(itA.hasNext()) {
			System.out.println("A: "+itA.next());
		}
		while(itB.hasNext()) {
			System.out.println("B: "+itB.next());
		}
	}
	
	public static void main(String[] args) throws Throwable {
		HDT hdt = HDTManager.mapIndexedHDT("/Users/mck/hdt/DBPedia-3.9-en.hdt", null);
		
		TripleID pat = new TripleID(0, 53201,0);
		final BitmapTriples t = (BitmapTriples) hdt.getTriples();
		
		
		Iterator<TripleID> itA = new BitmapTriplesIteratorYFOQ(t, pat);
		Iterator<TripleID> itB = new BitmapTriplesIteratorY(t, pat);
		
		measure(itA);
		
//		compare(itA,itB);

	}
}
