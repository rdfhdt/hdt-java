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

package hdt.compact.array;

import hdt.compact.integer.VByte;
import hdt.hdt.HDTVocabulary;
import hdt.listener.ProgressListener;
import hdt.util.BitUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * @author mario.arias
 *
 */
public class LogArray64 implements DynamicArray {
	private static final byte W = 64;
	
	private long [] data = new long[10];
	private int numbits=32;
	private long numentries=0;
	
	public LogArray64() {
		this(64);
	}
	
	public LogArray64(int numbits) {
		this(numbits, 0);
	}
	
	public LogArray64(int numbits, long numentries) {
		this.numentries = 0;
		this.numbits = numbits;
		long size = numWordsFor(numbits, numentries);
		data = new long[Math.max((int)size,10)];
	}
	
	/** longs required to represent "total" integers of "bitsField" bits each */
	private static final long numWordsFor(int bitsField, long total) {
		return ((bitsField*total+64-1)/64) * (64/W);
	}
	
	protected static final int lastWordNumBits(int bitsField, long total) {
		long totalBits = bitsField*total;
    	if(totalBits==0) {
    		return 0;
    	}
    	return (int) (W-((W*numWordsFor(bitsField, total))-totalBits));
    }
	
	/** Number of bytes required to represent n integers of e bits each, aligned to 64bit */
	private static final long numBytesFor(int bitsField, long total) {
		return ((bitsField*total+64-1)/64) * (64/8);
	}

	 /** Retrieve a given index from array data where every value uses bitsField bits
     * @param data Array
     * @param bitsField Length in bits of each field
     * @param index Position to be retrieved
     */
	private static final long getField(long [] data, int bitsField, long index) {
		if(bitsField==0) return 0;
		long i=index*bitsField/W, j=index*bitsField-W*i;
        long result;
        if (j+bitsField <= W) {
                result = (data[(int)i] << (W-j-bitsField)) >>> (W-bitsField);
        } else {
                result = data[(int)i] >>> j;
                result = result | (data[(int)i+1] << ( (W<<1) -j-bitsField)) >>> (W-bitsField);
        }
        return result;
		
//		long bitPos = index*bitsField;
//		int i=(int)(bitPos / W);
//		int j=(int)(bitPos % W);
//        long result;
//        if (j+bitsField <= W) {
//        	result = (data[i] << (W-j-bitsField)) >>> (W-bitsField);
//        } else {
//        	result = data[i] >>> j;
//        	result = result | (data[i+1] << ( (W<<1) -j-bitsField)) >>> (W-bitsField);
//        }
//        return result;
	}
	
	/** Store a given value in index into array data where every value uses bitsField bits
     * @param data Array
     * @param bitsField Length in bits of each field
     * @param index Position to store in
     * @param value Value to be stored
     */
	private static final void setField(long [] data, int bitsField, int index, long value) {
		if(bitsField==0) return;
		int i=index*bitsField/W, j=index*bitsField-i*W;
		long mask = ((j+bitsField) < W ? ~0L << (j+bitsField) : 0) 
						| ((W-j) < W ? ~0L >>> (W-j) : 0);

		value = value & ~(~0L << bitsField);
		
		data[i] = (data[i] & mask) | value << j;
		if (j+bitsField>W) {
			mask = ~0L << (bitsField+j-W);
			data[i+1] = (data[i+1] & mask)| value >>> (W-j);
		}
		
//		
//		long bitPos = index*bitsField;
//		int i=(int)(bitPos/W);
//		int j=(int)(bitPos%W);
//		
//		long mask = ~(~0L << bitsField) << j;
//		data[i] = (data[i] & ~mask) | (value << j);
//			
//		if((j+bitsField>W)) {
//			mask = ~0L << (bitsField+j-W);
//			data[i+1] = (data[i+1] & mask) | value >>> (W-j);
//		}
	}
	
	private final void resizeArray(int size) {
		long [] newData = new long[size];
		System.arraycopy(data, 0, newData, 0, Math.min(newData.length, data.length));
		data = newData;
	}
	
	
	
