/**
 * File: $HeadURL$
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

package org.rdfhdt.hdt.iterator;

import org.rdfhdt.hdt.dictionary.QueryableDictionary;
import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleString;

/**
 * Iterator of TripleStrings based on IteratorTripleID
 * 
 */
public class DictionaryTranslateIterator implements IteratorTripleString {

	/** The iterator of TripleID */
	IteratorTripleID iterator;
	/** The dictionary */
	QueryableDictionary dictionary;

	/**
	 * Basic constructor
	 * 
	 * @param iteratorTripleID
	 *            Iterator of TripleID to be used
	 * @param dictionary
	 *            The dictionary to be used
	 */
	public DictionaryTranslateIterator(IteratorTripleID iteratorTripleID, QueryableDictionary dictionary) {
		this.iterator = iteratorTripleID;
		this.dictionary = dictionary;
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
		return dictionary.tripleIDtoTripleString(triple);
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
	 * @see hdt.iterator.IteratorTripleString#hasPrevious()
	 */
	@Override
	public boolean hasPrevious() {
		return iterator.hasPrevious();
	}

	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleString#previous()
	 */
	@Override
	public TripleString previous() {
		TripleID triple = iterator.previous();
		return dictionary.tripleIDtoTripleString(triple);
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
