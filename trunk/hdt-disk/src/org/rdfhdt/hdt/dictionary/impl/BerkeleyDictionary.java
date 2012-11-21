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

import org.rdfhdt.hdt.dictionary.DictionarySection;
import org.rdfhdt.hdt.dictionary.impl.section.BerkeleyDictionarySection;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInfo;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdtdisk.util.CacheCalculator;

import com.sleepycat.je.CacheMode;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

/**
 * This class is an implementation of a modifiable dictionary that
 * uses dictionary sections that store entries on disc using a
 * BerkeleyDB JE key-value database.
 * 
 * This class is responsible for creating and cleaning up the
 * environment of the databses. All parameters of the environment
 * are set up here (most importantly cache).
 * 
 * @author Eugen
 *
 */
public class BerkeleyDictionary extends BaseTempDictionary {

	private File envHome;
	private Environment env;

	public BerkeleyDictionary(HDTSpecification spec) {

		super(spec);
		setupDBEnvironment(spec);

		// FIXME: Read stuff from properties
		try {
			subjects = new BerkeleyDictionarySection(spec, env, "subjects");
			predicates = new BerkeleyDictionarySection(spec, env, "predicates");
			objects = new BerkeleyDictionarySection(spec, env, "objects");
			shared = new BerkeleyDictionarySection(spec, env, "shared");
		} catch (Exception e){
			cleanupEnvironment();
			throw new RuntimeException(e);
		}
	}

	private void setupDBEnvironment(HDTSpecification spec) {

		//FIXME read from specs...
		envHome = new File("DB/dictionary");
		if (!envHome.exists() && !envHome.mkdir()){
			throw new RuntimeException("Unable to create DB folder...");
		}

		EnvironmentConfig envConf = new EnvironmentConfig();
		envConf.setAllowCreateVoid(true);
		envConf.setTransactionalVoid(false);
		envConf.setLockingVoid(false);
		envConf.setCacheModeVoid(CacheMode.DEFAULT);
		envConf.setCacheSizeVoid(CacheCalculator.getDictionaryCache(spec));

		env = new Environment(envHome, envConf);

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				BerkeleyDictionary.this.close();
				for (File f : envHome.listFiles()){
					String fname = f.getName();
					if (fname.equalsIgnoreCase("je.properties"))
						continue;
					if (fname.endsWith(".jdb") || fname.startsWith("je."))
						f.delete();
				}
			}
		}));
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
		
		if (env!=null){
			env.close();
			env = null;
		}
	}

	@Override
	public void close() {

		((BerkeleyDictionarySection)subjects).cleanup();
		((BerkeleyDictionarySection)predicates).cleanup();
		((BerkeleyDictionarySection)objects).cleanup();
		((BerkeleyDictionarySection)shared).cleanup();

		cleanupEnvironment();

	}
}
