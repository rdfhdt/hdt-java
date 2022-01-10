package org.rdfhdt.hdt.dictionary.impl.utilCat;

import org.rdfhdt.hdt.util.string.CharSequenceComparator;
import org.rdfhdt.hdt.util.string.CompactString;

import java.util.Comparator;

public class IteratorPlusElementComparator implements Comparator<IteratorPlusElement> {

    public int compare(IteratorPlusElement a, IteratorPlusElement b) {
        CharSequenceComparator comparator = new CharSequenceComparator();

        return comparator.compare(new CompactString(a.element.entity),new CompactString(b.element.entity));
    }
}
