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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.dictionary.ModifiableDictionarySection;
import org.rdfhdt.hdt.dictionary.QueryableDictionary;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.header.Header;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.ModifiableTriples;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.util.StopWatch;

/**
 * @author mario.arias, Eugen
 *
 */
public class HashDictionary extends BaseModifiableDictionary implements QueryableDictionary {
	
	/**
	 * 'Cheat' for extending more than one class.
	 * Essentially with this HashDictionary extends
	 * both BaseModifiableDictionary AND BaseQueryableDictionary :)
	 */
	private final BaseQueryableDictionary innerDict;
	
	/**
	 * Ad-hoc implementation of BaseQueryableDictionary...
	 * ... because not possible ot make anonymous abstract class.
	 * 
	 * @author Eugen
	 *
	 */
	private class InnerDict extends BaseQueryableDictionary {

		public InnerDict(HDTSpecification spec) {
			super(spec);
			
			InnerDict.this.subjects = HashDictionary.this.subjects;
			InnerDict.this.predicates = HashDictionary.this.predicates;
			InnerDict.this.objects = HashDictionary.this.objects;
			InnerDict.this.shared = HashDictionary.this.shared;
		}

		@Override
		public void populateHeader(Header header, String rootNode) {
			throw new NotImplementedException();
		}

		@Override
		public String getType() {
			return HDTVocabulary.DICTIONARY_TYPE_PLAIN;
		}

		@Override
		public void load(Dictionary other, ProgressListener listener) {
			//TODO
			throw new NotImplementedException();
		}

		@Override
		public void save(OutputStream output, ControlInformation ci,
				ProgressListener listener) throws IOException {
			//TODO
			throw new NotImplementedException();
		}

		@Override
		public void load(InputStream input, ControlInformation ci,
				ProgressListener listener) throws IOException {
			//TODO
			throw new NotImplementedException();
		}
		
	}
	
	/**
	 * constructor...
	 */
	public HashDictionary(HDTSpecification spec) {
		super(spec);
		
		// FIXME: Read types from spec
		subjects = new HashDictionarySection();
		predicates = new HashDictionarySection();
		objects = new HashDictionarySection();
		shared = new HashDictionarySection();
		
		innerDict = new InnerDict(spec);
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
		Iterator<? extends CharSequence> itSubj = ((ModifiableDictionarySection) subjects).getEntries();
		while(itSubj.hasNext()) {
			CharSequence str = itSubj.next();
			mapSubj.add(str);
			
			// GENERATE SHARED at the same time
			if(str.length()>0 && str.charAt(0)!='"' && objects.locate(str)!=0) {
				((ModifiableDictionarySection)shared).add(str);
			}
		}
		System.out.println("Num shared: "+shared.getNumberOfElements()+" in "+st.stopAndShow());

		// Generate old predicate mapping
		st.reset();
		Iterator<? extends CharSequence> itPred = ((ModifiableDictionarySection) predicates).getEntries();
		while(itPred.hasNext()) {
			CharSequence str = itPred.next();
			mapPred.add(str);
		}		
		
		// Generate old object mapping
		Iterator<? extends CharSequence> itObj = ((ModifiableDictionarySection) objects).getEntries();
		while(itObj.hasNext()) {
			CharSequence str = itObj.next();
			mapObj.add(str);
		}
		
		// Remove shared from subjects and objects
		Iterator<? extends CharSequence> itShared = ((ModifiableDictionarySection) shared).getEntries();
		while(itShared.hasNext()) {
			CharSequence sharedStr = itShared.next();
			((ModifiableDictionarySection)subjects).remove(sharedStr);
			((ModifiableDictionarySection)objects).remove(sharedStr);
		}
		System.out.println("Mapping generated in "+st.stopAndShow());
		
		// Sort sections individually
		st.reset();
		((ModifiableDictionarySection)subjects).sort();
		((ModifiableDictionarySection)predicates).sort();
		((ModifiableDictionarySection)objects).sort();
		((ModifiableDictionarySection)shared).sort();
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
		while(iteratorTriples.hasNext()) {
			TripleID triple = iteratorTriples.next();
			triples.update(triple, 
					mapSubj.getNewID(triple.getSubject()-1),
					mapPred.getNewID(triple.getPredicate()-1),
					mapObj.getNewID(triple.getObject()-1)
			);
		}
		System.out.println("Replace IDs in "+st.stopAndShow());
		
		isOrganzied = true;
	}
	
	@Override
	public void startProcessing() {
		// Do nothing.
	}
	
	@Override
	public void endProcessing() {
		// Do nothing.
	}

	@Override
	public void close() throws IOException {
		// Do nothing.
	}
	
//--- Queryable methods ---------------------	
	
	@Override
	public void save(OutputStream output, ControlInformation ci,
			ProgressListener listener) throws IOException {
		innerDict.save(output, ci, listener);
	}

	@Override
	public void load(InputStream input, ControlInformation ci,
			ProgressListener listener) throws IOException {
		innerDict.load(input, ci, listener);
	}

	@Override
	public TripleString tripleIDtoTripleString(TripleID tripleID) {
		return innerDict.tripleIDtoTripleString(tripleID);
	}

	@Override
	public CharSequence idToString(int id, TripleComponentRole position) {
		return innerDict.idToString(id, position);
	}

	@Override
	public void populateHeader(Header header, String rootNode) {
		innerDict.populateHeader(header, rootNode);
	}

	@Override
	public String getType() {
		return innerDict.getType();
	}
}
