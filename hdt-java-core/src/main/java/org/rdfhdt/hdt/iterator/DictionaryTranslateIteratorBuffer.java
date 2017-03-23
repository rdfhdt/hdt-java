/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/iterator/DictionaryTranslateIterator.java $
 * Revision: $Rev: 191 $
 * Last modified: $Date: 2013-03-03 11:41:43 +0000 (dom, 03 mar 2013) $
 * Last modified by: $Author: mario.arias $
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
 */

package org.rdfhdt.hdt.iterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.rdfhdt.hdt.dictionary.impl.DictionaryPFCOptimizedExtractor;
import org.rdfhdt.hdt.dictionary.impl.FourSectionDictionary;
import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleString;

/**
 * Iterator of TripleStrings based on IteratorTripleID
 * 
 */
public class DictionaryTranslateIteratorBuffer implements IteratorTripleString {
	private static int DEFAULT_BLOCK_SIZE=10000;
	
	final int blockSize;
	
	IteratorTripleID iterator;
	DictionaryPFCOptimizedExtractor dictionary;
	CharSequence s, p, o;

	List<TripleID> triples;
	Iterator<TripleID> child=Collections.emptyIterator();
	
	Map<Integer,CharSequence> mapSubject, mapPredicate, mapObject;

	int lastSid, lastPid, lastOid;
	CharSequence lastSstr, lastPstr, lastOstr;
	
	public DictionaryTranslateIteratorBuffer(IteratorTripleID iteratorTripleID, FourSectionDictionary dictionary, CharSequence s, CharSequence p, CharSequence o) {
		this(iteratorTripleID,dictionary,s,p,o,DEFAULT_BLOCK_SIZE);
	}
	
	public DictionaryTranslateIteratorBuffer(IteratorTripleID iteratorTripleID, FourSectionDictionary dictionary, CharSequence s, CharSequence p, CharSequence o, int blockSize) {
		this.blockSize = blockSize;
		this.iterator = iteratorTripleID;
		this.dictionary = new DictionaryPFCOptimizedExtractor(dictionary);

		this.s = s==null ? "" : s;
		this.p = p==null ? "" : p;
		this.o = o==null ? "" : o;
	}

	private void reset() {
		triples = new ArrayList<TripleID>(blockSize);
				
		if(s.length()==0) {
			mapSubject = new HashMap<>(blockSize);
		}

		if(p.length()==0) {
			mapPredicate = new HashMap<>();
		}

		if(o.length()==0) {
			mapObject = new HashMap<>(blockSize);
		}
	}

	private void fill(int [] arr, int count, Map<Integer,CharSequence> map, TripleComponentRole role) {
		Arrays.sort(arr, 0, count);
		
		int last=-1;
		for(int i=0;i<count;i++) {
			int val = arr[i];
			
			if(val!=last) {
				CharSequence str = dictionary.idToString(val, role);
				
				map.put(val, str);
				
				last = val;
			}
		}
	}

	private void fetchBlock() {
		reset();		

		int [] arrSubjects = new int[blockSize];
		int [] arrPredicates = new int[blockSize];
		int [] arrObjects = new int[blockSize];

		int count=0;
		for(int i=0;i<blockSize && iterator.hasNext();i++) {
			TripleID t = new TripleID(iterator.next());

			triples.add(t);

			if(s.length()==0) arrSubjects[count] = t.getSubject();
			if(p.length()==0) arrPredicates[count] = t.getPredicate();
			if(o.length()==0) arrObjects[count] = t.getObject();

			count++;
		}
		if(s.length()==0) fill(arrSubjects, count, mapSubject, TripleComponentRole.SUBJECT);
		if(p.length()==0) fill(arrPredicates, count, mapPredicate, TripleComponentRole.PREDICATE);
		if(o.length()==0) fill(arrObjects, count, mapObject, TripleComponentRole.OBJECT);

		this.child = triples.iterator();
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Iterator#hasNext()
	 */
	@Override
	public boolean hasNext() {
		boolean more = child.hasNext() || iterator.hasNext();
		if(!more) {
			mapSubject = mapPredicate = mapObject = null;
			triples = null;
		}
		return more;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Iterator#next()
	 */
	@Override
	public TripleString next() {
		if(!child.hasNext()) {
			fetchBlock();
		}

		TripleID triple = child.next();

		if(s.length()!=0) {
			lastSstr = s;
		} else if(triple.getSubject()!=lastSid) {
			lastSid = triple.getSubject();
			lastSstr = mapSubject.get(lastSid);
		}

		if(p.length()!=0) {
			lastPstr = p;
		} else if(triple.getPredicate()!=lastPid) {
			lastPid = triple.getPredicate();
			lastPstr = mapPredicate.get(lastPid);
		}

		if(o.length()!=0) {
			lastOstr = o;
		} else if(triple.getObject()!=lastOid) {
			lastOid = triple.getObject();
			lastOstr = mapObject.get(lastOid);
		}
		
		return new TripleString(lastSstr, lastPstr, lastOstr);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Iterator#remove()
	 */
	@Override
	public void remove() {
		iterator.remove();
	}

	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleString#goToStart()
	 */
	@Override
	public void goToStart() {
		iterator.goToStart();
		this.reset();
	}

	@Override
	public long estimatedNumResults() {
		return iterator.estimatedNumResults();
	}

	@Override
	public ResultEstimationType numResultEstimation() {		
		return iterator.numResultEstimation();
	}

	public static void setBlockSize(int size) {
		DEFAULT_BLOCK_SIZE = size;
	}
	
	public static int getBlockSize() {
		return DEFAULT_BLOCK_SIZE;
	}
}