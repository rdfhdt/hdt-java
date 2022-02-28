package org.rdfhdt.hdt.triples.impl;

import org.rdfhdt.hdt.compact.bitmap.Bitmap;
import org.rdfhdt.hdt.compact.bitmap.BitmapFactory;
import org.rdfhdt.hdt.compact.bitmap.ModifiableBitmap;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.dictionary.DictionarySection;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.triples.DictionaryEntriesDiff;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Implementation of the {@link DictionaryEntriesDiff} for multiple section dictionaries
 */
public class MultipleSectionDictionaryEntriesDiff implements DictionaryEntriesDiff {

    private final HDT hdtOriginal;
    private final IteratorTripleID iterator;
    private final Bitmap bitArrayDisk;
    private final HashMap<String, ModifiableBitmap> bitmaps;
    private final HashMap<String, Long> objectsOffsets;
    private long count;

    public MultipleSectionDictionaryEntriesDiff(HDT hdtOriginal, Bitmap deleteBitmap, IteratorTripleID iterator) {
        this.hdtOriginal = hdtOriginal;
        this.iterator = iterator;
        this.bitArrayDisk = deleteBitmap;
        this.bitmaps = new HashMap<>();
        this.objectsOffsets = new HashMap<>();
    }


    @Override
    public void loadBitmaps() {
        this.bitmaps.clear();

        // create a bitmap for each section

        Dictionary dict = hdtOriginal.getDictionary();
        this.bitmaps.put("P", BitmapFactory.createRWBitmap(dict.getPredicates().getNumberOfElements()));
        this.bitmaps.put("S", BitmapFactory.createRWBitmap(dict.getSubjects().getNumberOfElements()));

        // create bitmaps for all objects
        TreeMap<String, DictionarySection> allObjects = dict.getAllObjects();
        long count = 0;
        for (Map.Entry<String, DictionarySection> next : allObjects.entrySet()) {
            this.bitmaps.put(next.getKey(), BitmapFactory.createRWBitmap(next.getValue().getNumberOfElements()));
            objectsOffsets.put(next.getKey(), count);
            count += next.getValue().getNumberOfElements();
        }
        this.bitmaps.put("SH_S", BitmapFactory.createRWBitmap(dict.getShared().getNumberOfElements()));
        this.bitmaps.put("SH_O", BitmapFactory.createRWBitmap(dict.getShared().getNumberOfElements()));

        while (iterator.hasNext()) {
            TripleID tripleID = iterator.next();
            this.count = iterator.getLastTriplePosition();
            if (!this.bitArrayDisk.access(this.count)) { // triple not deleted bit = 0
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
                    String dataType = this.hdtOriginal.getDictionary().dataTypeOfId(objId);
                    long numObjectsBefore = objectsOffsets.get(dataType);
                    this.bitmaps.get(dataType).set(objId - numObjectsBefore - numShared - 1, true);
                }
            }
        }
    }

    @Override
    public long getCount() {
        return count;
    }

    @Override
    public HashMap<String, ModifiableBitmap> getBitmaps() {
        return bitmaps;
    }
}
