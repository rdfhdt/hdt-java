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

package org.rdfhdt.hdt.compact.sequence;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Iterator;

import org.rdfhdt.hdt.compact.integer.VByte;
import org.rdfhdt.hdt.exceptions.CRCException;
import org.rdfhdt.hdt.exceptions.IllegalFormatException;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.util.BitUtil;
import org.rdfhdt.hdt.util.crc.CRC32;
import org.rdfhdt.hdt.util.crc.CRC8;
import org.rdfhdt.hdt.util.crc.CRCInputStream;
import org.rdfhdt.hdt.util.crc.CRCOutputStream;
import org.rdfhdt.hdt.util.io.CountInputStream;
import org.rdfhdt.hdt.util.io.IOUtil;

/**
 * @author mario.arias
 *
 */
public class SequenceLog64Map implements Sequence,Closeable { 
	private static final byte W = 64;
	private static final int LONGS_PER_BUFFER=128*1024*1024; // 128*8 = 1Gb per chunk.
	private ByteBuffer [] buffers;
	private FileChannel ch;
	private int numbits;
	private long numentries=0;
	private long lastword;
	private int numwords;
	
	public SequenceLog64Map(File f) throws IOException {
		// Read from the beginning of the file
		this(new CountInputStream(new BufferedInputStream(new FileInputStream(f))), f);
	}
	
	public SequenceLog64Map(CountInputStream in, File f) throws IOException {
		CRCInputStream crcin = new CRCInputStream(in, new CRC8());
		
		int type = crcin.read();
		if(type!=SequenceFactory.TYPE_SEQLOG){
			throw new IllegalFormatException("Trying to read a LogArray but the data is not LogArray");
		}
		numbits = crcin.read();
		numentries = VByte.decode(crcin);
		
		if(!crcin.readCRCAndCheck()) {
			throw new CRCException("CRC Error while reading LogArray64 header.");
		}
		
		if(numbits>64) {
			throw new IllegalFormatException("LogArray64 cannot deal with more than 64bit per entry");
		}
		
		long base = in.getTotalBytes();
		
		numwords = (int)SequenceLog64.numWordsFor(numbits, numentries);
		if(numwords>0) {
			IOUtil.skip(in, (numwords-1)*8L);
			// Read only used bits from last entry (byte aligned, little endian)
			int lastWordUsed = SequenceLog64.lastWordNumBits(numbits, numentries);
			lastword = BitUtil.readLowerBitsByteAligned(lastWordUsed, in);
		}
		
		IOUtil.skip(in, 4);
		
		// Read packed data
		ch = new FileInputStream(f).getChannel();
		int buffer = 0;
		int block=0;
		buffers = new ByteBuffer[ 1+numwords/LONGS_PER_BUFFER ];
		while(block<numwords) {
			long current = base+ buffer*8L*LONGS_PER_BUFFER;
			long next = current+8L*LONGS_PER_BUFFER;
			long length = Math.min(ch.size(), next)-current;
//			System.out.println("Ini: "+current+ " Max: "+ next+ " Length: "+length);
			buffers[buffer] = ch.map(MapMode.READ_ONLY, current , length );
			buffers[buffer].order(ByteOrder.LITTLE_ENDIAN);
			
			block+=LONGS_PER_BUFFER;
			buffer++;
		}
	}
	
	private final long getWord(int w) {
		if(w==numwords-1) {
			return lastword;
		}

		ByteBuffer buffer = buffers[w/LONGS_PER_BUFFER];
		return buffer.getLong((w%LONGS_PER_BUFFER)*8);
	}

	/* (non-Javadoc)
	 * @see hdt.triples.array.Stream#get(int)
	 */
	@Override
	public long get(long index) {
		if(index<0 || index>=numentries) {
			throw new IndexOutOfBoundsException();
		}
		if(numbits==0) return 0;
		
		long bitPos = index*numbits;
        int i=(int)(bitPos / W);
        int j=(int)(bitPos % W);
        long result;
        if (j+numbits <= W) {
        	result = (getWord(i) << (W-j-numbits)) >>> (W-numbits);
        } else {
        	result = getWord(i) >>> j;
        	result = result | (getWord(i+1) << ( (W<<1) -j-numbits)) >>> (W-numbits);
        }
        return result;
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
		
		int numwords = (int)SequenceLog64.numWordsFor(numbits, numentries);	
		for(int i=0;i<numwords-1;i++) {
			IOUtil.writeLong(out, getWord(i));
		}
		
		if(numwords>0) {
			// Write only used bits from last entry (byte aligned, little endian)
			int lastWordUsedBits = SequenceLog64.lastWordNumBits(numbits, numentries);
			BitUtil.writeLowerBitsByteAligned(lastword, lastWordUsedBits, out);
		}
		
		out.writeCRC();
	}

	/* (non-Javadoc)
	 * @see hdt.triples.array.Stream#size()
	 */
	@Override
	public long size() {
		return SequenceLog64.numBytesFor(numbits, numentries);
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
	public void add(Iterator<Long> elements) {
		throw new NotImplementedException();
	}

	@Override
	public void load(InputStream input, ProgressListener listener)
			throws IOException {
		throw new NotImplementedException();
	}

	@Override
	public void close() throws IOException {
		ch.close();
	}
}