	/* (non-Javadoc)
	 * @see hdt.triples.array.Stream#add(java.util.Iterator)
	 */
	@Override
	public void add(Iterator<Long> elements) {
		long max = 0;
		numentries = 0;
		
		// Count and calculate number of bits needed per element.
		while(elements.hasNext()) {
			long val = elements.next().longValue();
			max = val>max ? val : max;
			numentries++;
		}
		
        // Prepare array
        numbits = BitUtil.log2(max);
        int size = (int) numWordsFor(numbits, numentries);
        data = new long[size];

        // Save
        int count = 0;
        while(elements.hasNext()) {
        	long element = elements.next().longValue();
        	setField(data, numbits, count, element);
        	count++;
        }
	}

	/* (non-Javadoc)
	 * @see hdt.triples.array.Stream#get(int)
	 */
	@Override
	public long get(long position) {
		if(position>=numentries) {
			throw new IndexOutOfBoundsException();
		}
		
		return getField(data, numbits, position);
	}
	
	public void set(int position, long value) {
		setField(data, numbits, position, value);
	}
	
	public void append(long value) {
//		if(value>=1<<numbits) {
//			throw new IllegalArgumentException();
//		}
		long neededSize = numWordsFor(numbits, numentries+1);
		if(data.length<neededSize) {
			resizeArray(data.length<<1);
		}
		this.set((int)numentries, value);
		numentries++;
	}
	
	public void aggresiveTrimToSize() {
		long max = 0;
		// Count and calculate number of bits needed per element.
		for(int i=0; i<numentries; i++) {
			long value = this.get(i);
			max = value>max ? value : max;
		}
		int newbits = BitUtil.log2(max);
		
		if(newbits!=numbits) {
			for(int i=0;i<numentries;i++) {
				long value = getField(data, numbits, i);
				setField(data, newbits, i, value);
			}
			numbits = newbits;
			long totalSize = numWordsFor(numbits, numentries);
			resizeArray((int)totalSize);
		}

	}
	
	public void trimToSize() {
		resizeArray((int)numWordsFor(numbits, numentries));
	}

	/* (non-Javadoc)
	 * @see hdt.triples.array.Stream#getNumberOfElements()
	 */
	@Override
	public long getNumberOfElements() {
		return numentries;
	}

	/* (non-Javadoc)
	 * @see hdt.triples.array.Stream#save(java.io.OutputStream, hdt.ProgressListener)
	 */
	@Override
	public void save(OutputStream output, ProgressListener listener) throws IOException {
		DataOutputStream dout = new DataOutputStream(output);
		
		output.write(numbits);
		VByte.encode(output, (int)numentries);
		int numwords = (int)numWordsFor(numbits, numentries);

		for(int i=0;i<numwords-1;i++) {
			dout.writeLong(data[i]);
		}
		
		if(numwords==0) {
			return;
		}
		
		// Write only used bits from last entry (byte aligned, little endian)
		int lastWordUsedBits = lastWordNumBits(numbits, numentries);
		BitUtil.writeLowerBitsByteAligned(data[numwords-1], lastWordUsedBits, dout);
	}

	/* (non-Javadoc)
	 * @see hdt.triples.array.Stream#load(java.io.InputStream, hdt.ProgressListener)
	 */
	@Override
	public void load(InputStream input, ProgressListener listener) throws IOException {
		DataInputStream din = new DataInputStream(input);
		
		numbits = input.read();
		numentries = VByte.decode(input);
		int numwords = (int)numWordsFor(numbits, numentries);
		
		data = new long[numwords];
		for(int i=0;i<numwords-1;i++) {
			data[i] = din.readLong();
		}
		
		if(numwords==0) {
			return;
		}
		
		// Read only used bits from last entry (byte aligned, little endian)
		int lastWordUsed = lastWordNumBits(numbits, numentries);
		data[numwords-1] = BitUtil.readLowerBitsByteAligned(lastWordUsed, din);
	}

	/* (non-Javadoc)
	 * @see hdt.triples.array.Stream#size()
	 */
	@Override
	public long size() {
		return numBytesFor(numbits, numentries);
	}
	
	public int getNumBits() {
		return numbits;
	}

	/* (non-Javadoc)
	 * @see hdt.compact.array.Stream#getType()
	 */
	@Override
	public String getType() {
		return HDTVocabulary.ARRAY_TYPE_LOG;
	}
}
