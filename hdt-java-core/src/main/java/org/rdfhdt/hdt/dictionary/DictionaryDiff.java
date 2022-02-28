package org.rdfhdt.hdt.dictionary;

import org.rdfhdt.hdt.compact.bitmap.ModifiableBitmap;
import org.rdfhdt.hdt.dictionary.impl.utilCat.CatMapping;
import org.rdfhdt.hdt.listener.ProgressListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public interface DictionaryDiff {
    /**
     * compute the diff of the previous dictionary
     * @param dictionary previous dictionary
     * @param bitmaps the bitmap for each sections
     * @param listener listener to get the progress
     * @throws IOException io error
     */
    void diff(Dictionary dictionary, Map<String, ModifiableBitmap> bitmaps, ProgressListener listener) throws IOException;

    /**
     * @return the CatMapping of the diff
     */
    CatMapping getMappingBack();

    /**
     * @return the new number of shared element
     */
    long getNumShared();

    /**
     * @return the cat mapping for each section
     */
    HashMap<String, CatMapping> getAllMappings();
}
