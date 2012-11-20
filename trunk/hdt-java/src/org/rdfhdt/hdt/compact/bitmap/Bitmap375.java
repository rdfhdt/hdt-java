/**
 * File: $HeadURL$
 * Revision: $Rev$
 * Last modified: $Date$
 * Last modified by: $Author$
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
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
 *   Mario Arias:               mario.arias@deri.org
 *   Javier D. Fernandez:       jfergar@infor.uva.es
 *   Miguel A. Martinez-Prieto: migumar2@infor.uva.es
 *   Alejandro Andres:          fuzzy.alej@gmail.com
 */

package org.rdfhdt.hdt.compact.bitmap;

import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.util.BitUtil;
import org.rdfhdt.hdt.util.io.IOUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Implements an index on top of the Bitmap64 to solve select and rank queries more efficiently.
 * 
 * index -> O(n)
 * rank1 -> O(1)
 * select1 -> O(log log n)
 * 
 * @author mario.arias
 *
 */
public class Bitmap375 extends Bitmap64 implements ModifiableBitmap {
	// Constants
	private static final int BLOCKS_PER_SUPER = 4;
	
	// Variables
	private int pop;
	private int [] superBlocks;
	private byte [] blocks;
	private boolean indexUpToDate = false;
    
	public Bitmap375() {
		super();
	}
	
	public Bitmap375(long nbits) {
		super(nbits);
	}
	
	public void dump() {
		int count = numWords(this.numbits);
		for(int i=0;i<count;i++) {
			System.out.print(i+"\t");
			IOUtil.printBits(words[i], 64);
		}
	}
	
	public void updateIndex() {
		trimToSize();
		superBlocks = new int[1+(words.length-1)/BLOCKS_PER_SUPER];
		blocks = new byte[words.length];
	
		int countBlock=0, countSuperBlock=0, blockIndex=0, superBlockIndex=0;
		
		while(blockIndex<words.length) {
			if((blockIndex%BLOCKS_PER_SUPER)==0) {
				countSuperBlock += countBlock;
				if(superBlockIndex<superBlocks.length) {
					superBlocks[superBlockIndex++] = countSuperBlock;
				}
				countBlock = 0;
			}		
			blocks[blockIndex] = (byte) countBlock;
			countBlock += Long.bitCount(words[blockIndex]);
			blockIndex++;
		}
		pop = countSuperBlock+countBlock; 
		indexUpToDate = true;
	}

	/* (non-Javadoc)
	 * @see hdt.compact.bitmap.Bitmap#access(long)
	 */
	@Override
	public boolean access(long bitIndex) {
		if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

        int wordIndex = wordIndex(bitIndex);
        if(wordIndex>=words.length) {
        	return false;
        }
        
        return ((words[wordIndex] & (1L << bitIndex)) != 0);
	}
	
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
		long superBlockRank = superBlocks[(int)superBlockIndex];
		
		long blockIndex = pos/W;
		long blockRank = 0xFF & blocks[(int)blockIndex];
		
		long chunkIndex = W-1-pos%W;
		long block = words[(int)blockIndex] << chunkIndex;
		long chunkRank = Long.bitCount(block);
		
		return superBlockRank + blockRank + chunkRank;
	}

	/* (non-Javadoc)
	 * @see hdt.compact.bitmap.Bitmap#rank0(long)
	 */
	@Override
	public long rank0(long pos) {
		return pos+1-rank1(pos);
	}

	/* (non-Javadoc)
	 * @see hdt.compact.bitmap.Bitmap#select0(long)
	 */
	@Override
	public long select0(long x) {
		throw new NotImplementedException();
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
		
		// Search superblock (binary Search)
		int superBlockIndex = Arrays.binarySearch(superBlocks, (int) x);
		if(superBlockIndex<0) {
			// Not found exactly, gives the position where it should be inserted
			superBlockIndex = -superBlockIndex-2;
		} else if(superBlockIndex>0){
			// If found exact, we need to check previous block.
			superBlockIndex--;
		}
		
		// If there is a run of many zeros, two correlative superblocks may have the same value,
		// We need to position at the first of them.
		while(superBlockIndex>0 && (superBlocks[superBlockIndex]>=x)) {
			superBlockIndex--;
		}
		
		int countdown = (int)x-superBlocks[superBlockIndex];
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
		int bitpos = BitUtil.select1(words[blockIdx], countdown);
		
		return blockIdx * W + bitpos - 1;
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