package org.rdfhdt.hdt.triples;

import org.rdfhdt.hdt.compact.bitmap.Bitmap;
import org.rdfhdt.hdt.compact.bitmap.ModifiableBitmap;

import java.util.Map;

public interface BitmapTriplesIteratorDiffBit {
    /**
     * @return the bitmaps
     */
    Map<String, ModifiableBitmap> getBitmaps();

    /**
     * create the bitmaps
     */
    void loadBitmaps();

    /**
     * @return the count
     */
    long getCount();
}
