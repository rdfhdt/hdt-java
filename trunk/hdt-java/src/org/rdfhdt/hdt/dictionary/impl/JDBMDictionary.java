/**
 * File: $HeadURL: http://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/dictionary/impl/HashDictionary.java $
 * Revision: $Rev: 17 $
 * Last modified: $Date: 2012-07-03 21:43:15 +0100 (Tue, 03 Jul 2012) $
 * Last modified by: $Author: mario.arias@gmail.com $
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import org.apache.jdbm.DB;
import org.apache.jdbm.DBMaker;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.dictionary.DictionarySection;
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

/**
 * @author mario.arias
 *
 */
public class JDBMDictionary extends BaseDictionary implements ModifiableDictionary {

	private DB db;

	public JDBMDictionary(HDTSpecification hdtSpec) {

		super(hdtSpec);
		db = setupDB(hdtSpec);

		// FIXME: Read stuff from properties
		subjects = new DictionarySectionJDBM(db, "subjects");
		predicates = new DictionarySectionJDBM(db, "predicates");
		objects = new DictionarySectionJDBM(db, "objects");
		shared = new DictionarySectionJDBM(db, "shared"); //TODO maybe DictionarySectionHash because small?
	}

	private DB setupDB(HDTSpecification dbSpec) {

		//FIXME read from specs...
		File folder = new File("DB");
		if (!folder.exists() && !folder.mkdir()){
			throw new RuntimeException("Unable to create DB folder...");
		}

		//FIXME read from specs...
		DBMaker db = DBMaker.openFile("DB/DBfile");
		db.closeOnExit();
		db.deleteFilesAfterClose();
		db.disableTransactions(); //more performance
		db.enableHardCache(); //db.enableMRUCache(); + db.setMRUCacheSize(cacheSize);
		
		return db.make();
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
	 * @see hdt.dictionary.Dictionary#reorganize(hdt.triples.ModifiableTriples)
	 */
	@Override
	public void reorganize(ModifiableTriples triples) {
		// this method is used in the one-pass way of working in which case
		// this kind of disc-backed dictionary should not be used because remapping
		// requires practically a copy of the dictionary which is very bad...
		// (it is ok for in-memory because then the copy is not made, only double
		// references to the same CharSequence objects)
		throw new NotImplementedException();
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

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#populateHeader(hdt.header.Header, java.lang.String)
	 */
	@Override
	public void populateHeader(Header header, String rootNode) {
		// TODO Auto-generated method stub

	}


	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#startProcessing()
	 */
	@Override
	public void startProcessing() {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void endProcessing() {

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
		//FIXME ... different type?
		return HDTVocabulary.DICTIONARY_TYPE_PLAIN;
	}

	@Override
	public void close() throws IOException {
		db.close();
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
