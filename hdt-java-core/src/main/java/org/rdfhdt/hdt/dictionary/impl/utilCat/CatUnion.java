package org.rdfhdt.hdt.dictionary.impl.utilCat;


import org.rdfhdt.hdt.util.string.CompactString;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * @author Ali Haidar
 *
 */
public class CatUnion implements Iterator<CatElement> {
    ArrayList<IteratorPlusElement> list;

    private List<Iterator<CatElement>> listIters;

    public CatUnion(List<Iterator<CatElement>> listIters) {

        this.list = new ArrayList<IteratorPlusElement>();
        this.listIters = new ArrayList<>(listIters);
        int count = 0;
        for (Iterator<CatElement> iter : this.listIters) {
            if (iter.hasNext()) {
                CatElement element = iter.next();
                list.add(new IteratorPlusElement(count, element));
            }
            count++;
        }

    }

    @Override
    public boolean hasNext() {
        if (list.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public CatElement next() {
        CharSequence element = null;
        ArrayList<CatElement.IteratorPlusPosition> ids = new ArrayList<>();
        list.sort(new IteratorPlusElementComparator());
        element = list.get(0).element.entity;
        CompactString elementCompactString = new CompactString(element);
        ListIterator<IteratorPlusElement> iteratorPlusElementIterator = list.listIterator();
        while (iteratorPlusElementIterator.hasNext()) {
            IteratorPlusElement next = iteratorPlusElementIterator.next();
            if(elementCompactString.equals(new CompactString(next.element.entity))) {
                int iter = next.iter;
                ids.addAll(next.element.IDs);
                if (listIters.get(iter).hasNext()) {
                    CatElement inElt = listIters.get(iter).next();
                    iteratorPlusElementIterator.set(new IteratorPlusElement(iter, inElt));
                } else {
                    iteratorPlusElementIterator.remove();
                }
            } else{
                break;
            }
        }
        return new CatElement(element,ids);
    }
}