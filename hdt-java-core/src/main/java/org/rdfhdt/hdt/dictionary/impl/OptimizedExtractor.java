package org.rdfhdt.hdt.dictionary.impl;

import org.rdfhdt.hdt.enums.TripleComponentRole;

public interface OptimizedExtractor {
    CharSequence idToString(long id, TripleComponentRole role);
}
