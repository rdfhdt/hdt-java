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

package org.rdfhdt.hdt.hdt.impl;

import org.rdfhdt.hdt.dictionary.QueryableDictionary;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.hdt.QueryableHDT;
import org.rdfhdt.hdt.iterator.DictionaryTranslateIterator;
import org.rdfhdt.hdt.iterator.IteratorTripleString;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.TripleID;

/**
 * This class is an implementation of a HDT that is modifiable but is also meant to be 
 * queryable, meaning that it supports searching and all other operations that a HDT has to have
 * but is also at the same time modifiable (most probably meaning in memory).
 * 
 * @author Eugen
 *
 */
public class HDTRW extends BaseModifiableHDT implements QueryableHDT {
	
	private QueryableDictionary dictionary;
	
	/**
	 * Constructor, it constructs a BaseModifiableHDT with the given
	 * parameters and then checks to see if the Dictionary object that
	 * was created in the BaseModifiableHDT is also a QueryableDictionary.
	 * If it is it puts in the the QueryableDictionary variable of the class
	 * for use in the methods of this class.
	 */
	public HDTRW(HDTSpecification spec, String baseUri, ModeOfLoading modeOfLoading) {
		super(spec, baseUri, modeOfLoading);
		
		try {
			this.dictionary = (QueryableDictionary)getDictionary();
		} catch (ClassCastException e){
			throw new RuntimeException("HDTRW has to be instantiated with a Dictionary object " +
					"that implements both ModifiableDictionary and QueryableDictionary. What " +
					"implementation of the dictionary is used is probably determined in the " +
					"constructor of BaseModifiableHDT or in the DictionaryFacory.",e);
		}
	}

	@Override
	public IteratorTripleString search(CharSequence subject, CharSequence predicate, CharSequence object) {

		// Conversion from TripleString to TripleID
		TripleID triple = new TripleID(
				dictionary.stringToId(subject, TripleComponentRole.SUBJECT),
				dictionary.stringToId(predicate, TripleComponentRole.PREDICATE),
				dictionary.stringToId(object, TripleComponentRole.OBJECT)
				);

		return new DictionaryTranslateIterator(getTriples().search(triple), dictionary);
	}

}
