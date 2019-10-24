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
 */

package org.rdfhdt.hdt.compact.bitmap;

import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.util.BitUtil;
import org.rdfhdt.hdt.util.io.IOUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Bitmap375 version that is backed up on disk
 */
public class Bitmap375Disk extends Bitmap64Disk implements ModifiableBitmap {
    // Constants
    private static final int BLOCKS_PER_SUPER = 4;

    // Variables
    private long pop;
    private long [] superBlocksLong;
    private int [] superBlocksInt;
    private byte [] blocks;
    private boolean indexUpToDate;

    public Bitmap375Disk(String location) {
        super(location);
    }

    public Bitmap375Disk(String location, long nbits) {
        super(location, nbits);
    }

    public void dump() {
        int count = (int) numWords(this.numbits);
        for(int i=0;i<count;i++) {
            System.out.print(i+"\t");
            IOUtil.printBitsln(words.get(i), 64);
        }
    }

    public void updateIndex() {
        trimToSize();
        if(numbits>Integer.MAX_VALUE) {
            superBlocksLong = new long[1+((int)words.length()-1)/BLOCKS_PER_SUPER];
        } else {
            superBlocksInt = new int[1+((int)words.length()-1)/BLOCKS_PER_SUPER];
        }
        blocks = new byte[(int)words.length()];

        long countBlock=0, countSuperBlock=0;
        int blockIndex=0, superBlockIndex=0;

        while(blockIndex<(int)words.length()) {
            if((blockIndex%BLOCKS_PER_SUPER)==0) {
                countSuperBlock += countBlock;
                if(superBlocksLong!=null) {
                    if(superBlockIndex<superBlocksLong.length) {
                        superBlocksLong[superBlockIndex++] = countSuperBlock;
                    }
                } else {
                    if(superBlockIndex<superBlocksInt.length) {
                        superBlocksInt[superBlockIndex++] = (int)countSuperBlock;
                    }
                }
                countBlock = 0;
            }
            blocks[blockIndex] = (byte) countBlock;
            countBlock += Long.bitCount(words.get(blockIndex));
            blockIndex++;
        }
        pop = countSuperBlock+countBlock;
        indexUpToDate = true;

//		for(int i=0;i<words.length;i++) {
//			if((i%BLOCKS_PER_SUPER)==0) {
//				int superB = i/BLOCKS_PER_SUPER;
//				System.out.println("Super: "+superB+ " rank1: "+superBlocks[superB]+" Rank0: "+(superB*BLOCKS_PER_SUPER*W-superBlocks[superB])+" Bits: "+(superB*BLOCKS_PER_SUPER*W));
//			}
//			System.out.println("\tWord: "+i+ " Rank1: "+blocks[i]+ " Rank0: "+ (W*(i%BLOCKS_PER_SUPER)-blocks[i]) + " Bits: "+(W*i));
//			System.out.print("\t\t"); IOUtil.printBits(words[i], W); System.out.println(" "+Long.bitCount(words[i]));
//		}
        //System.out.println("Bitmap with "+numbits+" bits. Data size: "+(words.length*8)+" Index Size: "+ (superBlocks.length*4+blocks.length));
    }

    /* (non-Javadoc)
     * @see hdt.compact.bitmap.Bitmap#access(long)
     */
    @Override
    public boolean access(long bitIndex) {
        if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

        int wordIndex = wordIndex(bitIndex);
        if(wordIndex>=words.length()) {
            return false;
        }

        return (words.get(wordIndex) & (1L << bitIndex)) != 0;
    }

    @Override
    public void set(long bitIndex, boolean value) {
        indexUpToDate=false;
        super.set(bitIndex, value);
    }

