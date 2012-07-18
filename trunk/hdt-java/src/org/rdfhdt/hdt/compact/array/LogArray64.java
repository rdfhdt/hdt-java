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

package org.rdfhdt.hdt.compact.array;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import org.rdfhdt.hdt.compact.integer.VByte;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.util.BitUtil;
import org.rdfhdt.hdt.util.io.IOUtil;

/**
 * @author mario.arias
 *
 */
public class LogArray64 implements DynamicArray { 
	private static final byte W = 64;
	
	private long [] data = new long[10];
	private int numbits;
	private long numentries=0;
	private long maxvalue;
	
	public LogArray64() {
		this(W);
	}
	
	public LogArray64(int numbits) {
		this(numbits, 0);
	}
	
	public LogArray64(int numbits, long numentries) {
		this.numentries = 0;
		this.numbits = numbits;
		this.maxvalue = BitUtil.maxVal(numbits);
		
		long size = numWordsFor(numbits, numentries);
		assert size>=0 && size<=Integer.MAX_VALUE;
		
		data = new long[Math.max((int)size,1)];
	}
	
	/** longs required to represent "total" integers of "bitsField" bits each */
	private static final long numWordsFor(int bitsField, long total) {
		return ((bitsField*total+63)/64);
	}
	
	/** Number of bits required for last word */
	protected static final int lastWordNumBits(int bitsField, long total) {
		long totalBits = bitsField*total;
		if(totalBits==0) {
			return 0;
		}
		return (int) ((totalBits-1) % W)+1;	// +1 To have output in the range 1-64, -1 to compensate.
	}
	
	/** Number of bytes required to represent n integers of e bits each */
	private static final long numBytesFor(int bitsField, long total) {
		return (bitsField*total+7)/8;
	}

	 /** Retrieve a given index from array data where every value uses bitsField bits
     * @param data Array
     * @param bitsField Length in bits of each field
     * @param index Position to be retrieved
     */
	private static final long getField(long [] data, int bitsField, long index) {
		if(bitsField==0) return 0;
		
        long bitPos = index*bitsField;
        int i=(int)(bitPos / W);
        int j=(int)(bitPos % W);
        long result;
        if (j+bitsField <= W) {
        	result = (data[i] << (W-j-bitsField)) >>> (W-bitsField);
        } else {
        	result = data[i] >>> j;
        	result = result | (data[i+1] << ( (W<<1) -j-bitsField)) >>> (W-bitsField);
        }
        return result;
	}
	
	/** Store a given value in index into array data where every value uses bitsField bits
     * @param data Array
     * @param bitsField Length in bits of each field
     * @param index Position to store in
     * @param value Value to be stored
     */
	private static final void setField(long [] data, int bitsField, long index, long value) {
		if(bitsField==0) return;
		
		long bitPos = index*bitsField;
		int i=(int)(bitPos/W);
		int j=(int)(bitPos%W);
		
		long mask = ~(~0L << bitsField) << j;
		data[i] = (data[i] & ~mask) | (value << j);
			
		if((j+bitsField>W)) {
			mask = ~0L << (bitsField+j-W);
			data[i+1] = (data[i+1] & mask) | value >>> (W-j);
		}
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
        	assert element<=maxvalue;
        	setField(data, numbits, count, element);
        	count++;
        }
	}

	/* (non-Javadoc)
	 * @see hdt.triples.array.Stream#get(int)
	 */
	@Override
	public long get(long position) {
		if(position<0 || position>=numentries) {
			throw new IndexOutOfBoundsException();
		}
		
		return getField(data, numbits, position);
	}
	
	public void set(long position, long value) {
		if(value<0 || value>maxvalue) {
			throw new IllegalArgumentException("Value exceeds the maximum for this data structure");
		}
		setField(data, numbits, position, value);
	}
	
	public void append(long value) {

		assert numentries<Integer.MAX_VALUE;
		
		if(value<0 || value>maxvalue) {
			throw new IllegalArgumentException("Value exceeds the maximum for this data structure");
		}
		
		long neededSize = numWordsFor(numbits, numentries+1);
		if(data.length<neededSize) {
			resizeArray(data.length*2);
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
		
		assert newbits <= numbits;
		
		if(newbits!=numbits) {
			for(int i=0;i<numentries;i++) {
				long value = getField(data, numbits, i);
				setField(data, newbits, i, value);
			}
			numbits = newbits;
			maxvalue = BitUtil.maxVal(numbits);
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
		output.write(numbits);
		VByte.encode(output, numentries);
		int numwords = (int)numWordsFor(numbits, numentries);
		
		for(int i=0;i<numwords-1;i++) {
			IOUtil.writeLong(output, data[i]);
		}
		
		if(numwords==0) {
			return;
		}
		
		// Write only used bits from last entry (byte aligned, little endian)
		int lastWordUsedBits = lastWordNumBits(numbits, numentries);
		BitUtil.writeLowerBitsByteAligned(data[numwords-1], lastWordUsedBits, output);
	}

	/* (non-Javadoc)
	 * @see hdt.triples.array.Stream#load(java.io.InputStream, hdt.ProgressListener)
	 */
	@Override
	public void load(InputStream input, ProgressListener listener) throws IOException {
		numbits = input.read();
		numentries = VByte.decode(input);
		int numwords = (int)numWordsFor(numbits, numentries);
		
		data = new long[numwords];
		for(int i=0;i<numwords-1;i++) {
			data[i] = IOUtil.readLong(input);
		}
		
		if(numwords==0) {
			return;
		}
		
		// Read only used bits from last entry (byte aligned, little endian)
		int lastWordUsed = lastWordNumBits(numbits, numentries);
		data[numwords-1] = BitUtil.readLowerBitsByteAligned(lastWordUsed, input);
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
