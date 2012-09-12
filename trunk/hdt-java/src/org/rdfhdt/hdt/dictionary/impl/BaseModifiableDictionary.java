/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/dictionary/impl/BaseDictionary.java $
 * Revision: $Rev: 57 $
 * Last modified: $Date: 2012-08-24 01:26:52 +0100 (Fri, 24 Aug 2012) $
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

import java.util.Iterator;

import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.dictionary.ModifiableDictionary;
import org.rdfhdt.hdt.dictionary.ModifiableDictionarySection;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.ModifiableTriples;

/**
 * This abstract class implements all methods that have implementation
 * common to all modifiable dictionaries (or could apply to)
 * 
 * @author Eugen
 *
 */
public abstract class BaseModifiableDictionary extends BaseDictionary implements ModifiableDictionary {
	
	protected boolean isOrganzied = false;

	public BaseModifiableDictionary(HDTSpecification spec) {
		super(spec);
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#insert(java.lang.String, datatypes.TripleComponentRole)
	 */
	@Override
	public int insert(CharSequence str, TripleComponentRole position) {
		switch(position) {
		case SUBJECT:
			isOrganzied = false;
			return ((ModifiableDictionarySection)subjects).add(str);
		case PREDICATE:
			isOrganzied = false;
			return ((ModifiableDictionarySection)predicates).add(str);			
		case OBJECT:
			isOrganzied = false;
			return ((ModifiableDictionarySection)objects).add(str);
		}
		throw new IllegalArgumentException();
	}

	@Override
	public void reorganize() {
		
		// Generate shared
		Iterator<? extends CharSequence> itSubj = ((ModifiableDictionarySection)subjects).getEntries();
		while(itSubj.hasNext()) {
			CharSequence str = itSubj.next();

			// FIXME: These checks really needed?
			if(str.length()>0 && str.charAt(0)!='"' && objects.locate(str)!=0) {
				((ModifiableDictionarySection)shared).add(str);
			}
		}

		// Remove shared from subjects and objects
		Iterator<? extends CharSequence> itShared = ((ModifiableDictionarySection)shared).getEntries();
		while(itShared.hasNext()) {
			CharSequence sharedStr = itShared.next();
			((ModifiableDictionarySection)subjects).remove(sharedStr);
			((ModifiableDictionarySection)objects).remove(sharedStr);
		}

		// Sort sections individually
		((ModifiableDictionarySection)shared).sort();
		((ModifiableDictionarySection)subjects).sort();
		((ModifiableDictionarySection)objects).sort();
		((ModifiableDictionarySection)predicates).sort();
		
		isOrganzied = true;

	}

	/**
	 * This method is used in the one-pass way of working in which case it
	 * should not be used with a disc-backed dictionary because remapping
	 * requires practically a copy of the dictionary which is very bad...
	 * (it is ok for in-memory and they should override and write implementation)
	 */
	@Override
	public void reorganize(ModifiableTriples triples) {
		throw new NotImplementedException();
	}
	
	@Override
	public boolean isOrganized() {
		return isOrganzied;
	}

	@Override
	public void clear() {
		((ModifiableDictionarySection)subjects).clear();
		((ModifiableDictionarySection)predicates).clear();
		((ModifiableDictionarySection)shared).clear();
		((ModifiableDictionarySection)objects).clear();
	}
	
	@Override
	public void load(Dictionary other, ProgressListener listener) {
		((ModifiableDictionarySection)subjects).clear();
		((ModifiableDictionarySection)predicates).clear();
		((ModifiableDictionarySection)objects).clear();
		((ModifiableDictionarySection)shared).clear();
		
		((ModifiableDictionarySection)subjects).load(other.getSubjects(), null);
		((ModifiableDictionarySection)predicates).load(other.getPredicates(), null);
		((ModifiableDictionarySection)objects).load(other.getObjects(), null);
		((ModifiableDictionarySection)shared).load(other.getShared(), null);
	}
	
}
