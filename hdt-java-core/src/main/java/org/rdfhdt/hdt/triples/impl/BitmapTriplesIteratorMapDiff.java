package org.rdfhdt.hdt.triples.impl;


import org.rdfhdt.hdt.compact.bitmap.Bitmap;
import org.rdfhdt.hdt.compact.bitmap.ModifiableBitmap;
import org.rdfhdt.hdt.dictionary.DictionaryDiff;
import org.rdfhdt.hdt.dictionary.impl.utilCat.CatMapping;
import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleIDComparator;
import org.rdfhdt.hdt.triples.Triples;

import java.util.*;


public class BitmapTriplesIteratorMapDiff implements IteratorTripleID {
    private final CatMapping subjMapping;
    private final CatMapping objMapping;
    private final CatMapping predMapping;
    private final CatMapping sharedMapping;

    private final long countTriples;

    DictionaryDiff dictionaryDiff;

    Iterator<TripleID> list;
    Triples triples;
    TripleIDComparator tripleIDComparator = new TripleIDComparator(TripleComponentOrder.SPO);
    Bitmap bitArrayDisk;

    public BitmapTriplesIteratorMapDiff(HDT hdtOriginal, Bitmap deleteBitmap, DictionaryDiff dictionaryDiff, long countTriples) {
        this.subjMapping = dictionaryDiff.getAllMappings().get("subject");
        this.objMapping = dictionaryDiff.getAllMappings().get("object");
        this.predMapping = dictionaryDiff.getAllMappings().get("predicate");
        this.sharedMapping = dictionaryDiff.getAllMappings().get("shared");
        this.dictionaryDiff = dictionaryDiff;
        this.countTriples = countTriples;
        this.triples = hdtOriginal.getTriples();
        this.bitArrayDisk = deleteBitmap;
        list = getTripleID(0).listIterator();
        count++;
    }


    @Override
    public boolean hasPrevious() {
        return false;
    }

    @Override
    public TripleID previous() {
        return null;
    }

    @Override
    public void goToStart() {

    }

    @Override
    public boolean canGoTo() {
        return false;
    }

    @Override
    public void goTo(long pos) {

    }

    @Override
    public long estimatedNumResults() {
        return countTriples;
    }

    @Override
    public ResultEstimationType numResultEstimation() {
        return null;
    }

    @Override
    public TripleComponentOrder getOrder() {
        return null;
    }

    @Override
    public long getLastTriplePosition() {
        throw new NotImplementedException();
    }

    private long count;

    @Override
    public boolean hasNext() {
        return count < dictionaryDiff.getMappingBack().getSize() || list.hasNext();
    }

    @Override
    public TripleID next() {
        if (!list.hasNext()) {
            list = getTripleID(count).iterator();
            count++;
        }
        return list.next();
    }

    private List<TripleID> getTripleID(long count) {
        ArrayList<TripleID> newTriples = new ArrayList<>();
        if (dictionaryDiff.getMappingBack().getSize() > 0) {
            long mapping = dictionaryDiff.getMappingBack().getMapping(count);
            IteratorTripleID it = this.triples.search(new TripleID(mapping, 0, 0));
            while (it.hasNext()) {
                TripleID next = it.next();
                if (!this.bitArrayDisk.access(it.getLastTriplePosition() - 1)) {
                    newTriples.add(mapTriple(next));
                }
            }
        }
        newTriples.sort(tripleIDComparator);
        return newTriples;
    }

    public TripleID mapTriple(TripleID tripleID) {

        long subjOld = tripleID.getSubject();
        long numShared = this.sharedMapping.getSize();
        long newSubjId;
        if (subjOld <= numShared) {
            if (this.sharedMapping.getType(subjOld - 1) == 1) {
                newSubjId = this.sharedMapping.getMapping(subjOld - 1);
            } else {
                newSubjId = this.sharedMapping.getMapping(subjOld - 1) + this.dictionaryDiff.getNumShared();
            }
        } else {
            if (this.subjMapping.getType(subjOld - numShared - 1) == 1)
                newSubjId = this.subjMapping.getMapping(subjOld - numShared - 1);
            else
                newSubjId = this.subjMapping.getMapping(subjOld - numShared - 1) + this.dictionaryDiff.getNumShared();
        }
        long newPredId = this.predMapping.getMapping(tripleID.getPredicate() - 1);

        long objOld = tripleID.getObject();
        long newObjId;
        if (objOld <= numShared) {
            long type = this.sharedMapping.getType(objOld - 1);
            if (type == 1) {
                newObjId = this.sharedMapping.getMapping(objOld - 1);
            } else {
                newObjId = this.sharedMapping.getMapping(objOld - 1) + this.dictionaryDiff.getNumShared();
            }

        } else {
            if (this.objMapping.getType(objOld - numShared - 1) == 1)
                newObjId = this.objMapping.getMapping(objOld - numShared - 1);
            else
                newObjId = this.objMapping.getMapping(objOld - numShared - 1) + this.dictionaryDiff.getNumShared();
        }
        return new TripleID(newSubjId, newPredId, newObjId);
    }
}
