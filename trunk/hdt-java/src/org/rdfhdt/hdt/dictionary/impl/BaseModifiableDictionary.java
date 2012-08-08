package org.rdfhdt.hdt.dictionary.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.dictionary.DictionarySection;
import org.rdfhdt.hdt.dictionary.DictionarySectionModifiable;
import org.rdfhdt.hdt.dictionary.ModifiableDictionary;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.header.Header;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.ModifiableTriples;

public abstract class BaseModifiableDictionary extends BaseDictionary implements
		ModifiableDictionary {

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
			return ((DictionarySectionModifiable)subjects).add(str);
		case PREDICATE:
			return ((DictionarySectionModifiable)predicates).add(str);			
		case OBJECT:
			return ((DictionarySectionModifiable)objects).add(str);
		}
		throw new IllegalArgumentException();
	}

	@Override
	public void reorganize() {
		
		// Generate shared
		Iterator<? extends CharSequence> itSubj = ((DictionarySectionModifiable)subjects).getEntries();
		while(itSubj.hasNext()) {
			CharSequence str = itSubj.next();

			// FIXME: These checks really needed?
			if(str.length()>0 && str.charAt(0)!='"' && objects.locate(str)!=0) {
				((DictionarySectionModifiable)shared).add(str);
			}
		}

		// Remove shared from subjects and objects
		Iterator<? extends CharSequence> itShared = ((DictionarySectionModifiable)shared).getEntries();
		while(itShared.hasNext()) {
			CharSequence sharedStr = itShared.next();
			((DictionarySectionModifiable)subjects).remove(sharedStr);
			((DictionarySectionModifiable)objects).remove(sharedStr);
		}

		// Sort sections individually
		((DictionarySectionModifiable)subjects).sort();
		((DictionarySectionModifiable)predicates).sort();
		((DictionarySectionModifiable)objects).sort();
		((DictionarySectionModifiable)shared).sort();

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
		((DictionarySectionModifiable)subjects).clear();
		((DictionarySectionModifiable)predicates).clear();
		((DictionarySectionModifiable)shared).clear();
		((DictionarySectionModifiable)objects).clear();
	}
	
	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#load(hdt.dictionary.Dictionary)
	 */
	@Override
	public void load(Dictionary other, ProgressListener listener) {
		((DictionarySectionModifiable)subjects).clear();
		((DictionarySectionModifiable)predicates).clear();
		((DictionarySectionModifiable)objects).clear();
		((DictionarySectionModifiable)shared).clear();
		
		((DictionarySectionModifiable)subjects).load(other.getSubjects(), null);
		((DictionarySectionModifiable)predicates).load(other.getPredicates(), null);
		((DictionarySectionModifiable)objects).load(other.getObjects(), null);
		((DictionarySectionModifiable)shared).load(other.getShared(), null);
	}
	
	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#save(java.io.OutputStream, hdt.ControlInformation, hdt.ProgressListener)
	 */
	@Override
	public void save(OutputStream output, ControlInformation ci, ProgressListener listener) {
		throw new NotImplementedException();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#load(java.io.InputStream)
	 */
	@Override
	public void load(InputStream input, ControlInformation ci, ProgressListener listener) {
		throw new NotImplementedException();
	}
	
	@Override
	public void populateHeader(Header header, String rootNode) {
		throw new NotImplementedException();
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
