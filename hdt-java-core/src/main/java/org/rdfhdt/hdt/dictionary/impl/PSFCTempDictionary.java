package org.rdfhdt.hdt.dictionary.impl;

import org.rdfhdt.hdt.dictionary.TempDictionary;
import org.rdfhdt.hdt.dictionary.TempDictionarySection;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.triples.TempTriples;

import java.io.IOException;

public class PSFCTempDictionary implements TempDictionary {
    private final TempDictionary delegate;

    public PSFCTempDictionary(TempDictionary delegate) {
        this.delegate = delegate;
    }

    @Override
    public int stringToId(CharSequence subject, TripleComponentRole role) {
        return delegate.stringToId(PSFCFourSectionDictionary.encode(subject), role);
    }

    @Override
    public int insert(CharSequence str, TripleComponentRole position) {
        return delegate.insert(PSFCFourSectionDictionary.encode(str), position);
    }

    @Override public TempDictionarySection getSubjects() { return delegate.getSubjects(); }
    @Override public TempDictionarySection getPredicates() { return delegate.getPredicates(); }
    @Override public TempDictionarySection getObjects() { return delegate.getObjects(); }
    @Override public TempDictionarySection getShared() { return delegate.getShared(); }
    @Override public void startProcessing() { delegate.startProcessing(); }
    @Override public void endProcessing() { delegate.endProcessing(); }
    @Override public boolean isOrganized() { return delegate.isOrganized(); }
    @Override public void reorganize() { delegate.reorganize(); }
    @Override public void reorganize(TempTriples triples) { delegate.reorganize(triples); }
    @Override public void clear() { delegate.clear(); }
    @Override public void close() throws IOException { delegate.close(); }
}
