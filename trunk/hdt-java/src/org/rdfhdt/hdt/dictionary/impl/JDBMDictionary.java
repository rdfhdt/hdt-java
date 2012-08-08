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

import org.apache.jdbm.DB;
import org.apache.jdbm.DBMaker;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.options.HDTSpecification;

/**
 * @author mario.arias
 *
 */
public class JDBMDictionary extends BaseModifiableDictionary {

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

	@Override
	public void startProcessing() {
		// do nothing
	}
	
	@Override
	public void endProcessing() {
		// do nothing
	}
	
	@Override
	public void close() throws IOException {
		db.close();
	}
	
	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#getType()
	 */
	@Override
	public String getType() {
		//FIXME ... different type?
		return HDTVocabulary.DICTIONARY_TYPE_PLAIN;
	}
}
