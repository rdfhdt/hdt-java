package org.rdfhdt.hdt.dictionary.impl.utilCat;

import java.util.ArrayList;
import java.util.Iterator;

public class CatWrapper implements Iterator<CatElement> {
    public Iterator<? extends CharSequence> sectionIter;
    public String iterName;
    int count = 0;
    public CatWrapper(Iterator<? extends CharSequence> sectionIter,String iterName){
        this.sectionIter = sectionIter;
        this.iterName = iterName;
    }

    @Override
    public boolean hasNext() {
        if(sectionIter.hasNext())
            return true;
        else
            return false;
    }

    @Override
    public CatElement next() {
        CharSequence entity = sectionIter.next();
        count++;
        ArrayList<CatElement.IteratorPlusPosition> IDs = new ArrayList<>();

        IDs.add(new CatElement.IteratorPlusPosition(iterName,count));
        return new CatElement(entity,IDs);
    }
}
