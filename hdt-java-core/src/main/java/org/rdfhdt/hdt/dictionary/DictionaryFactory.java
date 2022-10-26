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
import org.rdfhdt.hdt.options.HDTOptionsKeys;
import org.rdfhdt.hdt.options.HDTSpecification;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Factory that creates Dictionary objects
 */
public class DictionaryFactory {

	/**
	 * @deprecated use {@link org.rdfhdt.hdt.options.HDTOptionsKeys#TEMP_DICTIONARY_IMPL_VALUE_HASH} instead
	 */
	@Deprecated
	public static final String MOD_DICT_IMPL_HASH = HDTOptionsKeys.TEMP_DICTIONARY_IMPL_VALUE_HASH;
	/**
	 * @deprecated use {@link org.rdfhdt.hdt.options.HDTOptionsKeys#TEMP_DICTIONARY_IMPL_VALUE_MULT_HASH} instead
	 */
	@Deprecated
	public static final String MOD_DICT_IMPL_MULT_HASH = HDTOptionsKeys.TEMP_DICTIONARY_IMPL_VALUE_MULT_HASH;
	/**
	 * @deprecated use {@link org.rdfhdt.hdt.options.HDTOptionsKeys#TEMP_DICTIONARY_IMPL_VALUE_HASH_PSFC} instead
	 */
	@Deprecated
	public static final String MOD_DICT_IMPL_HASH_PSFC = HDTOptionsKeys.TEMP_DICTIONARY_IMPL_VALUE_HASH_PSFC;
	/**
	 * @deprecated use {@link org.rdfhdt.hdt.options.HDTOptionsKeys#DICTIONARY_TYPE_VALUE_FOUR_SECTION_BIG} instead
	 */
	@Deprecated
	public static final String DICTIONARY_TYPE_FOUR_SECTION_BIG = HDTOptionsKeys.DICTIONARY_TYPE_VALUE_FOUR_SECTION_BIG;
	/**
	 * @deprecated use {@link org.rdfhdt.hdt.options.HDTOptionsKeys#DICTIONARY_TYPE_VALUE_MULTI_OBJECTS} instead
	 */
	@Deprecated
	public static final String DICTIONARY_TYPE_MULTI_OBJECTS = HDTOptionsKeys.DICTIONARY_TYPE_VALUE_MULTI_OBJECTS;

	private DictionaryFactory() {
	}

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
	 * Creates a temp dictionary (allow insert)
	 *
	 * @param spec specs to read dictionary
	 * @return TempDictionary
	 */
	public static TempDictionary createTempDictionary(HDTOptions spec) {
		String name = Objects.requireNonNullElse(spec.get(HDTOptionsKeys.TEMP_DICTIONARY_IMPL_KEY), "");

		// Implementations available in the Core
		switch (name) {
			case "":
			case HDTOptionsKeys.TEMP_DICTIONARY_IMPL_VALUE_HASH:
				return new HashDictionary(spec, false);
			case HDTOptionsKeys.TEMP_DICTIONARY_IMPL_VALUE_HASH_PSFC:
				return new PSFCTempDictionary(new HashDictionary(spec, false));
			case HDTOptionsKeys.TEMP_DICTIONARY_IMPL_VALUE_MULT_HASH:
				return new HashDictionary(spec, true);
			default:
				throw new IllegalFormatException("Implementation of triples not found for " + name);
		}
	}

	/**
	 * Creates a dictionary
	 *
	 * @param spec specs to read dictionary
	 * @return Dictionary
	 */
	public static DictionaryPrivate createDictionary(HDTOptions spec) {
		String name = Objects.requireNonNullElse(spec.get(HDTOptionsKeys.DICTIONARY_TYPE_KEY), "");
		switch (name) {
			case "":
			case HDTOptionsKeys.DICTIONARY_TYPE_VALUE_FOUR_SECTION:
				return new FourSectionDictionary(spec);
			case HDTOptionsKeys.DICTIONARY_TYPE_VALUE_FOUR_PSFC_SECTION:
				return new PSFCFourSectionDictionary(spec);
			case HDTOptionsKeys.DICTIONARY_TYPE_VALUE_FOUR_SECTION_BIG:
				return new FourSectionDictionaryBig(spec);
			case HDTOptionsKeys.DICTIONARY_TYPE_VALUE_MULTI_OBJECTS:
				return new MultipleSectionDictionary(spec);
			default:
				throw new IllegalFormatException("Implementation of dictionary not found for " + name);
		}
	}

	/**
	 * Creates a write-dictionary
	 *
	 * @param spec       specs to read dictionary
	 * @param location   write location
	 * @param bufferSize write buffer sizes
	 * @return WriteDictionary
	 */
	public static DictionaryPrivate createWriteDictionary(HDTOptions spec, Path location, int bufferSize) {
		String name = Objects.requireNonNullElse(spec.get(HDTOptionsKeys.DICTIONARY_TYPE_KEY), "");
		switch (name) {
			case "":
			case HDTOptionsKeys.DICTIONARY_TYPE_VALUE_FOUR_SECTION:
			case HDTOptionsKeys.DICTIONARY_TYPE_VALUE_FOUR_SECTION_BIG:
				return new WriteFourSectionDictionary(spec, location, bufferSize);
			case HDTOptionsKeys.DICTIONARY_TYPE_VALUE_MULTI_OBJECTS:
				return new WriteMultipleSectionDictionary(spec, location, bufferSize);
			default:
				throw new IllegalFormatException("Implementation of write dictionary not found for " + name);
		}
	}

	/**
	 * Creates a dictionary
	 *
	 * @param ci specs to read dictionary
	 * @return Dictionary
	 */
	public static DictionaryPrivate createDictionary(ControlInfo ci) {
		String name = ci.getFormat();
		switch (name) {
			case HDTVocabulary.DICTIONARY_TYPE_FOUR_SECTION:
				return new FourSectionDictionary(new HDTSpecification());
			case HDTVocabulary.DICTIONARY_TYPE_FOUR_PSFC_SECTION:
				return new PSFCFourSectionDictionary(new HDTSpecification());
			case HDTVocabulary.DICTIONARY_TYPE_MULT_SECTION:
				return new MultipleSectionDictionary(new HDTSpecification());
			default:
				throw new IllegalFormatException("Implementation of dictionary not found for " + name);
		}
	}

	/**
	 * create a {@link DictionaryDiff} to create diff of a HDT in a new location
	 *
	 * @param dictionary the hdt dictionary
	 * @param location   the location of the new dictionary
	 * @return dictionaryDiff
	 */
	public static DictionaryDiff createDictionaryDiff(Dictionary dictionary, String location) {
		String type = dictionary.getType();
		switch (type) {
			case HDTVocabulary.DICTIONARY_TYPE_FOUR_SECTION:
			case HDTVocabulary.DICTIONARY_TYPE_FOUR_PSFC_SECTION:
				return new FourSectionDictionaryDiff(location);
			case HDTVocabulary.DICTIONARY_TYPE_MULT_SECTION:
				return new MultipleSectionDictionaryDiff(location);
			default:
				throw new IllegalFormatException("Implementation of DictionaryDiff not found for " + type);
		}
	}
}
