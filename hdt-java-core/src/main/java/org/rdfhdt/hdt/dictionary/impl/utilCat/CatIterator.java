/**
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 3.0 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Contacting the authors:
 *   Dennis Diefenbach:         dennis.diefenbach@univ-st-etienne.fr
 *   Jose Gimenez Garcia:       jose.gimenez.garcia@univ-st-etienne.fr
 */

package org.rdfhdt.hdt.dictionary.impl.utilCat;

import org.apache.commons.math3.util.Pair;
import org.rdfhdt.hdt.util.string.CharSequenceComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;


/**
 * @author Dennis Diefenbach & Jose Gimenez Garcia
 */
public class CatIterator implements Iterator<Integer> {
    ArrayList<IteratorPlusInt> list;
    private Iterator<Pair<Integer,Integer>> it1;
    private Iterator<Pair<Integer,Integer>> it2;

    public CatIterator(Iterator<Pair<Integer,Integer>> it1, Iterator<Pair<Integer,Integer>> it2){
        this.it1=it1;
        this.it2=it2;
        list = new ArrayList<IteratorPlusInt>();
        if (it1.hasNext()){
            list.add(new IteratorPlusInt(1,it1.next()));
        }
        if (it2.hasNext()){
            list.add(new IteratorPlusInt(2,it2.next()));
        }
    }

    @Override
    public boolean hasNext() {
        if (list.size()>0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Integer next() {
        int r;
        Collections.sort(list, new ScoreComparator());
        if (list.get(0).iterator==1){
            r = list.get(0).value.getKey();
            if (it1.hasNext()){
                list.set(0,new IteratorPlusInt(1,it1.next()));
            } else {
                list.remove(0);
            }
        } else {
            r = list.get(0).value.getKey();
            if (it2.hasNext()){
                list.set(0,new IteratorPlusInt(2,it2.next()));
            } else {
                list.remove(0);
            }
        }
        return r;
    }

    @Override
    public void remove() {
        try {
            throw new Exception("Not implemented");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class IteratorPlusInt {
        public int iterator;
        public Pair<Integer,Integer> value;

        public IteratorPlusInt( int iterator, Pair<Integer,Integer> value){
            this.iterator = iterator;
            this.value = value;

        }
    }

    public class ScoreComparator implements Comparator<IteratorPlusInt> {

        public int compare(IteratorPlusInt a, IteratorPlusInt b) {
            CharSequenceComparator comparator = new CharSequenceComparator();
            if (a.value.getKey()>b.value.getKey()){
                return 1;
            } else if (a.value.getKey()<b.value.getKey()) {
                return -1;
            } else {
                return 0;
            }
        }
    }
}