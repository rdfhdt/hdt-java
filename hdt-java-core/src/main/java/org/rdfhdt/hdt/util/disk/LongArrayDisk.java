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

package org.rdfhdt.hdt.util.disk;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

//Implementing an array of longs that is backed up on disk. Following this: http://vanillajava.blogspot.fr/2011/12/using-memory-mapped-file-for-huge.html

public class LongArrayDisk {
    private static final long MAPPING_SIZE = 1 << 30;
    private RandomAccessFile array = null;
    private MappedByteBuffer[] mappings_array;
    private long size;
    private String location;

    public LongArrayDisk(String location, long size){
        try {
            this.location = location;
            this.size = size;
            this.array = new RandomAccessFile(location, "rw");
            size = 8 * (size);
            int blocks = (int) Math.ceil((double)size / MAPPING_SIZE);
            mappings_array = new MappedByteBuffer[blocks];
            for (int block = 0; block < blocks; block++) {
                long size2 = MAPPING_SIZE;
                if (block+1==blocks){
                    size2 = Math.min(MAPPING_SIZE, size%MAPPING_SIZE);
                }
                mappings_array[block] = array.getChannel().map(FileChannel.MapMode.READ_WRITE, block*MAPPING_SIZE, size2);
            }
            for (long i = 0; i< this.size; i++){
                if (i%10000000==0){ //This is done because otherwise the changes are not written to disk fast enough
                    for (int b=0; b<blocks; b++){
                        mappings_array[b].force();
                    }

                }
                this.set(i,0);
            }
        } catch (IOException e) {
            try {
                array.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    public long get(long x) {
        long p = x * 8;
        int block = (int) (p / MAPPING_SIZE);
        int offset = (int) (p % MAPPING_SIZE);
        return mappings_array[block].getLong(offset);
    }

    public long getLong(long x) {
        return this.get(x);
    }

    public void set(long x, long y) {
        long p = x * 8;
        int block = (int) (p / MAPPING_SIZE);
        int offset = (int) (p % MAPPING_SIZE);
        mappings_array[block].putLong(offset, y);
    }

    public long length(){
        return size;
    }

    public void resize(long newSize){
        try {
            long oldSize = this.size;
            this.size = newSize;
            long sizeBit = 8 * (newSize);
            this.array.setLength(sizeBit);

            int blocks = (int) Math.ceil((double)sizeBit / MAPPING_SIZE);
            mappings_array = new MappedByteBuffer[blocks];
            for (int block = 0; block < blocks; block++) {
                long sizeMapping = MAPPING_SIZE;
                if (block+1==blocks){
                    sizeMapping = Math.min(MAPPING_SIZE, sizeBit%MAPPING_SIZE);
                }
                mappings_array[block] = array.getChannel().map(FileChannel.MapMode.READ_WRITE, block*MAPPING_SIZE, sizeMapping);
            }

            for (long i = oldSize; i< newSize; i++){
                if (i%10000000==0){ //This is done because otherwise the changes are not written to disk fast enough
                    for (int b=0; b<blocks; b++){
                        mappings_array[b].force();
                    }
                }
                this.set(i,0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public long getSize() {
        return size;
    }
}