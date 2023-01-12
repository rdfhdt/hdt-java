package org.rdfhdt.hdt.hdt.impl.diskindex;

import org.rdfhdt.hdt.compact.bitmap.Bitmap;
import org.rdfhdt.hdt.compact.sequence.Sequence;
import org.rdfhdt.hdt.iterator.utils.FetcherIterator;
import org.rdfhdt.hdt.util.io.compress.Pair;

public class ObjectAdjReader extends FetcherIterator<Pair> {
    private final Sequence seqZ, seqY;
    private final Bitmap bitmapZ;
    private long indexY, indexZ;

    public ObjectAdjReader(Sequence seqZ, Sequence seqY, Bitmap bitmapZ) {
        this.seqZ = seqZ;
        this.seqY = seqY;
        this.bitmapZ = bitmapZ;
    }

    @Override
    protected Pair getNext() {
        if (indexZ >= seqZ.getNumberOfElements()) {
            return null;
        }

        Pair pair = new Pair();
        // create a pair object
        pair.object = seqZ.get(indexZ);
        pair.predicatePosition = indexY;
        pair.predicate = seqY.get(indexY);

        // shift to the next predicate if required
        if (bitmapZ.access(indexZ++)) {
            indexY++;
        }
        return pair;
    }
}
