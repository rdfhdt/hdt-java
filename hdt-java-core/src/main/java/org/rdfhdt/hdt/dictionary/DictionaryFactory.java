/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/dictionary/DictionaryFactory.java $
 * Revision: $Rev: 191 $
 * Last modified: $Date: 2013-03-03 11:41:43 +0000 (dom, 03 mar 2013) $
 * Last modified by: $Author: mario.arias $
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 3.0 of the License.
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

import org.rdfhdt.hdt.dictionary.impl.*;
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
	public static final String MOD_DICT_IMPL_MULT_HASH = "multHash";
	public static final String MOD_DICT_IMPL_HASH_PSFC = "hashPsfc";
	public static final String DICTIONARY_TYPE_FOUR_SECTION_BIG ="dictionaryFourBig";
	public static final String DICTIONARY_TYPE_MULTI_OBJECTS = "dictionaryMultiObj";
	private DictionaryFactory() {}

	/**
	 * Creates a default dictionary (HashDictionary)
	 *
	 * @return Dictionary
	 */
	public static Dictionary createDefaultDictionary()
			throws IllegalArgumentException {
		return new FourSectionDictionary(new HDTSpecification());
	}

	/**
	 * Creates a default dictionary (HashDictionary)
	 *
	 * @return Dictionary
	 */
	public static TempDictionary createTempDictionary(HDTOptions spec) {
		String name = spec.get("tempDictionary.impl");

		// Implementations available in the Core
		if(name==null || "".equals(name) || MOD_DICT_IMPL_HASH.equals(name)) {
			return new HashDictionary(spec,false);
		} else if(MOD_DICT_IMPL_HASH_PSFC.equals(name)){
			return new PSFCTempDictionary(new HashDictionary(spec,false));
		} else if(MOD_DICT_IMPL_MULT_HASH.equals(name)){
			return new HashDictionary(spec,true);
		}
		throw new IllegalFormatException("Implementation of triples not found for "+name);
	}

	public static DictionaryPrivate createDictionary(HDTOptions spec) {
		String name = spec.get("dictionary.type");
		if(name==null || HDTVocabulary.DICTIONARY_TYPE_FOUR_SECTION.equals(name)) {
			return new FourSectionDictionary(spec);
		}
		else if (HDTVocabulary.DICTIONARY_TYPE_FOUR_PSFC_SECTION.equals(name)){
			return new PSFCFourSectionDictionary(spec);
		}
		else if (DICTIONARY_TYPE_FOUR_SECTION_BIG.equals(name)){
			return new FourSectionDictionaryBig(spec);
		}else if ((DICTIONARY_TYPE_MULTI_OBJECTS.equals(name))){
			return new MultipleSectionDictionary(spec);
		}
		throw new IllegalFormatException("Implementation of dictionary not found for "+name);
	}

	public static DictionaryPrivate createDictionary(ControlInfo ci) {
		String name = ci.getFormat();
		if(HDTVocabulary.DICTIONARY_TYPE_FOUR_SECTION.equals(name)) {
			return new FourSectionDictionary(new HDTSpecification());
		} else if (HDTVocabulary.DICTIONARY_TYPE_FOUR_PSFC_SECTION.equals(name)) {
			return new PSFCFourSectionDictionary(new HDTSpecification());
		} else if(HDTVocabulary.DICTIONARY_TYPE_MULT_SECTION.equals(name)){
			return new MultipleSectionDictionary(new HDTSpecification());
		}
		throw new IllegalFormatException("Implementation of dictionary not found for "+name);
	}

	/**
	 * create a {@link DictionaryDiff} to create diff of a HDT in a new location
	 * @param dictionary the hdt dictionary
	 * @param location the location of the new dictionary
	 * @return dictionaryDiff
	 */
	public static DictionaryDiff createDictionaryDiff(Dictionary dictionary, String location) {
		String type = dictionary.getType();
		if (HDTVocabulary.DICTIONARY_TYPE_FOUR_SECTION.equals(type) || type.equals(HDTVocabulary.DICTIONARY_TYPE_FOUR_PSFC_SECTION))
			return new FourSectionDictionaryDiff(location);
		else if (type.equals(HDTVocabulary.DICTIONARY_TYPE_MULT_SECTION))
			return new MultipleSectionDictionaryDiff(location);

		throw new IllegalFormatException("Implementation of DictionaryDiff not found for "+type);
	}
}
