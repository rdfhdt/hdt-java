package org.rdfhdt.hdt.util;

import java.util.Iterator;
import java.util.Map;

public class CustomIterator implements Iterator<CharSequence> {
    public CharSequence prev = "";
    boolean first = true;
    Iterator<? extends CharSequence> iter;
    Map<CharSequence,Long> literalsCounts;
    private long currCount;
    public CustomIterator(Iterator<? extends CharSequence> iter, Map<CharSequence,Long> literalsCounts) {
        this.iter = iter;
        this.literalsCounts = literalsCounts;
        if(iter.hasNext()) {
            prev = iter.next();
            currCount = literalsCounts.get(LiteralsUtils.getType(prev));
            currCount--;
        } else {
            first = false;
        }
    }

    @Override
    public boolean hasNext() {
        if(currCount == 0){
            if(first)
                return true;
            if(iter.hasNext()){
                prev = iter.next();
                currCount = literalsCounts.get(LiteralsUtils.getType(prev));
                currCount--;
                first = true;
            }
            return false;
        }else{
            return true;
        }
    }

    @Override
    public CharSequence next() {
        if(first) {
            first = false;
        } else {
            prev = iter.next();
            currCount--;
        }
        return LiteralsUtils.removeType(prev);
    }
}
