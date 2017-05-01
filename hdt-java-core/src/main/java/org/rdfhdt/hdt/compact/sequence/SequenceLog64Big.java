/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/compact/sequence/SequenceLog64.java $
 * Revision: $Rev: 130 $
 * Last modified: $Date: 2013-01-21 00:09:42 +0000 (lun, 21 ene 2013) $
 * Last modified by: $Author: mario.arias $
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

package org.rdfhdt.hdt.compact.sequence;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.rdfhdt.hdt.compact.integer.VByte;
import org.rdfhdt.hdt.exceptions.CRCException;
import org.rdfhdt.hdt.exceptions.IllegalFormatException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.util.BitUtil;
import org.rdfhdt.hdt.util.crc.CRC32;
import org.rdfhdt.hdt.util.crc.CRC8;
import org.rdfhdt.hdt.util.crc.CRCInputStream;
import org.rdfhdt.hdt.util.crc.CRCOutputStream;
import org.rdfhdt.hdt.util.io.IOUtil;

import pl.edu.icm.jlargearrays.LongLargeArray;
import pl.edu.icm.jlargearrays.LargeArrayUtils;

/**
 * @author mario.arias,Lyudmila Balakireva
 *
 */
public class SequenceLog64Big implements DynamicSequence { 
	private static final byte W = 64;
	private static final int INDEX = 1073741824;
	
    LongLargeArray data;
	private int numbits;
	private long numentries=0;
	private long maxvalue;
	
	public SequenceLog64Big() {
		this(W);
	}
	
	public SequenceLog64Big(int numbits) {
		this(numbits, 0);
	}
	
	public SequenceLog64Big(int numbits, long capacity) {
		this.numentries = 0;
		this.numbits = numbits;
		this.maxvalue = BitUtil.maxVal(numbits);
		
		long size = numWordsFor(numbits, capacity);
		LongLargeArray.setMaxSizeOf32bitArray(SequenceLog64Big.INDEX);
		
		data = new LongLargeArray(Math.max((int)size,1)); 
	}
	
	public SequenceLog64Big(int numbits, long capacity, boolean initialize) {
		this(numbits, capacity);
		if(initialize) {
			numentries = capacity;
		}
	}
	
	/** longs required to represent "total" integers of "bitsField" bits each */
	public static long numWordsFor(int bitsField, long total) {
		return ((bitsField*total+63)/64);
	}
	
	/** Number of bits required for last word */
	public static long lastWordNumBits(int bitsField, long total) {
		long totalBits = bitsField*total;
		if(totalBits==0) {
			return 0;
		}
		return (totalBits-1) % W +1;	// +1 To have output in the range 1-64, -1 to compensate.
	}
	
	/** Number of bits required for last word */
	public static long lastWordNumBytes(int bitsField, long total) {
		return ((lastWordNumBits(bitsField, total)-1)/8)+1;	// +1 To have output in the range 1-8, -1 to compensate.
	}

	/** Number of bytes required to represent n integers of e bits each */
	public static long numBytesFor(int bitsField, long total) {
		return (bitsField*total+7)/8;
	}

	/** Retrieve a given index from array data where every value uses bitsField bits
        * @param data Array
        * @param bitsField Length in bits of each field
        * @param index Position to be retrieved
        */
	private static long getField(LongLargeArray data, int bitsField, long index) {
		if(bitsField==0) return 0;
		
        long bitPos = index*bitsField;
        long i= bitPos / W;
        long j= bitPos % W;
        long result;
        if (j+bitsField <= W) {
                result = (data.get(i) << (W-j-bitsField)) >>> (W-bitsField);
        } else {
                result = data.get(i) >>> j;
                result = result | (data.get(i+1) << ( (W<<1) -j-bitsField)) >>> (W-bitsField);
        }
        return result;
	}
	
	/** Store a given value in index into array data where every value uses bitsField bits
        * @param data Array
        * @param bitsField Length in bits of each field
        * @param index Position to store in
        * @param value Value to be stored
        */
	private static void setField(LongLargeArray data, int bitsField, long index, long value) {
		if(bitsField==0) return;
		
		long bitPos = index*bitsField;
		long i= bitPos/W;
		long j= bitPos%W;
		
		long mask = ~(~0L << bitsField) << j;
		data.set(i, (data.getLong(i) & ~mask) | (value << j));
			
		if((j+bitsField>W)) {
			mask = ~0L << (bitsField+j-W);
			data.set(i+1 , (data.get(i+1) & mask) | value >>> (W-j));
		}
	}
	
	private void resizeArray(long size) {
		//data = Arrays.copyOf(data, size);	
		
		LongLargeArray a = new LongLargeArray(size);
		if (size < data.length()) {
			LargeArrayUtils.arraycopy(data, 0, a, 0, size);
			}
			else {
		    LargeArrayUtils.arraycopy(data, 0, a, 0, data.length());
			}
		data = a;
		
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
			long val = elements.next();
			max = val>max ? val : max;
			numentries++;
		}
		
        // Prepare array
        numbits = BitUtil.log2(max);
        long size = numWordsFor(numbits, numentries);
        data = new LongLargeArray(size);

