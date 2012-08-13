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

import org.rdfhdt.hdt.dictionary.impl.BerkeleyDBDictionary;
import org.rdfhdt.hdt.dictionary.impl.HashDictionary;
import org.rdfhdt.hdt.dictionary.impl.JDBMDictionary;
import org.rdfhdt.hdt.dictionary.impl.SectionDictionary;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.options.HDTSpecification;

/**
 * Factory that creates Dictionary objects
 * 
 */
public class DictionaryFactory {
	
	public static final String DICT_MOD_HASH = "hash";
	public static final String DICT_MOD_JDBM = "jdbm";
	public static final String DICT_MOD_BERKELEY = "berkeley";

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
		//FIXME ... dictionary.name as option... some values in HDTVocabulary... ?
		String dictName = spec.get("dictionary.name");
		//TODO switch-case can use String in 1.7 and after...
		if (DICT_MOD_HASH.equalsIgnoreCase(dictName)){
			return new HashDictionary(spec);
		} else if (DICT_MOD_JDBM.equalsIgnoreCase(dictName)){
			return new JDBMDictionary(spec);
		} else if (DICT_MOD_BERKELEY.equalsIgnoreCase(dictName)){
			return new BerkeleyDBDictionary(spec);
		}
		return new HashDictionary(spec);
	}
	
	public static Dictionary createDictionary(HDTSpecification spec) {
		return create(spec.get("dictionary.type"));
	}
	
	public static Dictionary createDictionary(ControlInformation ci) {
		return create(ci.get("codification"));
	}
	
	/**
	 * Creates a Dictionary
	 * 
	 * @param spec Specification of the required dictionary
	 * @return Dictionary
	 */
	//FIXME specs passed on...?
	private static Dictionary create(String name) {
		if(HDTVocabulary.DICTIONARY_TYPE_PFC.equals(name)) {
			return new SectionDictionary(new HDTSpecification());
		} else if(HDTVocabulary.DICTIONARY_TYPE_PLAIN.equals(name)) {
			//FIXME other versions...?
			return new HashDictionary(new HDTSpecification());
		}
		return new SectionDictionary(new HDTSpecification());
	}
}
