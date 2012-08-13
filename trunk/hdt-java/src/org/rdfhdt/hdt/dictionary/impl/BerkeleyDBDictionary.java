package org.rdfhdt.hdt.dictionary.impl;

import java.io.File;
import java.io.IOException;

import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.options.HDTSpecification;

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
		shared = new DictionarySectionBerkeley(env, "shared"); //TODO maybe DictionarySectionHash because small?
	}
	
	private void setupDBEnvironment(HDTSpecification spec) {

		//FIXME read from specs...
		File folder = new File("DB");
		if (!folder.exists() && !folder.mkdir()){
			throw new RuntimeException("Unable to create DB folder...");
		}

		EnvironmentConfig envConf = new EnvironmentConfig();
		envConf.setAllowCreate(true);
		envConf.setTransactional(false);
		//TODO setup cache
		//envConf.setCacheMode(CacheMode.DEFAULT);
		//envConf.setCacheModeStrategy(CacheModeStrategy...);
		//envConf.setCachePercent(percent); envConf.setCacheSize(totalBytes);
		
		env = new Environment(folder, envConf);
	}

	@Override
	public void startProcessing() {
	}

	@Override
	public void endProcessing() {
	}

	@Override
	public void close() throws IOException {
		((DictionarySectionBerkeley)subjects).cleanup();
		((DictionarySectionBerkeley)predicates).cleanup();
		((DictionarySectionBerkeley)objects).cleanup();
		((DictionarySectionBerkeley)shared).cleanup();
		
		env.close();
		env = null;
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
