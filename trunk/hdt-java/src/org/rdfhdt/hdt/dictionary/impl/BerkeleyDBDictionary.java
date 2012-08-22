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

import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.options.HDTSpecification;

import com.sleepycat.je.CacheMode;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

public class BerkeleyDBDictionary extends BaseModifiableDictionary {

	private Environment env;

	public BerkeleyDBDictionary(HDTSpecification spec) {

		super(spec);
		setupDBEnvironment(spec);

		// FIXME: Read stuff from properties
		try {
			subjects = new DictionarySectionBerkeley(spec, env, "subjects");
			predicates = new DictionarySectionBerkeley(spec, env, "predicates");
			objects = new DictionarySectionBerkeley(spec, env, "objects");
			shared = new DictionarySectionBerkeley(spec, env, "shared");
		} catch (Exception e){
			//TODO something smarter??
			cleanupEnvironment();
			throw new RuntimeException(e);
		}
	}

	private void setupDBEnvironment(HDTSpecification spec) {

		//FIXME read from specs...
		File folder = new File("DB");
		if (!folder.exists() && !folder.mkdir()){
			throw new RuntimeException("Unable to create DB folder...");
		}

		EnvironmentConfig envConf = new EnvironmentConfig();
		envConf.setAllowCreateVoid(true);
		envConf.setTransactionalVoid(false);
		envConf.setCacheModeVoid(CacheMode.DEFAULT);
		int cachePercent = 25; //TODO read from specs... ?
		envConf.setCachePercentVoid(cachePercent);

		env = new Environment(folder, envConf);
	}

	@Override
	public void startProcessing() {
		//do nothing
	}

	@Override
	public void endProcessing() {
		//do nothing
	}

	/**
	 * Closes the environment and cleans up the environment home folder.
	 * 
	 * Throws DatabaseException (runtime) if unable to close Environment (no need for explicit handling)
	 */
	private void cleanupEnvironment() {

		File envHome = env.getHome();
		env.cleanLog();
		env.close();
		env = null;

		//TODO cleanup DB folder manually like this, or not? ??
		for (File f : envHome.listFiles()){
			String fname = f.getName();
			if (fname.equalsIgnoreCase("je.properties"))
				continue;
			if (fname.endsWith(".jdb") || fname.startsWith("je."))
				f.delete();
		}
	}

	@Override
	public void close() {

		try {	
			((DictionarySectionBerkeley)subjects).cleanup();
			((DictionarySectionBerkeley)predicates).cleanup();
			((DictionarySectionBerkeley)objects).cleanup();
			((DictionarySectionBerkeley)shared).cleanup();
		} catch (Exception e){
			cleanupEnvironment();
			throw new RuntimeException("Closing of databases failed (most probably files left behind)", e);
		}

		if (env!=null) //in some wierd case the above cleanupEnvironment() was called and the execution still came here...
			cleanupEnvironment();

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
