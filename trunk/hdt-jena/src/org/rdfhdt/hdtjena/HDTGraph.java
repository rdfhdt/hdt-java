/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/compact/array/ArrayFactory.java $
 * Revision: $Rev$
 * Last modified: $Date$
 * Last modified by: $Author$
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Contacting the authors:
 *   Mario Arias:               mario.arias@deri.org
 *   Javier D. Fernandez:       jfergar@infor.uva.es
 *   Miguel A. Martinez-Prieto: migumar2@infor.uva.es
 *   Alejandro Andres:          fuzzy.alej@gmail.com
 */

package org.rdfhdt.hdtjena;

import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdtjena.cache.DictionaryCache;
import org.rdfhdt.hdtjena.cache.DictionaryCacheArray;
import org.rdfhdt.hdtjena.cache.DictionaryCacheArrayWeak;

import com.hp.hpl.jena.graph.Capabilities;
import com.hp.hpl.jena.graph.GraphStatisticsHandler;
import com.hp.hpl.jena.graph.JenaNodeCreator;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.TripleMatch;
import com.hp.hpl.jena.graph.impl.GraphBase;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * @author mck
 *
 */
public class HDTGraph extends GraphBase {
	private HDT hdt;
	static HDTCapabilities capabilities= new HDTCapabilities();
	HDTStatistics hdtStatistics;
	Dictionary dictionary;
	long numSearches = 0;
	
	DictionaryCache cacheSubject, cachePredicate, cacheObject;
	
	static {
		// Register Stage Generator
//		StageGenerator orig = (StageGenerator)ARQ.getContext().get(ARQ.stageGenerator);
//		StageBuilder.setGenerator(ARQ.getContext(), new StageGeneratorHDT(orig));
	}
	
	public HDTGraph(HDT hdt) {
		this.hdt = hdt;
		this.hdtStatistics = new HDTStatistics(hdt);
		this.dictionary = hdt.getDictionary();
		
//		cacheSubject = cachePredicate = cacheObject = new DictionaryNodeCacheNone();

		cacheSubject = new DictionaryCacheArrayWeak(hdt.getDictionary().getNsubjects());
		cachePredicate = new DictionaryCacheArray(hdt.getDictionary().getNpredicates());
		cacheObject = new DictionaryCacheArrayWeak(hdt.getDictionary().getNobjects());
	
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.impl.GraphBase#graphBaseFind(com.hp.hpl.jena.graph.TripleMatch)
	 */
	@Override
	protected ExtendedIterator<Triple> graphBaseFind(TripleMatch jenaTriple) {
		IteratorTripleID hdtIterator;
//		System.out.println("Triple Search: "+jenaTriple);
		
		Dictionary dictionary = hdt.getDictionary();
		
		int subject=0, predicate=0, object=0;
		
		if(jenaTriple.getMatchSubject()!=null) {
			subject = dictionary.stringToId(jenaTriple.getMatchSubject().toString(), TripleComponentRole.SUBJECT);
		}

		if(jenaTriple.getMatchPredicate()!=null) {
			predicate = dictionary.stringToId(jenaTriple.getMatchPredicate().toString(), TripleComponentRole.PREDICATE);
		}

		if(jenaTriple.getMatchObject()!=null) {
			object = dictionary.stringToId(jenaTriple.getMatchObject().toString(), TripleComponentRole.OBJECT);
		}

		// System.out.println("Search: |"+subject+ "| |"+predicate+"| |"+ object);
		hdtIterator = hdt.getTriples().search( new TripleID(subject, predicate, object) );
		numSearches++;
		return new HDTJenaIterator(this, hdtIterator);
	}
	
	public long getNumSearches() {
		return numSearches;
	}
	
	public Triple getJenaTriple(TripleID triple) {
		Node s, p, o;		
		
		s = cacheSubject.get(triple.getSubject());
		if(s==null) {
			CharSequence subjString = dictionary.idToString(triple.getSubject(), TripleComponentRole.SUBJECT);
			char subjFirst = subjString.charAt(0); 
			if(subjFirst=='_') {
				s = JenaNodeCreator.createAnon(subjString);
			} else {
				s = JenaNodeCreator.createURI(subjString);
			}
			cacheSubject.put(triple.getSubject(), s);
		}

		p = cachePredicate.get(triple.getPredicate());
		if(p==null) {
			CharSequence subjPredicate = dictionary.idToString(triple.getPredicate(), TripleComponentRole.PREDICATE);
			p = JenaNodeCreator.createURI(subjPredicate);
			cachePredicate.put(triple.getPredicate(), p);
		}

		o = cacheObject.get(triple.getObject());
		if(o==null) {
			CharSequence subjObject = dictionary.idToString(triple.getObject(), TripleComponentRole.OBJECT);
			char objFirst = subjObject.charAt(0); 
			if(objFirst=='_') {
				o = JenaNodeCreator.createAnon(subjObject);
			} else if(objFirst=='"') {
				o = JenaNodeCreator.createLiteral(subjObject);
			} else {
				o = JenaNodeCreator.createURI(subjObject);
			}
			cacheObject.put(triple.getObject(), o);
		}
		
		return new Triple(s, p, o);
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.impl.GraphBase#getStatisticsHandler()
	 */
	@Override
	public GraphStatisticsHandler getStatisticsHandler() {
		return hdtStatistics;
	}
	
	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.impl.GraphBase#getCapabilities()
	 */
	@Override
	public Capabilities getCapabilities() {
		return HDTGraph.capabilities;
	}

	/*
	public ReorderTransformation getReorderTransform() {
		//return new ReorderSt;
		return null;
	}*/
}
