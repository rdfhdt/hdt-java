package org.rdfhdt.hdt.dictionary;

import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleString;

public class DictionaryUtil {
	/**
	 * Converts a TripleID to a TripleString using a dictionary.
	 * 
	 * @param tripleID
	 *            The Triple to convert from
	 * @return TripleString
	 */
	public static TripleString tripleIDtoTripleString(Dictionary dict, TripleID tripleID) {
		return new TripleString(
				dict.idToString(tripleID.getSubject(), TripleComponentRole.SUBJECT), 
				dict.idToString(tripleID.getPredicate(), TripleComponentRole.PREDICATE), 
				dict.idToString(tripleID.getObject(), TripleComponentRole.OBJECT)
				);
	}
	
	/**
	 * Converts a TripleString to a TripleID using a dictionary.
	 * 
	 * @param tripleString
	 *            The Triple to convert from
	 * @return TripleID
	 */
	public static TripleID tripleStringtoTripleID(Dictionary dict, TripleString tripleString) {
		return new TripleID(
				dict.stringToId(tripleString.getSubject(), TripleComponentRole.SUBJECT), 
				dict.stringToId(tripleString.getPredicate(), TripleComponentRole.PREDICATE), 
				dict.stringToId(tripleString.getObject(), TripleComponentRole.OBJECT)
				);
	}
}
