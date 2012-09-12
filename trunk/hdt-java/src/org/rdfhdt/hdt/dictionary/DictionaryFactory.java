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

package org.rdfhdt.hdt.dictionary;

import org.rdfhdt.hdt.dictionary.impl.BerkeleyDictionary;
import org.rdfhdt.hdt.dictionary.impl.BerkeleyNativeDictionary;
import org.rdfhdt.hdt.dictionary.impl.HashDictionary;
import org.rdfhdt.hdt.dictionary.impl.JDBMDictionary;
import org.rdfhdt.hdt.dictionary.impl.KyotoDictionary;
import org.rdfhdt.hdt.dictionary.impl.SectionDictionary;
import org.rdfhdt.hdt.hdt.HDTFactory;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.options.HDTSpecification;

/**
 * Factory that creates Dictionary objects
 * 
 */
public class DictionaryFactory {
	
	public static final String MOD_DICT_TYPE_IN_MEM = "in-memory";
	public static final String MOD_DICT_TYPE_ON_DISC = "on-disc";
	
	public static final String MOD_DICT_IMPL_HASH = "hash";
	public static final String MOD_DICT_IMPL_JDBM = "jdbm";
	public static final String MOD_DICT_IMPL_BERKELEY = "berkeleyJE";
	public static final String MOD_DICT_IMPL_BERKELEY_NATIVE = "berkeley";
	public static final String MOD_DICT_IMPL_KYOTO = "kyoto";

	/**
	 * Creates a default dictionary (HashDictionary)
	 * 
	 * @return Dictionary
	 */
	public static Dictionary createDefaultDictionary()
			throws IllegalArgumentException {
		return new SectionDictionary(new HDTSpecification());
	}

	/**
	 * Creates a default dictionary (HashDictionary)
	 * 
	 * @return Dictionary
	 */
	public static ModifiableDictionary createModifiableDictionary(HDTSpecification spec)
			throws IllegalArgumentException {
		
		String dictType = spec.get("tempDictionary.type");
		String dictImpl = spec.get("tempDictionary.impl");
		String loaderType = spec.get("loader.type");
		
		//TODO switch-case can use String in 1.7 and after...
		if (MOD_DICT_TYPE_IN_MEM.equals(dictType)){
			if (MOD_DICT_IMPL_HASH.equals(dictImpl)){
				return new HashDictionary(spec);
			} else {
				System.err.println("Unknown in-memory dictionary implementation, using hash.");
				spec.set("tempDictionary.impl", MOD_DICT_IMPL_HASH);
				return new HashDictionary(spec);
			}
		} else if (MOD_DICT_TYPE_ON_DISC.equals(dictType)) {
			if (!HDTFactory.LOADER_TWO_PASS.equals(loaderType)){
				String errmsg = "tempTriples.impl cannot be \"set\" if loader.type is not set to two-pass!";
				System.err.println(errmsg);
				throw new RuntimeException(errmsg);
			}
			if (MOD_DICT_IMPL_JDBM.equals(dictImpl)) {
				return new JDBMDictionary(spec);
			} else if (MOD_DICT_IMPL_BERKELEY.equals(dictImpl)) {
				return new BerkeleyDictionary(spec);
			} else if (MOD_DICT_IMPL_BERKELEY_NATIVE.equals(dictImpl)) {
				return new BerkeleyNativeDictionary(spec);
			} else if (MOD_DICT_IMPL_KYOTO.equals(dictImpl)) {
				return new KyotoDictionary(spec);
			} else {
				System.err.println("Unknown on-disc dictionary implementation, using jdbm.");
				spec.set("tempDictionary.impl", MOD_DICT_IMPL_JDBM);
				return new JDBMDictionary(spec);
			} 
		} else {
			System.err.println("Unknown dictionary type, using in-memory hash.");
			spec.set("tempDictionary.type", MOD_DICT_TYPE_IN_MEM);
			spec.set("tempDictionary.impl", MOD_DICT_IMPL_HASH);
			return new HashDictionary(spec);
		}
	}
	
	public static QueryableDictionary createDictionary(HDTSpecification spec) {
		return create(spec.get("dictionary.type"));
	}
	
	public static QueryableDictionary createDictionary(ControlInformation ci) {
		return create(ci.get("codification"));
	}
	
	/**
	 * Creates a Dictionary
	 * 
	 * @param specs Specification of the required dictionary
	 * @return Dictionary
	 */
	//FIXME specs passed on...?
	private static QueryableDictionary create(String name) {
		if(HDTVocabulary.DICTIONARY_TYPE_PFC.equals(name)) {
			return new SectionDictionary(new HDTSpecification());
		} else if(HDTVocabulary.DICTIONARY_TYPE_PLAIN.equals(name)) {
			return new HashDictionary(new HDTSpecification());
		}
		return new SectionDictionary(new HDTSpecification());
	}
}
