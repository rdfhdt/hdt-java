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
import org.rdfhdt.hdt.dictionary.impl.section.JDBMDictionarySection;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.util.ProfilingUtil;

/**
 * This class is an implementation of a modifiable dictionary that
 * uses dictionary sections that store entries on disc using a
 * JDBM key-value database.
 * 
 * This class is responsible for creating and cleaning up the
 * database (environment of the maps). All parameters of the database
 * are set up here (most importantly cache).
 * 
 * @author Eugen
 *
 */
public class JDBMDictionary extends BaseTempDictionary {

	private DB db;

	public JDBMDictionary(HDTOptions spec) {

		super(spec);
		db = setupDB(spec);

		subjects = new JDBMDictionarySection(spec, db, "subjects");
		predicates = new JDBMDictionarySection(spec, db, "predicates");
		objects = new JDBMDictionarySection(spec, db, "objects");
		shared = new JDBMDictionarySection(spec, db, "shared");
	}

	private int getCacheSize(HDTOptions spec) {
		long size = (int) ProfilingUtil.parseSize(spec.get("tempDictionary.cache"));
		if(size==-1) {
			size = 64*1024*1024;
		}
		return (int) (size/5500);
	}
	
	private DB setupDB(HDTOptions spec) {

		File folder = new File("DB");
		if (!folder.exists() && !folder.mkdir()){
			throw new RuntimeException("Unable to create DB folder...");
		}
		File path = new File(folder.getPath()+"/dictionary");
		path.delete();

		DBMaker db = DBMaker.openFile(path.getAbsolutePath());
		db.closeOnExit();
		db.deleteFilesAfterClose();
		db.disableTransactions(); //more performance
	
		db.setMRUCacheSize(getCacheSize(spec));
		
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
}
