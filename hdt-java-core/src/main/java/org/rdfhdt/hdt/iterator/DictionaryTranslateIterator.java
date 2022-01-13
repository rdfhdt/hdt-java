/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/iterator/DictionaryTranslateIterator.java $
 * Revision: $Rev: 191 $
 * Last modified: $Date: 2013-03-03 11:41:43 +0000 (dom, 03 mar 2013) $
 * Last modified by: $Author: mario.arias $
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 3.0 of the License.
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

package org.rdfhdt.hdt.iterator;

import org.rdfhdt.hdt.dictionary.Dictionary;
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
public class DictionaryTranslateIterator implements IteratorTripleString {

	/** The iterator of TripleID */
    final IteratorTripleID iterator;
	/** The dictionary */
    final Dictionary dictionary;

	CharSequence s, p, o;
	
	long lastSid, lastPid, lastOid;
	CharSequence lastSstr, lastPstr, lastOstr;
	
	/**
	 * Basic constructor
	 * 
	 * @param iteratorTripleID
	 *            Iterator of TripleID to be used
	 * @param dictionary
	 *            The dictionary to be used
	 */
	public DictionaryTranslateIterator(IteratorTripleID iteratorTripleID, Dictionary dictionary) {
		this.iterator = iteratorTripleID;
		this.dictionary = dictionary;
		this.s = this.p = this.o = "";
	}
	
	/**
	 * Basic constructor
	 * 
	 * @param iteratorTripleID
	 *            Iterator of TripleID to be used
	 * @param dictionary
	 *            The dictionary to be used
	 */
	public DictionaryTranslateIterator(IteratorTripleID iteratorTripleID, Dictionary dictionary, CharSequence s, CharSequence p, CharSequence o) {
		this.iterator = iteratorTripleID;
		this.dictionary = dictionary;
		this.s = s==null ? "" : s;
		this.p = p==null ? "" : p;
		this.o = o==null ? "" : o;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Iterator#hasNext()
	 */
	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Iterator#next()
	 */
	@Override
	public TripleString next() {
		TripleID triple = iterator.next();
		// convert the tripleID to TripleString
		
		if(s.length()!=0) {
			lastSstr = s;
		} else if(triple.getSubject()!=lastSid) {
			lastSid = triple.getSubject();
			lastSstr = dictionary.idToString(lastSid, TripleComponentRole.SUBJECT);
		}
		
		if(p.length()!=0) {
			lastPstr = p;
		} else if(triple.getPredicate()!=lastPid) {
			lastPstr = dictionary.idToString(triple.getPredicate(), TripleComponentRole.PREDICATE);
			lastPid = triple.getPredicate();
		}
		
		if(o.length()!=0) {
			lastOstr = o;
		} else if(triple.getObject()!=lastOid) {
			lastOstr = dictionary.idToString(triple.getObject(), TripleComponentRole.OBJECT);
			lastOid = triple.getObject();
		}

		return new TripleString(lastSstr, lastPstr, lastOstr,triple.getIndex());
//		return DictionaryUtil.tripleIDtoTripleString(dictionary, triple);
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
	}

	@Override
	public long estimatedNumResults() {
		return iterator.estimatedNumResults();
	}

	@Override
	public ResultEstimationType numResultEstimation() {		
		return iterator.numResultEstimation();
	}

}
