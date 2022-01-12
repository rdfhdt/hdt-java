package org.rdfhdt.hdt.dictionary.impl.utilCat;

import java.util.ArrayList;

public class CatElement {
    public CharSequence entity;
    public ArrayList<IteratorPlusPosition> IDs;
    public CatElement(CharSequence entity, ArrayList<IteratorPlusPosition> IDs){
        this.entity = entity;
        this.IDs = new ArrayList<>(IDs);
    }
    public static class IteratorPlusPosition{
        public CharSequence iter;
        public long pos;
        public IteratorPlusPosition(CharSequence iter,long pos){
            this.iter = iter;
            this.pos = pos;
        }
    }

    @Override
    public boolean equals(Object o) {
        return entity.toString().equals(((CatElement) o).entity.toString());
    }
}
