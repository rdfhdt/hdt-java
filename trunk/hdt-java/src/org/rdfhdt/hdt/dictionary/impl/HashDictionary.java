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
import org.rdfhdt.hdt.dictionary.DictionarySectionModifiable;
import org.rdfhdt.hdt.dictionary.ModifiableDictionary;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.header.Header;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.ModifiableTriples;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.util.StopWatch;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * @author mario.arias
 *
 */
public class HashDictionary extends BaseDictionary implements ModifiableDictionary {
	
	public HashDictionary(HDTSpecification spec) {
		super(spec);
	
		// FIXME: Read types from spec
		subjects = new DictionarySectionHash();
		predicates = new DictionarySectionHash();
		objects = new DictionarySectionHash();
		shared = new DictionarySectionHash();
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
	public void clear() {
		((DictionarySectionModifiable)subjects).clear();
		((DictionarySectionModifiable)predicates).clear();
		((DictionarySectionModifiable)shared).clear();
		((DictionarySectionModifiable)objects).clear();
	}
	
	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#startProcessing()
	 */
	@Override
	public void startProcessing() {
		
	}
	
	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#reorganize(hdt.triples.ModifiableTriples)
	 */
	@Override
	public void reorganize(ModifiableTriples triples) {
		DictionaryIDMapping mapSubj = new DictionaryIDMapping(subjects.getNumberOfElements());
		DictionaryIDMapping mapPred = new DictionaryIDMapping(predicates.getNumberOfElements());
		DictionaryIDMapping mapObj = new DictionaryIDMapping(objects.getNumberOfElements());
		
		StopWatch st = new StopWatch();
		
		// Generate old subject mapping
		Iterator<CharSequence> itSubj = subjects.getEntries();
		while(itSubj.hasNext()) {
			CharSequence str = itSubj.next();
			mapSubj.add(str);
			
			// GENERATE SHARED at the same time
			if(str.length()>0 && str.charAt(0)!='"' && objects.locate(str)!=0) {
				((DictionarySectionModifiable)shared).add(str);
			}
		}
		System.out.println("Num shared: "+shared.getNumberOfElements()+" in "+st.stopAndShow());

		// Generate old predicate mapping
		st.reset();
		Iterator<CharSequence> itPred = predicates.getEntries();
		while(itPred.hasNext()) {
			CharSequence str = itPred.next();
			mapPred.add(str);
		}		
		
		// Generate old object mapping
		Iterator<CharSequence> itObj = objects.getEntries();
		while(itObj.hasNext()) {
			CharSequence str = itObj.next();
			mapObj.add(str);
		}
		
		// Remove shared from subjects and objects
		Iterator<CharSequence> itShared = shared.getEntries();
		while(itShared.hasNext()) {
			CharSequence sharedStr = itShared.next();
			((DictionarySectionModifiable)subjects).remove(sharedStr);
			((DictionarySectionModifiable)objects).remove(sharedStr);
		}
		System.out.println("Mapping generated in "+st.stopAndShow());
		
		// Sort sections individually
		st.reset();
		((DictionarySectionModifiable)subjects).sort();
		((DictionarySectionModifiable)predicates).sort();
		((DictionarySectionModifiable)objects).sort();
		((DictionarySectionModifiable)shared).sort();
		System.out.println("Sections sorted in "+ st.stopAndShow());
		
		// Update mappings with new IDs
		st.reset();
		for(int j=0;j<mapSubj.size();j++) {
			mapSubj.setNewID(j, this.stringToId(mapSubj.getString(j), TripleComponentRole.SUBJECT));
//			System.out.print("Subj Old id: "+(j+1) + " New id: "+ mapSubj.getNewID(j)+ " STR: "+mapSubj.getString(j));
		}
		
		for(int j=0;j<mapPred.size();j++) {
			mapPred.setNewID(j, this.stringToId(mapPred.getString(j), TripleComponentRole.PREDICATE));
//			System.out.print("Pred Old id: "+(j+1) + " New id: "+ mapPred.getNewID(j)+ " STR: "+mapPred.getString(j));
		}
		
		for(int j=0;j<mapObj.size();j++) {
			mapObj.setNewID(j, this.stringToId(mapObj.getString(j), TripleComponentRole.OBJECT));
			//System.out.print("Obj Old id: "+(j+1) + " New id: "+ mapObj.getNewID(j)+ " STR: "+mapObj.getString(j));
		}
		System.out.println("Update mappings in "+st.stopAndShow());
		 
		// Replace old IDs with news
		st.reset();
		Iterator<TripleID> iteratorTriples = triples.searchAll();
		int count = 0;
		while(iteratorTriples.hasNext()) {
			TripleID triple = iteratorTriples.next();
			
			triples.replace(count++, 
					mapSubj.getNewID(triple.getSubject()-1),
					mapPred.getNewID(triple.getPredicate()-1),
					mapObj.getNewID(triple.getObject()-1)
			);
		}
		System.out.println("Replace IDs in "+st.stopAndShow());
	}


	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#endProcessing()
	 */
	@Override
	public void reorganize() {

		// Generate shared
		Iterator<CharSequence> itSubj = subjects.getEntries();
		while(itSubj.hasNext()) {
			CharSequence str = itSubj.next();
			
			// FIXME: This checks really needed?
			if(str.length()>0 && str.charAt(0)!='"' && objects.locate(str)!=0) {
				((DictionarySectionModifiable)shared).add(str);
			}
		}
		
		// Remove shared from subjects and objects
		Iterator<CharSequence> itShared = shared.getEntries();
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

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#populateHeader(hdt.header.Header, java.lang.String)
	 */
	@Override
	public void populateHeader(Header header, String rootNode) {
		
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#load(hdt.dictionary.Dictionary)
	 */
	@Override
	public void load(Dictionary other, ProgressListener listener) {
		throw new NotImplementedException();
	}


	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#getType()
	 */
	@Override
	public String getType() {
		return HDTVocabulary.DICTIONARY_TYPE_PLAIN;
	}
}
