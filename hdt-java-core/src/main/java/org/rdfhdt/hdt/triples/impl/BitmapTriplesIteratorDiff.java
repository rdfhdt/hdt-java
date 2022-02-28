package org.rdfhdt.hdt.triples.impl;

import org.rdfhdt.hdt.compact.bitmap.ModifiableBitmap;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;

/**
 * Class to compute inside a bitmap the elements that are into 2 HDTs
 */
public class BitmapTriplesIteratorDiff {
    private final HDT hdtOriginal;
    private final HDT hdtDiff;
    private final ModifiableBitmap bitmap;

    /**
     * create a {@link BitmapTriplesIteratorDiff} to compute inside a bitmap the triples in both HDTs
     * @param hdtOriginal the original hdt
     * @param hdtDiff the second hdt to diff
     * @param bitmap the bitmap to fill
     */
    public BitmapTriplesIteratorDiff(HDT hdtOriginal, HDT hdtDiff, ModifiableBitmap bitmap) {
        this.hdtOriginal = hdtOriginal;
        this.hdtDiff = hdtDiff;
        this.bitmap = bitmap;
    }

    /**
     * fill the diff bitmap
     */
    public void fillBitmap() {
        IteratorTripleString iterator1;
        try {
            // search all the triples of the first HDT
            iterator1 = hdtOriginal.search("", "", "");
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
        while (iterator1.hasNext()) {
            TripleString origNext = iterator1.next();
            try {
                // search the triple in the second HDT
                IteratorTripleString otherTriple = hdtDiff.search(
                        origNext.getSubject(),
                        origNext.getPredicate(),
                        origNext.getObject()
                );
                // if the triple is in both HDT, we mark in the delete bitmap the triple to be deleted
                if (otherTriple.hasNext()) {
                    bitmap.set(iterator1.getLastTriplePosition(), true);
                }
            } catch (NotFoundException e) {
                // continue
            }
        }
    }
}
