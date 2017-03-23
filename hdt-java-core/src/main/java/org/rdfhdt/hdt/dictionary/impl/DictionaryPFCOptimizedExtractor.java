package org.rdfhdt.hdt.dictionary.impl;

import org.rdfhdt.hdt.dictionary.impl.section.PFCDictionarySectionMap;
import org.rdfhdt.hdt.dictionary.impl.section.PFCOptimizedExtractor;
import org.rdfhdt.hdt.enums.TripleComponentRole;

public class DictionaryPFCOptimizedExtractor {
	private final PFCOptimizedExtractor shared, subjects, predicates, objects;
	private final int numshared;
	
	public DictionaryPFCOptimizedExtractor(FourSectionDictionary origDict) {
		numshared=(int) origDict.getNshared();
		shared = new PFCOptimizedExtractor((PFCDictionarySectionMap) origDict.shared);
		subjects = new PFCOptimizedExtractor((PFCDictionarySectionMap) origDict.subjects);
		predicates = new PFCOptimizedExtractor((PFCDictionarySectionMap) origDict.predicates);
		objects = new PFCOptimizedExtractor((PFCDictionarySectionMap) origDict.objects);
	}

	public CharSequence idToString(int id, TripleComponentRole role) {
		PFCOptimizedExtractor section = getSection(id, role);
		int localId = getLocalId(id, role);
		return section.extract(localId);
	}
	
	private PFCOptimizedExtractor getSection(int id, TripleComponentRole role) {
		switch (role) {
		case SUBJECT:
			if(id<=numshared) {
				return shared;
			} else {
				return subjects;
			}
		case PREDICATE:
			return predicates;
		case OBJECT:
			if(id<=numshared) {
				return shared;
			} else {
				return objects;
			}
		}
		throw new IllegalArgumentException();
	}

	
	private int getLocalId(int id, TripleComponentRole position) {
		switch (position) {
		case SUBJECT:
		case OBJECT:
			if(id<=numshared) {
				return id;
			} else {
				return id-numshared;
			}
		case PREDICATE:
			return id;
		}

		throw new IllegalArgumentException();
	}
}
