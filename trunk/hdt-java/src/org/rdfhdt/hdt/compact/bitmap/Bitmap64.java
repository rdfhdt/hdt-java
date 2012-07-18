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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.util.BitUtil;
import org.rdfhdt.hdt.util.io.IOUtil;

/**
 * @author mario.arias
 *
 */
public class Bitmap64 {
	
	// Constants
	public final static int LOGW = 6;
	public final static int W = 64;
	
	// Variables
	protected long numbits;
	protected long [] words;
    
	public Bitmap64() {
		this(W);
	}
	
	public Bitmap64(long nbits) {
		this.numbits = 0;
		this.words = new long[numWords(nbits)];
	}
	
	public Bitmap64(Bitmap64 other) {
		this(other, other.getNumBits());
	}
	
	public Bitmap64(Bitmap64 other, long numbits) {
		this.numbits = numbits;
		this.words = new long[numWords(numbits)];
		if(this.words.length>0) {
			this.words[this.words.length-1] = 0;
		}
		System.arraycopy(other.words, 0, this.words, 0, words.length);
	}
	
	/**
     * Given a bit index, return word index containing it.
     */
	protected static final int wordIndex(long bitIndex) {
        return (int) (bitIndex >>> LOGW);
    }
    
    protected static final int numWords(long numbits) {
    	return (int) ((numbits-1)>>>LOGW) + 1;
    }
    
    protected static final int numBytes(long numbits) {
    	return (int) ((numbits-1)>>>3) + 1;
    }
    
    protected static final int lastWordNumBits(long numbits) {
    	if(numbits==0) {
    		return 0;
    	}
    	return (int) ((numbits-1) % W)+1;	// +1 To have output in the range 1-64, -1 to compensate.
    }

	protected final void ensureSize(int wordsRequired) {
		if(words.length<wordsRequired) {
			long [] newWords = new long[Math.max(words.length*2, wordsRequired)];
			System.arraycopy(words, 0, newWords, 0, Math.min(words.length, newWords.length));
			words = newWords;
		}
	}
	
	public void trim(long numbits) {
		this.numbits = numbits;
	}
	
	protected void trimToSize() {
		int wordNum = numWords(numbits);
		if(wordNum!=words.length) {
			words = Arrays.copyOf(words, wordNum);
		}
	}

	public boolean access(long bitIndex) {
		if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

        int wordIndex = wordIndex(bitIndex);
        if(wordIndex>=words.length) {
        	return false;
        }
        
        return ((words[wordIndex] & (1L << bitIndex)) != 0);
	}
	
	/* (non-Javadoc)
	 * @see hdt.compact.bitmap.ModifiableBitmap#append(boolean)
	 */
	public void append(boolean value) {
		this.set(numbits++, value );
	}
	
	public void set(long bitIndex, boolean value) {		
		if (bitIndex < 0)
			throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

		int wordIndex = wordIndex(bitIndex);
		ensureSize(wordIndex+1);
	
		if(value) {
			words[wordIndex] |= (1L << bitIndex);
		} else {
			words[wordIndex] &= ~(1L << bitIndex);
		}
		
		this.numbits = Math.max(this.numbits, bitIndex+1);
	}

	public long selectPrev1(long start) {
		throw new NotImplementedException();
	}
	
	public long selectNext1(long fromIndex) {
		if (fromIndex < 0)
	        throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);

		int wordIndex = wordIndex(fromIndex);
		if (wordIndex >= words.length)
			return -1;

		long word = words[wordIndex] & (~0L << fromIndex);

		while (true) {
			if (word != 0)
				return (wordIndex * W) + Long.numberOfTrailingZeros(word);
			if (++wordIndex == words.length)
				return -1;
			word = words[wordIndex];
		}
	}

	public long getWord(int word) {
		return words[word];
	}
	
	public long getNumBits() {
		return numbits;
	}
	
	public long getSizeBytes() {
		return numWords(numbits)*8;
	}

	public void save(OutputStream output, ProgressListener listener) throws IOException {
		output.write((byte)7); // Code for BitSequence375 in C++
		IOUtil.writeLong(output, numbits);
		int numwords = numWords(numbits);
		for(int i=0;i<numwords-1;i++) {
			IOUtil.writeLong(output, words[i]);
		}
		
		if(numwords==0) {
			return;
		}
		
		// Write only used bits from last entry (byte aligned, little endian)
		int lastWordUsed = lastWordNumBits(numbits);
		BitUtil.writeLowerBitsByteAligned(words[numwords-1], lastWordUsed, output);
	}

	public void load(InputStream input, ProgressListener listener) throws IOException {
		int type = input.read();
		if(type!=7) {
			throw new IllegalArgumentException("Trying to read Bitmap64 on a section that is not Bitmap64");
		}
		numbits = IOUtil.readLong(input);
		int numwords = numWords(numbits);
		words = new long[numwords];
		for(int i=0;i<numwords-1;i++) {
			words[i] = IOUtil.readLong(input);
		}
		
		if(numwords==0) {
			return;
		}
		
		// Read only used bits from last entry (byte aligned, little endian)
		int lastWordUsedBits = lastWordNumBits(numbits);
		words[numwords-1] = BitUtil.readLowerBitsByteAligned(lastWordUsedBits, input);
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		for(long i=0;i<numbits;i++) {
			str.append(access(i) ? '1' : '0');
		}
		return str.toString();
	}
}