        // Save
        int count = 0;
        while(elements.hasNext()) {
            long element = elements.next();
            assert element<=maxvalue;
            setField(data, numbits, count, element);
            count++;
        }
	}

	public void addIntegers(ArrayList<Integer> elements) {
		long max = 0;
		numentries = 0;
		
		// Count and calculate number of bits needed per element.
		for (int i=0;i<elements.size();i++){
			long val = elements.get(i).longValue();
			max = val>max ? val : max;
			numentries++;
		}
		
        // Prepare array
        numbits = BitUtil.log2(max);
        long size = numWordsFor(numbits, numentries);
        data = new LongLargeArray(size);

        // Save
        int count = 0;
        for (int i=0;i<elements.size();i++){
            long element = elements.get(i).longValue();
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
//		if(position<0 || position>=numentries) {
			//System.out.println("pos, numentries:"+position+","+numentries);
			//throw new IndexOutOfBoundsException();
//		}
		
		return getField(data, numbits, position);
	}
	
	@Override
    public void set(long position, long value) {
		//if(value<0 || value>maxvalue) {
			//throw new IllegalArgumentException("Value exceeds the maximum for this data structure");
		//}
		setField(data, numbits, position, value);
	}
	
	@Override
    public void append(long value) {

		//assert numentries<Integer.MAX_VALUE;
		
		//if(value<0 || value>maxvalue) {
			//throw new IllegalArgumentException("Value exceeds the maximum for this data structure");
		//}
		
		long neededSize = numWordsFor(numbits, numentries+1);
		//System.out.println("append needed size:"+neededSize);
		if(data.length()<neededSize) {
			resizeArray(data.length()*2);
		}
		
		this.set(numentries, value);
		numentries++;
	}
	
	@Override
    public void aggressiveTrimToSize() {
		long max = 0;
		// Count and calculate number of bits needed per element.
		for(long i=0; i<numentries; i++) {
			long value = this.get(i);
			max = value>max ? value : max;
		}
		int newbits = BitUtil.log2(max);
		
		assert newbits <= numbits;
		//System.out.println("newbits"+newbits);
		if(newbits!=numbits) {
			for(long i=0;i<numentries;i++) {
				long value = getField(data, numbits, i);
				setField(data, newbits, i, value);
			}
			numbits = newbits;
			maxvalue = BitUtil.maxVal(numbits);
			
			long totalSize = numWordsFor(numbits, numentries);
			
			if (totalSize!=data.length()){
				resizeArray((int)totalSize);
			}
		}

	}
	
	@Override
    public void trimToSize() {
		resizeArray(numWordsFor(numbits, numentries));
	}
	
	public void resize(long numentries) {
		this.numentries = numentries;
		resizeArray(numWordsFor(numbits, numentries));
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
		
		CRCOutputStream out = new CRCOutputStream(output, new CRC8());
		
		out.write(SequenceFactory.TYPE_SEQLOG);
		out.write(numbits);
		VByte.encode(out, numentries);
		
		out.writeCRC();
		
		out.setCRC(new CRC32());
		
		long numwords = numWordsFor(numbits, numentries);
		for(long i=0;i<numwords-1;i++) {
			IOUtil.writeLong(out, data.getLong(i));
		}
		
		if(numwords>0) {
			// Write only used bits from last entry (byte aligned, little endian)
			long lastWordUsedBits = lastWordNumBits(numbits, numentries);
			BitUtil.writeLowerBitsByteAligned(data.get(numwords-1), lastWordUsedBits, out);
		}
		
		out.writeCRC();
	}

	/* (non-Javadoc)
	 * @see hdt.triples.array.Stream#load(java.io.InputStream, hdt.ProgressListener)
	 */
	@SuppressWarnings("resource")
	@Override
	public void load(InputStream input, ProgressListener listener) throws IOException {
		CRCInputStream in = new CRCInputStream(input, new CRC8());
		
		int type = in.read();
		if(type!=SequenceFactory.TYPE_SEQLOG){
			throw new IllegalFormatException("Trying to read a LogArray but the data is not LogArray");
		}
		numbits = in.read();
		numentries = VByte.decode(in);
		
		if(!in.readCRCAndCheck()) {
			throw new CRCException("CRC Error while reading LogArray64 header.");
		}
		
		if(numbits>64) {
			throw new IllegalFormatException("LogArray64 cannot deal with more than 64bit per entry");
		}
		
		in.setCRC(new CRC32());
		
		long numwords = numWordsFor(numbits, numentries);
		data = new LongLargeArray(numwords);
		for(long i=0;i<numwords-1;i++) {
			data.set(i , IOUtil.readLong(in));
		}
		
		if(numwords>0) {
			// Read only used bits from last entry (byte aligned, little endian)
			long lastWordUsed = lastWordNumBits(numbits, numentries);
			data.set(numwords-1 , BitUtil.readLowerBitsByteAligned(lastWordUsed, in));
		}
		
		if(!in.readCRCAndCheck()) {
			throw new CRCException("CRC Error while reading LogArray64 data.");
		}
	}

	/* (non-Javadoc)
	 * @see hdt.triples.array.Stream#size()
	 */
	@Override
	public long size() {
		return numBytesFor(numbits, numentries);
	}
	
	public long getRealSize() {
		return data.length()*8L;
	}
	
	public int getNumBits() {
		return numbits;
	}

	/* (non-Javadoc)
	 * @see hdt.compact.array.Stream#getType()
	 */
	@Override
	public String getType() {
		return HDTVocabulary.SEQ_TYPE_LOG;
	}

	@Override
	public void close() throws IOException {
		data=null;
	}
}
