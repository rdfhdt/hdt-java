package org.rdfhdt.hdt.util.io.compress;

import org.rdfhdt.hdt.iterator.utils.ExceptionIterator;
import org.rdfhdt.hdt.iterator.utils.MergeExceptionIterator;

import java.io.IOException;
import java.util.Comparator;

public class PairMergeIterator extends MergeExceptionIterator<Pair, IOException> {

    public PairMergeIterator(ExceptionIterator<Pair, IOException> in1, ExceptionIterator<Pair, IOException> in2, Comparator<Pair> comparator) {
        super(in1, in2, comparator);
    }

    public static <T extends ExceptionIterator<Pair, IOException>> ExceptionIterator<Pair, IOException> buildOfTree(
            T[] lst, Comparator<Pair> comparator) {
        return buildOfTree(it -> it, comparator, lst, 0, lst.length);
    }
}
