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

package hdt.dictionary.impl;

import hdt.dictionary.Dictionary;
import hdt.dictionary.DictionarySection;
import hdt.enums.TripleComponentRole;
import hdt.options.HDTSpecification;
import hdt.triples.TripleID;
import hdt.triples.TripleString;

/**
 * @author mario.arias
 *
 */
public abstract class BaseDictionary implements Dictionary {
	protected HDTSpecification spec;
	protected DictionarySection subjects;
	protected DictionarySection predicates, objects, shared;
	
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
	
	private DictionarySection getSection(int id, TripleComponentRole role) {
		switch (role) {
		case SUBJECT:
			if(id<=shared.getNumberOfElements()) {
				return shared;
			} else {
				return subjects;
			}
		case PREDICATE:
			return predicates;
		case OBJECT:
			if(id<=shared.getNumberOfElements()) {
				return shared;
			} else {
				return objects;
			}
		}
		throw new IllegalArgumentException();
	}
	
	private int getGlobalId(Mapping mapping, int id, DictionarySectionType position) {
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


	private int getGlobalId(int id, DictionarySectionType position) {
		return getGlobalId(this.mapping, id, position);
	}
	
	private int getLocalId(Mapping mapping, int id, TripleComponentRole position) {
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

	private int getLocalId(int id, TripleComponentRole position) {
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
	 * @see hdt.dictionary.Dictionary#tripleIDtoTripleString(hdt.triples.TripleID)
	 */
	@Override
	public TripleString tripleIDtoTripleString(TripleID tripleID) {
		return new TripleString(
				idToString(tripleID.getSubject(), TripleComponentRole.SUBJECT), 
				idToString(tripleID.getPredicate(), TripleComponentRole.PREDICATE), 
				idToString(tripleID.getObject(), TripleComponentRole.OBJECT)
		);
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#idToString(int, datatypes.TripleComponentRole)
	 */
	@Override
	public CharSequence idToString(int id, TripleComponentRole role) {
		DictionarySection section = getSection(id, role);
		int localId = getLocalId(id, role);
		return section.extract(localId);
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

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#getNumberOfElements()
	 */
	@Override
	public long getNumberOfElements() {
		return subjects.getNumberOfElements()+predicates.getNumberOfElements()+objects.getNumberOfElements()+shared.getNumberOfElements();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#size()
	 */
	@Override
	public long size() {
		return subjects.size()+predicates.size()+objects.size()+shared.size();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#getNsubjects()
	 */
	@Override
	public long getNsubjects() {
		return subjects.getNumberOfElements()+shared.getNumberOfElements();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#getNpredicates()
	 */
	@Override
	public long getNpredicates() {
		return predicates.getNumberOfElements();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#getNobjects()
	 */
	@Override
	public long getNobjects() {
		return objects.getNumberOfElements()+shared.getNumberOfElements();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#getSsubobj()
	 */
	@Override
	public long getSsubobj() {
		return shared.getNumberOfElements();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#getMaxID()
	 */
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

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#getMaxSubjectID()
	 */
	@Override
	public long getMaxSubjectID() {
		return getNsubjects();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#getMaxPredicateID()
	 */
	@Override
	public long getMaxPredicateID() {
		return getNpredicates();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#getMaxObjectID()
	 */
	@Override
	public long getMaxObjectID() {
		if(mapping==Mapping.MAPPING2) {
			return shared.getNumberOfElements()+objects.getNumberOfElements();
		} else {
			return shared.getNumberOfElements()+subjects.getNumberOfElements()+objects.getNumberOfElements();
		}
	}

	

}
