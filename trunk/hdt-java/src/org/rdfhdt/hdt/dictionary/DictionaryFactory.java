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

import org.rdfhdt.hdt.dictionary.impl.FourSectionDictionary;
import org.rdfhdt.hdt.dictionary.impl.HashDictionary;
import org.rdfhdt.hdt.exceptions.IllegalFormatException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.options.ControlInfo;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTSpecification;

/**
 * Factory that creates Dictionary objects
 * 
 */
public class DictionaryFactory {

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
		return new FourSectionDictionary(new HDTSpecification());
	}

//	private static void requireTwoPass(HDTOptions spec) {
//		String loaderType = spec.get("loader.type");
//		if (!"two-pass".equals(loaderType)){
//			String errmsg = "tempTriples.impl cannot be \"set\" if loader.type is not set to two-pass!";
//			System.err.println(errmsg);
//			throw new RuntimeException(errmsg);
//		}
//	}
	/**
	 * Creates a default dictionary (HashDictionary)
	 * 
	 * @return Dictionary
	 */
	public static TempDictionary createTempDictionary(HDTOptions spec) {
		String dictImpl = spec.get("tempDictionary.impl");
		
		//TODO switch-case can use String in 1.7 and after...
		if (MOD_DICT_IMPL_HASH.equals(dictImpl)){
			return new HashDictionary(spec);
//		} else	if (MOD_DICT_IMPL_JDBM.equals(dictImpl)) {
//			requireTwoPass(spec);
//			return new JDBMDictionary(spec);
//		} else if (MOD_DICT_IMPL_BERKELEY.equals(dictImpl)) {
//			requireTwoPass(spec);
//			return new BerkeleyDictionary(spec);
//			// FIXME: Disabled until we can fix it.
//		} else if (MOD_DICT_IMPL_BERKELEY_NATIVE.equals(dictImpl)) {
//			//return new BerkeleyNativeDictionary(spec);
//		} else if (MOD_DICT_IMPL_KYOTO.equals(dictImpl)) {
//			requireTwoPass(spec);
//			return new KyotoDictionary(spec);
		} else {
			//System.err.println("Dictionary type not specified, using in-memory hash.");
			spec.set("tempDictionary.impl", MOD_DICT_IMPL_HASH);
			return new HashDictionary(spec);
		}
	}
	
	public static DictionaryPrivate createDictionary(HDTOptions spec) {
		String name = spec.get("dictionary.type");
		if(name==null || HDTVocabulary.DICTIONARY_TYPE_FOUR_SECTION.equals(name)) {
			return new FourSectionDictionary(spec);
		}
		throw new IllegalFormatException("Implementation of ditionary not found for "+name);
	}
	
	public static DictionaryPrivate createDictionary(ControlInfo ci) {
		String name = ci.getFormat();
		if(HDTVocabulary.DICTIONARY_TYPE_FOUR_SECTION.equals(name)) {
			return new FourSectionDictionary(new HDTSpecification());
		}
		throw new IllegalFormatException("Implementation of ditionary not found for "+name);
	}
}