    /* (non-Javadoc)
     * @see hdt.compact.bitmap.Bitmap#rank1(long)
     */
    @Override
    public long rank1(long pos) {
        if(pos<0) {
            return 0;
        }
        if(!indexUpToDate) {
            updateIndex();
        }
        if(pos>=numbits) {
            return pop;
        }

        long superBlockIndex = pos/(BLOCKS_PER_SUPER*W);
        long superBlockRank;
        if(superBlocksLong!=null) {
            superBlockRank = superBlocksLong[(int)superBlockIndex];
        } else {
            superBlockRank = superBlocksInt[(int)superBlockIndex];
        }

        long blockIndex = pos/W;
        long blockRank = 0xFF & blocks[(int)blockIndex];

        long chunkIndex = W-1-pos%W;
        long block = words.get((int)blockIndex) << chunkIndex;
        long chunkRank = Long.bitCount(block);

        return superBlockRank + blockRank + chunkRank;
    }

    /* (non-Javadoc)
     * @see hdt.compact.bitmap.Bitmap#rank0(long)
     */
    @Override
    public long rank0(long pos) {
        return pos+1L-rank1(pos);
    }

    private static int binarySearch0(long[] a, int fromIndex, int toIndex, long key) {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = mid * BLOCKS_PER_SUPER * W-a[mid];

            if (midVal < key)
                low = mid + 1;
            else if (midVal > key)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }

    private static int binarySearch0(int[] a, int fromIndex, int toIndex, long key) {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = mid * BLOCKS_PER_SUPER * W-a[mid];

            if (midVal < key)
                low = mid + 1;
            else if (midVal > key)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }

    /* (non-Javadoc)
     * @see hdt.compact.bitmap.Bitmap#select0(long)
     */
    @Override
    public long select0(long x) {
        //System.out.println("\t**Select0 for: "+ x);

        if(x<0) {
            return -1;
        }
        if(!indexUpToDate) {
            updateIndex();
        }
        if(x>numbits-pop) {
            return numbits;
        }

        // Search superblock (binary Search)
        int superBlockIndex;
        if(superBlocksLong!=null) {
            superBlockIndex = binarySearch0(superBlocksLong, 0, superBlocksLong.length, x);
        } else {
            superBlockIndex = binarySearch0(superBlocksInt, 0, superBlocksInt.length, x);
        }
        if(superBlockIndex<0) {
            // Not found exactly, gives the position where it should be inserted
            superBlockIndex = -superBlockIndex-2;
        } else if(superBlockIndex>0){
            // If found exact, we need to check previous block.
            superBlockIndex--;
        }

        long countdown;

        if(superBlocksLong!=null) {
            // If there is a run of many ones, two correlative superblocks may have the same value,
            // We need to position at the first of them.
            while(superBlockIndex>0 && (superBlockIndex*BLOCKS_PER_SUPER*W-superBlocksLong[superBlockIndex]>=x)) {
                superBlockIndex--;
            }

            countdown = x-(superBlockIndex*BLOCKS_PER_SUPER*W-superBlocksLong[superBlockIndex]);
        } else {
            // If there is a run of many ones, two correlative superblocks may have the same value,
            // We need to position at the first of them.
            while(superBlockIndex>0 && (superBlockIndex*BLOCKS_PER_SUPER*W-superBlocksInt[superBlockIndex]>=x)) {
                superBlockIndex--;
            }

            countdown = x-(superBlockIndex*BLOCKS_PER_SUPER*W-superBlocksInt[superBlockIndex]);
        }
        int blockIdx = superBlockIndex * BLOCKS_PER_SUPER;

        // Search block
        while(true) {
            if(blockIdx>= (superBlockIndex+1) * BLOCKS_PER_SUPER || blockIdx>=blocks.length) {
                blockIdx--;
                break;
            }
            if((0xFF & (W*(blockIdx%BLOCKS_PER_SUPER) - blocks[blockIdx]))>=countdown) {
                // We found it!
                blockIdx--;
                break;
            }
            blockIdx++;
        }
        if(blockIdx<0) {
            blockIdx=0;
        }
        countdown = countdown - (0xFF & ((blockIdx%BLOCKS_PER_SUPER)*W - blocks[blockIdx]));

        // Search bit inside block
        int bitpos = BitUtil.select0(words.get(blockIdx), (int)countdown);

        return ((long)blockIdx) * W + bitpos - 1;
    }

