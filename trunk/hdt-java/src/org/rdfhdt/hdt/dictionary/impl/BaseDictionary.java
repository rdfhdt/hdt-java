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

package org.rdfhdt.hdt.dictionary.impl;

import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.dictionary.DictionarySection;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleString;

/**
 * 
 * This abstract clas implements all general methods that are the same
 * for every implementation of Dictionary.
 * 
 * @author mario.arias, Eugen
 *
 */
public abstract class BaseDictionary implements Dictionary {
	
	protected HDTSpecification spec;
	
	protected DictionarySection subjects; 
	protected DictionarySection predicates;
	protected DictionarySection objects;
	protected DictionarySection shared;
	
	public BaseDictionary(HDTSpecification spec) {
		this.spec = spec;
	}
	
	protected enum DictionarySectionType {
		SUBJECT,
		PREDICATE,
		OBJECT,
		SHARED
	};
	
	protected enum Mapping {
		MAPPING1,
		MAPPING2
	}
	
	protected Mapping mapping = Mapping.MAPPING2;
	
	protected int getGlobalId(Mapping mapping, int id, DictionarySectionType position) {
		switch (position) {
		case SUBJECT:
			return shared.getNumberOfElements()+id;

		case PREDICATE:
			return id;

		case OBJECT:
			if(mapping==Mapping.MAPPING2) {
				return shared.getNumberOfElements()+id;
			} else {
				return shared.getNumberOfElements()+subjects.getNumberOfElements()+id;
			}
		case SHARED:	                
			return id;
		}

		throw new IllegalArgumentException();
	}


	protected int getGlobalId(int id, DictionarySectionType position) {
		return getGlobalId(this.mapping, id, position);
	}

	protected int getLocalId(Mapping mapping, int id, TripleComponentRole position) {
		switch (position) {
		case SUBJECT:
			if(id<=shared.getNumberOfElements()) {
				return id;
			} else {
				return id-shared.getNumberOfElements();
			}
		case OBJECT:
			if(id<=shared.getNumberOfElements()) {
				return id;
			} else {
				if(mapping==Mapping.MAPPING2) {
					return id-shared.getNumberOfElements();
				} else {
					return 2+id-shared.getNumberOfElements()-subjects.getNumberOfElements();
				}
			}
		case PREDICATE:
			return id;
		}

		throw new IllegalArgumentException();
	}

	protected int getLocalId(int id, TripleComponentRole position) {
		return getLocalId(mapping,id,position);
	}
	
	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#tripleStringtoTripleID(hdt.triples.TripleString)
	 */
	@Override
	public TripleID tripleStringtoTripleID(TripleString tripleString) {
		return new TripleID(
				stringToId(tripleString.getSubject(), TripleComponentRole.SUBJECT), 
				stringToId(tripleString.getPredicate(), TripleComponentRole.PREDICATE), 
				stringToId(tripleString.getObject(), TripleComponentRole.OBJECT)
				);
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#stringToId(java.lang.CharSequence, datatypes.TripleComponentRole)
	 */
	@Override
	public int stringToId(CharSequence str, TripleComponentRole position) {

		if(str==null || str.length()==0 || str.charAt(0)=='?') {
			return 0;
		}

		int ret=0;
		switch(position) {
		case SUBJECT:
			ret = shared.locate(str);
			if(ret!=0) {
				return getGlobalId(ret, DictionarySectionType.SHARED);
			}
			ret = subjects.locate(str);
			if(ret!=0) {
				return getGlobalId(ret, DictionarySectionType.SUBJECT);
			}
			return -1;
		case PREDICATE:
			ret = predicates.locate(str);
			if(ret!=0) {
				return getGlobalId(ret, DictionarySectionType.PREDICATE);
			}
			return -1;
		case OBJECT:
			ret = shared.locate(str);
			if(ret!=0) {
				return getGlobalId(ret, DictionarySectionType.SHARED);
			}
			ret = objects.locate(str);
			if(ret!=0) {
				return getGlobalId(ret, DictionarySectionType.OBJECT);
			}
			return -1;
		}
		throw new IllegalArgumentException();
	}	
	
	@Override
	public long getNumberOfElements() {
		return subjects.getNumberOfElements()+predicates.getNumberOfElements()+objects.getNumberOfElements()+shared.getNumberOfElements();
	}

	@Override
	public long size() {
		return subjects.size()+predicates.size()+objects.size()+shared.size();
	}

	@Override
	public long getNsubjects() {
		return subjects.getNumberOfElements()+shared.getNumberOfElements();
	}

	@Override
	public long getNpredicates() {
		return predicates.getNumberOfElements();
	}

	@Override
	public long getNobjects() {
		return objects.getNumberOfElements()+shared.getNumberOfElements();
	}

	@Override
	public long getNshared() {
		return shared.getNumberOfElements();
	}

	@Override
	public long getMaxID() {
		int s = subjects.getNumberOfElements();
        int o = objects.getNumberOfElements();
        int sh = shared.getNumberOfElements();
        int max = s>o ? s : o;

        if(mapping==Mapping.MAPPING2) {
                return sh+max;
        } else {
                return sh+s+o;
        }

	}

	@Override
	public long getMaxSubjectID() {
		return getNsubjects();
	}

	@Override
	public long getMaxPredicateID() {
		return getNpredicates();
	}

	@Override
	public long getMaxObjectID() {
		if(mapping==Mapping.MAPPING2) {
			return shared.getNumberOfElements()+objects.getNumberOfElements();
		} else {
			return shared.getNumberOfElements()+subjects.getNumberOfElements()+objects.getNumberOfElements();
		}
	}
	
	@Override
	public DictionarySection getSubjects() {
		return subjects;
	}
	
	@Override
	public DictionarySection getPredicates() {
		return predicates;
	}
	
	@Override
	public DictionarySection getObjects() {
		return objects;
	}
	
	@Override
	public DictionarySection getShared() {
		return shared;
	}
	
}