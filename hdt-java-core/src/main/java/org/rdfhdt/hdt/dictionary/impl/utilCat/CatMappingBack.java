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

import java.util.ArrayList;
import org.rdfhdt.hdt.util.disk.LongArrayDisk;


/**
 * @author Dennis Diefenbach & Jose Gimenez Garcia
 */
public class CatMappingBack {
    private String location;
    private long size;
    private LongArrayDisk mapping1;
    private LongArrayDisk mappingType1;
    private LongArrayDisk mapping2;
    private LongArrayDisk mappingType2;

    public CatMappingBack(String location, long size){
        this.location = location;
        this.size = size+1;
        this.mapping1 = new LongArrayDisk(location+"mapping_back_1",this.size);
        this.mapping2 = new LongArrayDisk(location+"mapping_back_2",this.size);;
        this.mappingType1 = new LongArrayDisk(location+"mapping_back_type_1",this.size);;
        this.mappingType2 = new LongArrayDisk(location+"mapping_back_type_2",this.size);;
    }

    public long size(){
        return size;
    }

    public ArrayList<Long> getMapping(long i){
        ArrayList<Long> r = new ArrayList<>();
        if (mapping1.get(i)!=0){
            r.add(mapping1.get(i));
        }
        if (mapping2.get(i)!=0){
            r.add(mapping2.get(i));
        }
        return r;
    }

    public ArrayList<Integer> getType(long i){
        ArrayList<Integer> r = new ArrayList<>();
        if (mapping1.get(i)!=0){
            r.add((int)mappingType1.get(i));
        }
        if (mapping2.get(i)!=0){
            r.add((int)mappingType2.get(i));
        }
        return r;
    }

    public void set(long i, int mapping, int type){
        if (this.mapping1.get(i)==0){
            mapping1.set(i,mapping);
            mappingType1.set(i,type);
        } else {
            mapping2.set(i,mapping);
            mappingType2.set(i,type);
        }
    }

}