    /* (non-Javadoc)
     * @see hdt.compact.bitmap.Bitmap#select1(long)
     */
    @Override
    public long select1(long x) {
        //System.out.println("\t**Select1 for: "+ x);

        if(x<0) {
            return -1;
        }
        if(!indexUpToDate) {
            updateIndex();
        }
        if(x>pop) {
            return numbits;
        }
        if(numbits==0) {
            return 0;
        }

        // Search superblock (binary Search)
        int superBlockIndex;
        if(superBlocksLong!=null) {
            superBlockIndex = Arrays.binarySearch(superBlocksLong, x);
        } else {
            superBlockIndex = Arrays.binarySearch(superBlocksInt, (int)x);
        }
        if(superBlockIndex<0) {
            // Not found exactly, gives the position where it should be inserted
            superBlockIndex = -superBlockIndex-2;
        } else if(superBlockIndex>0){
            // If found exact, we need to check previous block.
            superBlockIndex--;
        }

        long countdown;
        if(superBlocksLong!=null) {
            // If there is a run of many zeros, two correlative superblocks may have the same value,
            // We need to position at the first of them.
            while(superBlockIndex>0 && (superBlocksLong[superBlockIndex]>=x)) {
                superBlockIndex--;
            }
            countdown = x-superBlocksLong[superBlockIndex];
        } else {
            // If there is a run of many zeros, two correlative superblocks may have the same value,
            // We need to position at the first of them.
            while(superBlockIndex>0 && (superBlocksInt[superBlockIndex]>=x)) {
                superBlockIndex--;
            }
            countdown = x-superBlocksInt[superBlockIndex];
        }
        int blockIdx = superBlockIndex * BLOCKS_PER_SUPER;

        // Search block
        while(true) {
            if(blockIdx>= (superBlockIndex+1) * BLOCKS_PER_SUPER || blockIdx>=blocks.length) {
                blockIdx--;
                break;
            }
            if((0xFF & blocks[blockIdx])>=countdown) {
                // We found it!
                blockIdx--;
                break;
            }
            blockIdx++;
        }
        if(blockIdx<0) {
            blockIdx=0;
        }
        countdown = countdown - (0xFF & blocks[blockIdx]);

        // Search bit inside block
        int bitpos = BitUtil.select1(words.get(blockIdx), (int)countdown);

        return ((long)blockIdx) * W + bitpos - 1;
    }

    /* (non-Javadoc)
     * @see hdt.compact.bitmap.Bitmap#countOnes()
     */
    @Override
    public long countOnes() {
        return rank1(numbits);
    }

    /* (non-Javadoc)
     * @see hdt.compact.bitmap.Bitmap#countZeros()
     */
    @Override
    public long countZeros() {
        return numbits-countOnes();
    }

    @Override
    public long getRealSizeBytes() {
        updateIndex();

        long accum = super.getRealSizeBytes();

        if(superBlocksLong!=null) {
            accum+=superBlocksLong.length*8;
        }

        if(superBlocksInt!=null) {
            accum+=superBlocksInt.length*4;
        }

        accum+=blocks.length;

        return accum;
    }

    /* (non-Javadoc)
     * @see hdt.compact.bitmap.Bitmap#getType()
     */
    @Override
    public String getType() {
        return HDTVocabulary.BITMAP_TYPE_PLAIN;
    }

    /* (non-Javadoc)
     * @see hdt.compact.bitmap.Bitmap#save(java.io.OutputStream, hdt.listener.ProgressListener)
     */
    @Override
    public void save(OutputStream output, ProgressListener listener) throws IOException {
        super.save(output, listener);
    }

    /* (non-Javadoc)
     * @see hdt.compact.bitmap.Bitmap#load(java.io.InputStream, hdt.listener.ProgressListener)
     */
    @Override
    public void load(InputStream input, ProgressListener listener) throws IOException {
        super.load(input, listener);
        updateIndex();
    }
}