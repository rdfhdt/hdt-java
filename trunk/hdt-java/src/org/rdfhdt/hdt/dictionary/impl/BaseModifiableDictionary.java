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

public abstract class BaseModifiableDictionary extends BaseDictionary implements ModifiableDictionary {

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
			return ((ModifiableDictionarySection)subjects).add(str);
		case PREDICATE:
			return ((ModifiableDictionarySection)predicates).add(str);			
		case OBJECT:
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
		((ModifiableDictionarySection)subjects).sort();
		((ModifiableDictionarySection)predicates).sort();
		((ModifiableDictionarySection)objects).sort();
		((ModifiableDictionarySection)shared).sort();

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
