package org.rdfhdt.hdt.triples;

import org.rdfhdt.hdt.compact.bitmap.Bitmap;
import org.rdfhdt.hdt.compact.bitmap.ModifiableBitmap;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.exceptions.IllegalFormatException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.triples.impl.BitmapTriplesIteratorDiffBitImpl;
import org.rdfhdt.hdt.triples.impl.BitmapTriplesIteratorDiffBitLiterals;

import java.util.Map;

public interface BitmapTriplesIteratorDiffBit {
    static BitmapTriplesIteratorDiffBit createForType(Dictionary dictionary, HDT hdt, Bitmap deleteBitmap, IteratorTripleID hdtIterator) {
        String type = dictionary.getType();
        if (HDTVocabulary.DICTIONARY_TYPE_FOUR_SECTION.equals(type) || type.equals(HDTVocabulary.DICTIONARY_TYPE_FOUR_PSFC_SECTION))
            return new BitmapTriplesIteratorDiffBitImpl(hdt, deleteBitmap, hdtIterator);
        if (type.equals(HDTVocabulary.DICTIONARY_TYPE_MULT_SECTION))
            return new BitmapTriplesIteratorDiffBitLiterals(hdt, deleteBitmap, hdtIterator);

        throw new IllegalFormatException("Implementation of BitmapTriplesIteratorDiffBit not found for "+type);
    }
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
