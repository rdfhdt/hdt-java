package org.rdfhdt.hdt.dictionary;

import org.rdfhdt.hdt.dictionary.impl.utilCat.CatMapping;
import org.rdfhdt.hdt.dictionary.impl.utilCat.CatMappingBack;
import org.rdfhdt.hdt.listener.ProgressListener;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;

public interface DictionaryCat extends Closeable {
    void cat(Dictionary dictionary1, Dictionary dictionary2, ProgressListener listener) throws IOException;
    CatMappingBack getMappingS();
    long getNumShared();
    HashMap<String, CatMapping> getAllMappings();
}
