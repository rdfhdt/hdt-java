/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/dictionary/impl/BaseDictionary.java $
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

package org.rdfhdt.hdt.dictionary.impl;

import org.rdfhdt.hdt.dictionary.DictionaryPrivate;
import org.rdfhdt.hdt.dictionary.DictionarySection;
import org.rdfhdt.hdt.dictionary.DictionarySectionPrivate;
import org.rdfhdt.hdt.dictionary.TempDictionary;
import org.rdfhdt.hdt.enums.DictionarySectionRole;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.util.string.CompactString;
import org.rdfhdt.hdt.util.string.DelayedString;

import java.util.TreeMap;

/**
 * 
 * This abstract class implements all general methods that are the same
 * for every implementation of Dictionary.
 * 
 * @author mario.arias, Eugen
 *
 */
public abstract class BaseDictionary implements DictionaryPrivate {
	
	protected final HDTOptions spec;
	
	protected DictionarySectionPrivate subjects; 
	protected DictionarySectionPrivate predicates;
	protected DictionarySectionPrivate objects;
	protected DictionarySectionPrivate shared;
	protected DictionarySectionPrivate graphs;
	
	public BaseDictionary(HDTOptions spec) {
		this.spec = spec;
	}
	
	protected long getGlobalId(long id, DictionarySectionRole position) {
		switch (position) {
		case SUBJECT:
		case OBJECT:
			return shared.getNumberOfElements()+id;
			
		case PREDICATE:
		case GRAPH:
		case SHARED:
			return id;
		default:
			throw new IllegalArgumentException();
		}
	}

	protected long getLocalId(long id, TripleComponentRole position) {
		switch (position) {
		case SUBJECT:
		case OBJECT:
			if(id<=shared.getNumberOfElements()) {
				return id;
			} else {
				return id-shared.getNumberOfElements();
			}
		case PREDICATE:
		case GRAPH:
			return id;
		default:
			throw new IllegalArgumentException();
		}
	}
	
	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#stringToId(java.lang.CharSequence, datatypes.TripleComponentRole)
	 */
	@Override
	public long stringToId(CharSequence str, TripleComponentRole position) {
		str = DelayedString.unwrap(str);

		if(str==null || str.length()==0) {
			return 0;
		}
				
		if(str instanceof String) {
			// CompactString is more efficient for the binary search.
			str = new CompactString(str);
		}

		long ret=0;
		switch(position) {
		case SUBJECT:
			ret = shared.locate(str);
			if(ret!=0) {
				return getGlobalId(ret, DictionarySectionRole.SHARED);
			}
			ret = subjects.locate(str);
			if(ret!=0) {
				return getGlobalId(ret, DictionarySectionRole.SUBJECT);
			}
			return -1;
		case PREDICATE:
			ret = predicates.locate(str);
			if(ret!=0) {
				return getGlobalId(ret, DictionarySectionRole.PREDICATE);
			}
			return -1;
		case GRAPH:
			ret = graphs.locate(str);
			if(ret!=0) {
				return getGlobalId(ret, DictionarySectionRole.GRAPH);
			}
			return -1;
		case OBJECT:
			if(str.charAt(0)!='"') {
				ret = shared.locate(str);
				if(ret!=0) {
					return getGlobalId(ret, DictionarySectionRole.SHARED);
				}
			}
			ret = objects.locate(str);
			if(ret!=0) {
				return getGlobalId(ret, DictionarySectionRole.OBJECT);
			}
			return -1;
		default:
			throw new IllegalArgumentException();
		}
	}	
	
	@Override
	public long getNumberOfElements() {
		long s = subjects.getNumberOfElements();
		long p = predicates.getNumberOfElements();
		long o = objects.getNumberOfElements();
		if (!this.supportGraphs())
			return s+p+o;
		long g = graphs.getNumberOfElements();
		return s+p+o+g;
	}

	@Override
	public long size() {
		long s = subjects.size();
		long p = predicates.size();
		long o = objects.size();
		if (!this.supportGraphs())
			return s+p+o;
		long g = graphs.size();
		return s+p+o+g;
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
	public long getNgraphs() {
		if (graphs == null)
			return 0;
		return graphs.getNumberOfElements();
	}

	@Override
	public long getNshared() {
		return shared.getNumberOfElements();
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
	public DictionarySection getGraphs() {
		return graphs;
	}
	
	@Override
	public DictionarySection getShared() {
		return shared;
	}
	
	private DictionarySectionPrivate getSection(long id, TripleComponentRole role) {
		switch (role) {
		case SUBJECT:
			if(id<=shared.getNumberOfElements()) {
				return shared;
			} else {
				return subjects;
			}
		case PREDICATE:
			return predicates;
		case GRAPH:
			return graphs;
		case OBJECT:
			if(id<=shared.getNumberOfElements()) {
				return shared;
			} else {
				return objects;
			}
		default:
			throw new IllegalArgumentException();
		}
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#idToString(int, datatypes.TripleComponentRole)
	 */
	@Override
	public CharSequence idToString(long id, TripleComponentRole role) {
		DictionarySectionPrivate section = getSection(id, role);
		long localId = getLocalId(id, role);
		return section.extract(localId);
	}
	@Override
	public CharSequence dataTypeOfId(long id) {
		throw new IllegalArgumentException("Method is not applicable on this dictionary");
	}
	@Override
	public TreeMap<String, DictionarySection> getAllObjects() {
		throw new IllegalArgumentException("Method is not applicable on this dictionary");
	}
	@Override
	public long getNAllObjects() {
		throw new IllegalArgumentException("Method is not applicable on this dictionary");
	}

	@Override
	public void loadAsync(TempDictionary other, ProgressListener listener) throws InterruptedException {
		throw new NotImplementedException();
	}

	@Override
	public boolean supportGraphs() {
		return false;
	}
}
