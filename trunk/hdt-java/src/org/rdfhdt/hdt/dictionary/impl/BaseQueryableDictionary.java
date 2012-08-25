/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/dictionary/impl/BaseDictionary.java $
 * Revision: $Rev: 47 $
 * Last modified: $Date: 2012-08-03 22:32:51 +0100 (Fri, 03 Aug 2012) $
 * Last modified by: $Author: simpsonim13@gmail.com $
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

package org.rdfhdt.hdt.dictionary.impl;

import org.rdfhdt.hdt.dictionary.QueryableDictionary;
import org.rdfhdt.hdt.dictionary.QueryableDictionarySection;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleString;

/**
 * This abstract class implements all general methods that are the same
 * for every QueryableDictionary.
 * 
 * @author mario.arias, Eugen
 *
 */
public abstract class BaseQueryableDictionary extends BaseDictionary implements QueryableDictionary {

	public BaseQueryableDictionary(HDTSpecification spec) {
		super(spec);
	}

	private QueryableDictionarySection getSection(int id, TripleComponentRole role) {
		switch (role) {
		case SUBJECT:
			if(id<=shared.getNumberOfElements()) {
				return (QueryableDictionarySection)shared;
			} else {
				return (QueryableDictionarySection)subjects;
			}
		case PREDICATE:
			return (QueryableDictionarySection)predicates;
		case OBJECT:
			if(id<=shared.getNumberOfElements()) {
				return (QueryableDictionarySection)shared;
			} else {
				return (QueryableDictionarySection)objects;
			}
		}
		throw new IllegalArgumentException();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#tripleIDtoTripleString(hdt.triples.TripleID)
	 */
	@Override
	public TripleString tripleIDtoTripleString(TripleID tripleID) {
		return new TripleString(
				idToString(tripleID.getSubject(), TripleComponentRole.SUBJECT).toString(), 
				idToString(tripleID.getPredicate(), TripleComponentRole.PREDICATE).toString(), 
				idToString(tripleID.getObject(), TripleComponentRole.OBJECT).toString()
				);
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#idToString(int, datatypes.TripleComponentRole)
	 */
	@Override
	public CharSequence idToString(int id, TripleComponentRole role) {
		QueryableDictionarySection section = getSection(id, role);
		int localId = getLocalId(id, role);
		return section.extract(localId);
	}

}
