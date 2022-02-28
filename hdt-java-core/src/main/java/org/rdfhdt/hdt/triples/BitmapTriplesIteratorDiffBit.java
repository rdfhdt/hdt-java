package org.rdfhdt.hdt.triples;

import org.rdfhdt.hdt.compact.bitmap.Bitmap;
import org.rdfhdt.hdt.compact.bitmap.ModifiableBitmap;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.exceptions.IllegalFormatException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.triples.impl.FourSectionBitmapTriplesIteratorDiffBit;
import org.rdfhdt.hdt.triples.impl.MultipleSectionBitmapTriplesIteratorDiffBit;

import java.util.Map;

/**
 * Class to build the bitmaps for each section of a Dictionary
 */
public interface BitmapTriplesIteratorDiffBit {
    /**
     * create a {@link BitmapTriplesIteratorDiffBit} for a particular dictionary type
     * @param dictionary the dictionary to get the type
     * @param hdt argument to create the Iterator
     * @param deleteBitmap argument to create the Iterator
     * @param hdtIterator argument to create the Iterator
     * @return iterator
     */
    static BitmapTriplesIteratorDiffBit createForType(Dictionary dictionary, HDT hdt, Bitmap deleteBitmap, IteratorTripleID hdtIterator) {
        String type = dictionary.getType();

        // four section dictionaries
        if (HDTVocabulary.DICTIONARY_TYPE_FOUR_SECTION.equals(type) || type.equals(HDTVocabulary.DICTIONARY_TYPE_FOUR_PSFC_SECTION))
            return new FourSectionBitmapTriplesIteratorDiffBit(hdt, deleteBitmap, hdtIterator);

        // multiple section dictionaries
        if (type.equals(HDTVocabulary.DICTIONARY_TYPE_MULT_SECTION))
            return new MultipleSectionBitmapTriplesIteratorDiffBit(hdt, deleteBitmap, hdtIterator);

        throw new IllegalFormatException("Implementation of BitmapTriplesIteratorDiffBit not found for " + type);
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
