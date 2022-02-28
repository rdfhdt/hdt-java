package org.rdfhdt.hdt.triples.impl;

import org.rdfhdt.hdt.compact.bitmap.Bitmap;
import org.rdfhdt.hdt.compact.bitmap.BitmapFactory;
import org.rdfhdt.hdt.compact.bitmap.ModifiableBitmap;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.triples.DictionaryEntriesDiff;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the {@link DictionaryEntriesDiff} for four section dictionaries
 */
public class FourSectionDictionaryEntriesDiff implements DictionaryEntriesDiff {

    private final HDT hdtOriginal;
    private final IteratorTripleID iterator;
    private final Bitmap bitArrayDisk;
    private final Map<String, ModifiableBitmap> bitmaps;
    private long count;

    public FourSectionDictionaryEntriesDiff(HDT hdtOriginal, Bitmap deleteBitmap, IteratorTripleID iterator) {
        this.hdtOriginal = hdtOriginal;
        this.iterator = iterator;
        this.bitArrayDisk = deleteBitmap;
        this.bitmaps = new HashMap<>();
    }

    @Override
    public void loadBitmaps() {
        this.bitmaps.clear();
        count = 0;

        // create a bitmap for each section

        Dictionary dict = hdtOriginal.getDictionary();
        this.bitmaps.put("P", BitmapFactory.createRWBitmap(dict.getPredicates().getNumberOfElements()));
        this.bitmaps.put("S", BitmapFactory.createRWBitmap(dict.getSubjects().getNumberOfElements()));
        this.bitmaps.put("O", BitmapFactory.createRWBitmap(dict.getObjects().getNumberOfElements()));
        this.bitmaps.put("SH_S", BitmapFactory.createRWBitmap(dict.getShared().getNumberOfElements()));
        this.bitmaps.put("SH_O", BitmapFactory.createRWBitmap(dict.getShared().getNumberOfElements()));

        // if triple is marked as to delete mark the corresponding subject, predicate and object as to delete

        while (iterator.hasNext()) {
            TripleID tripleID = iterator.next();
            count = iterator.getLastTriplePosition();
            if (!this.bitArrayDisk.access(count)) { // triple not deleted bit = 0
                long predId = tripleID.getPredicate();
                long subjId = tripleID.getSubject();
                long objId = tripleID.getObject();
                // assign true for elements to keep when creating dictionary
                this.bitmaps.get("P").set(predId - 1, true);
                long numShared = hdtOriginal.getDictionary().getShared().getNumberOfElements();
                if (subjId <= numShared) {
                    this.bitmaps.get("SH_S").set(subjId - 1, true);
                } else {
                    this.bitmaps.get("S").set(subjId - numShared - 1, true);
                }
                if (objId <= numShared) {
                    this.bitmaps.get("SH_O").set(objId - 1, true);
                } else {
                    this.bitmaps.get("O").set(objId - numShared - 1, true);
                }
            }
        }
    }

    @Override
    public long getCount() {
        return count;
    }

    @Override
    public Map<String, ModifiableBitmap> getBitmaps() {
        return bitmaps;
    }
}
