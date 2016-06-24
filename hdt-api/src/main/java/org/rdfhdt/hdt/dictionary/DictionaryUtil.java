package org.rdfhdt.hdt.dictionary;

import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleString;

public class DictionaryUtil {
	
	private DictionaryUtil() {}
	
	/**
	 * Converts a TripleID to a TripleString using a dictionary.
	 * 
	 * @param tripleID
	 *            The Triple to convert from
	 * @return TripleString
	 */
	public static TripleString tripleIDtoTripleString(Dictionary dict, TripleID tripleID) {
		return new TripleString(
				dict.idToString(tripleID.getSubject(), TripleComponentRole.SUBJECT).toString(), 
				dict.idToString(tripleID.getPredicate(), TripleComponentRole.PREDICATE).toString(), 
				dict.idToString(tripleID.getObject(), TripleComponentRole.OBJECT).toString()
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
				dict.stringToId(tripleString.getSubject().toString(), TripleComponentRole.SUBJECT), 
				dict.stringToId(tripleString.getPredicate().toString(), TripleComponentRole.PREDICATE), 
				dict.stringToId(tripleString.getObject().toString(), TripleComponentRole.OBJECT)
				);
	}
}
