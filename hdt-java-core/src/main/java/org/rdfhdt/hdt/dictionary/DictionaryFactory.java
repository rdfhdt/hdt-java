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

import org.rdfhdt.hdt.dictionary.impl.FourQuadSectionDictionary;
import org.rdfhdt.hdt.dictionary.impl.FourSectionDictionary;
import org.rdfhdt.hdt.dictionary.impl.FourSectionDictionaryBig;
import org.rdfhdt.hdt.dictionary.impl.FourSectionDictionaryDiff;
import org.rdfhdt.hdt.dictionary.impl.HashDictionary;
import org.rdfhdt.hdt.dictionary.impl.HashQuadDictionary;
import org.rdfhdt.hdt.dictionary.impl.MultipleSectionDictionary;
import org.rdfhdt.hdt.dictionary.impl.MultipleSectionDictionaryDiff;
import org.rdfhdt.hdt.dictionary.impl.PSFCFourSectionDictionary;
import org.rdfhdt.hdt.dictionary.impl.PSFCTempDictionary;
import org.rdfhdt.hdt.dictionary.impl.WriteFourSectionDictionary;
import org.rdfhdt.hdt.dictionary.impl.WriteMultipleSectionDictionary;
import org.rdfhdt.hdt.dictionary.impl.kcat.FourSectionDictionaryKCat;
import org.rdfhdt.hdt.dictionary.impl.kcat.MultipleSectionDictionaryKCat;
import org.rdfhdt.hdt.exceptions.IllegalFormatException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.hdt.impl.diskimport.MultiSectionSectionCompressor;
import org.rdfhdt.hdt.hdt.impl.diskimport.SectionCompressor;
import org.rdfhdt.hdt.iterator.utils.AsyncIteratorFetcher;
import org.rdfhdt.hdt.listener.MultiThreadListener;
import org.rdfhdt.hdt.options.ControlInfo;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTOptionsKeys;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.util.io.CloseSuppressPath;
import org.rdfhdt.hdt.util.string.ByteString;

import java.nio.file.Path;
import java.util.TreeMap;

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
		String name = spec.get(HDTOptionsKeys.TEMP_DICTIONARY_IMPL_KEY, "");
		// Implementations available in the Core
		switch (name) {
			case "":
			case HDTOptionsKeys.TEMP_DICTIONARY_IMPL_VALUE_HASH:
				return new HashDictionary(spec, false);
			case HDTOptionsKeys.TEMP_DICTIONARY_IMPL_VALUE_HASH_QUAD:
				return new HashQuadDictionary(spec, false);
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
		String name = spec.get(HDTOptionsKeys.DICTIONARY_TYPE_KEY, "");
		switch (name) {
			case "":
			case HDTOptionsKeys.DICTIONARY_TYPE_VALUE_FOUR_SECTION:
				return new FourSectionDictionary(spec);
			case HDTOptionsKeys.DICTIONARY_TYPE_VALUE_FOUR_QUAD_SECTION:
				return new FourQuadSectionDictionary(spec);
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
		String name = spec.get(HDTOptionsKeys.DICTIONARY_TYPE_KEY, "");
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
	 * Creates a write-dictionary
	 *
	 * @param name       name of the HDT Dictionary type
	 * @param spec       specs to read dictionary
	 * @param location   write location
	 * @param bufferSize write buffer sizes
	 * @return WriteDictionary
	 */
	public static DictionaryPrivate createWriteDictionary(String name, HDTOptions spec, Path location, int bufferSize) {
		switch (name) {
			case "":
			case HDTVocabulary.DICTIONARY_TYPE_FOUR_SECTION:
				return new WriteFourSectionDictionary(spec, location, bufferSize);
			case HDTVocabulary.DICTIONARY_TYPE_MULT_SECTION:
				return new WriteMultipleSectionDictionary(spec, location, bufferSize);
			default:
				throw new IllegalFormatException("Implementation of write dictionary not found for " + name);
		}
	}

	public static SectionCompressor createSectionCompressor(HDTOptions spec, CloseSuppressPath baseFileName,
															AsyncIteratorFetcher<TripleString> source,
															MultiThreadListener listener, int bufferSize,
															long chunkSize, int k, boolean debugSleepKwayDict) {
		String name = spec.get(HDTOptionsKeys.DICTIONARY_TYPE_KEY, "");

		switch (name) {
			case "":
			case HDTOptionsKeys.DICTIONARY_TYPE_VALUE_FOUR_SECTION:
			case HDTOptionsKeys.DICTIONARY_TYPE_VALUE_FOUR_SECTION_BIG:
				return new SectionCompressor(baseFileName, source, listener, bufferSize, chunkSize, k, debugSleepKwayDict);
			case HDTOptionsKeys.DICTIONARY_TYPE_VALUE_MULTI_OBJECTS:
				return new MultiSectionSectionCompressor(baseFileName, source, listener, bufferSize, chunkSize, k, debugSleepKwayDict);
			default:
				throw new IllegalFormatException("Implementation of section compressor not found for " + name);
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
			case HDTVocabulary.DICTIONARY_TYPE_FOUR_QUAD_SECTION:
				return new FourQuadSectionDictionary(new HDTSpecification());
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

	/**
	 * create {@link org.rdfhdt.hdt.dictionary.DictionaryKCat} for HDTCat
	 *
	 * @param dictionary dictionary
	 * @return dictionaryKCat
	 */
	public static DictionaryKCat createDictionaryKCat(Dictionary dictionary) {
		String type = dictionary.getType();
		switch (type) {
			case HDTVocabulary.DICTIONARY_TYPE_FOUR_SECTION:
				return new FourSectionDictionaryKCat(dictionary);
			case HDTVocabulary.DICTIONARY_TYPE_MULT_SECTION:
				return new MultipleSectionDictionaryKCat(dictionary);
			default:
				throw new IllegalArgumentException("Implementation of DictionaryKCat not found for " + type);
		}
	}

	public static DictionaryPrivate createWriteDictionary(
			String type,
			HDTOptions spec,
			DictionarySectionPrivate subject,
			DictionarySectionPrivate predicate,
			DictionarySectionPrivate object,
			DictionarySectionPrivate shared,
			TreeMap<ByteString, DictionarySectionPrivate> sub
	) {
		switch (type) {
			case HDTVocabulary.DICTIONARY_TYPE_FOUR_SECTION:
			case HDTVocabulary.DICTIONARY_TYPE_FOUR_PSFC_SECTION:
				return new WriteFourSectionDictionary(spec, subject, predicate, object, shared);
			case HDTVocabulary.DICTIONARY_TYPE_MULT_SECTION:
				return new WriteMultipleSectionDictionary(spec, subject, predicate, shared, sub);
			default:
				throw new IllegalArgumentException("Unknown dictionary type " + type);
		}
	}
}
