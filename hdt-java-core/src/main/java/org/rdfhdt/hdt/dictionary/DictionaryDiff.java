package org.rdfhdt.hdt.dictionary;

import org.rdfhdt.hdt.compact.bitmap.ModifiableBitmap;
import org.rdfhdt.hdt.dictionary.impl.utilCat.CatMapping;
import org.rdfhdt.hdt.listener.ProgressListener;

import java.util.HashMap;
import java.util.Map;

public interface DictionaryDiff {
    void diff(Dictionary dictionary, Map<String, ModifiableBitmap> bitmaps, ProgressListener listener);
    CatMapping getMappingBack();
    long getNumShared();
    HashMap<String, CatMapping> getAllMappings();
}
