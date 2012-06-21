/**
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
/**
 * 
 */
package hdt.dictionary;

import hdt.dictionary.impl.HashDictionary;
import hdt.dictionary.impl.SectionDictionary;
import hdt.hdt.HDTVocabulary;
import hdt.options.ControlInformation;
import hdt.options.HDTSpecification;

/**
 * Factory that creates Dictionary objects
 * 
 */
public class DictionaryFactory {

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
	public static ModifiableDictionary createModifiableDictionary()
			throws IllegalArgumentException {
		return new HashDictionary(new HDTSpecification());
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
	private static Dictionary create(String name) {
		if(HDTVocabulary.DICTIONARY_TYPE_PFC.equals(name)) {
			return new SectionDictionary(new HDTSpecification());
		} else if(HDTVocabulary.DICTIONARY_TYPE_PLAIN.equals(name)) {
			return new HashDictionary(new HDTSpecification());
		} else {
			return new SectionDictionary(new HDTSpecification());
		}
	}
}
