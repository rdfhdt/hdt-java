package org.rdfhdt.hdt.dictionary.impl;

import java.io.File;
import java.io.IOException;

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
		subjects = new DictionarySectionBerkeley(env, "subjects");
		predicates = new DictionarySectionBerkeley(env, "predicates");
		objects = new DictionarySectionBerkeley(env, "objects");
		shared = new DictionarySectionBerkeley(env, "shared");
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
		//TODO read from specs... ? (or fixed in percent? the Xmx is given anyway manually outside...)
		envConf.setCachePercentVoid(25); //envConf.setCacheSizeVoid(totalBytes);
		
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

	@Override
	public void close() throws IOException {
		
		((DictionarySectionBerkeley)subjects).cleanup();
		((DictionarySectionBerkeley)predicates).cleanup();
		((DictionarySectionBerkeley)objects).cleanup();
		((DictionarySectionBerkeley)shared).cleanup();
		
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
	
	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#getType()
	 */
	@Override
	public String getType() {
		//FIXME ... different type?
		return HDTVocabulary.DICTIONARY_TYPE_PLAIN;
	}
}
