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

import org.rdfhdt.hdt.dictionary.impl.section.KyotoDictionarySection;
import org.rdfhdt.hdt.options.HDTOptions;

/**
 * This class is an implementation of a modifiable dictionary that
 * uses dictionary sections that store entries on disc using a
 * KyotoCabinet key-value database (native, written in c++).
 * 
 * This class creates only the folder for the database, databases
 * are configured in each of the dictionary sections separately.
 * 
 * @author Eugen
 *
 */
public class KyotoDictionary extends BaseTempDictionary {
	
	public KyotoDictionary(HDTOptions spec) {

		super(spec);
		
		//FIXME read from specs...
		File homeDir = new File("DB");
		if (!homeDir.exists() && !homeDir.mkdir()){
			throw new RuntimeException("Unable to create DB folder...");
		}

		// FIXME: Read stuff from properties
		subjects = new KyotoDictionarySection(spec, homeDir, "subjects");
		predicates = new KyotoDictionarySection(spec, homeDir, "predicates");
		objects = new KyotoDictionarySection(spec, homeDir, "objects");
		shared = new KyotoDictionarySection(spec, homeDir, "shared");
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
		try {	
			((KyotoDictionarySection)subjects).cleanup();
			((KyotoDictionarySection)predicates).cleanup();
			((KyotoDictionarySection)objects).cleanup();
			((KyotoDictionarySection)shared).cleanup();
		} catch (Exception e){
			throw new RuntimeException("Closing of databases failed (most probably files left behind)", e);
		}
	}
}
