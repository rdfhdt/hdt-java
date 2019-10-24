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

import org.rdfhdt.hdt.compact.integer.VByte;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.util.BitUtil;
import org.rdfhdt.hdt.util.crc.CRC32;
import org.rdfhdt.hdt.util.crc.CRC8;
import org.rdfhdt.hdt.util.crc.CRCOutputStream;
import org.rdfhdt.hdt.util.disk.LongArrayDisk;
import org.rdfhdt.hdt.util.io.IOUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Version of Bitmap64 which is backed up on disk
 */
public class Bitmap64Disk {

    // Constants
    protected final static int LOGW = 6;
    protected final static int W = 64;

    // Variables
    protected long numbits;
    protected LongArrayDisk words;

    public Bitmap64Disk(String location) {
        this(location, W);
    }

    public Bitmap64Disk(String location, long nbits) {
        this.numbits = 0;
        this.words = new LongArrayDisk(location,numWords(nbits));
    }

    /**
     * Given a bit index, return word index containing it.
     */
    protected static int wordIndex(long bitIndex) {
        return (int) (bitIndex >>> LOGW);
    }

    public static long numWords(long numbits) {
        return ((numbits-1)>>>LOGW) + 1;
    }

    public static long numBytes(long numbits) {
        return ((numbits-1)>>>3) + 1;
    }

    protected static int lastWordNumBits(long numbits) {
        if(numbits==0) {
            return 0;
        }
        return (int) ((numbits-1) % W)+1;	// +1 To have output in the range 1-64, -1 to compensate.
    }

    protected final void ensureSize(int wordsRequired) {
        words.resize(Math.max(words.getSize()*2, wordsRequired));
    }

    public void trim(long numbits) {
        this.numbits = numbits;
    }

    public void trimToSize() {
        int wordNum = (int) numWords(numbits);
        if(wordNum!=words.length()) {
            words.resize(wordNum);
        }
    }

    public boolean access(long bitIndex) {
        if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

        int wordIndex = wordIndex(bitIndex);
        if(wordIndex>=words.length()) {
            return false;
        }

        return (words.get(wordIndex) & (1L << bitIndex)) != 0;
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
            words.set(wordIndex, words.get(wordIndex) | (1L << bitIndex));
        } else {
            words.set(wordIndex,words.get(wordIndex) & ~(1L << bitIndex));
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
        if (wordIndex >= words.length())
            return -1;

        long word = words.get(wordIndex) & (~0L << fromIndex);

        while (true) {
            if (word != 0)
                return ((long)wordIndex * W) + Long.numberOfTrailingZeros(word);
            if (++wordIndex == words.length())
                return -1;
            word = words.get(wordIndex);
        }
    }

    public long getWord(int word) {
        return words.get(word);
    }

    public long getNumBits() {
        return numbits;
    }

    public long getSizeBytes() {
        return numWords(numbits)*8;
    }

    public void save(OutputStream output, ProgressListener listener) throws IOException {
        CRCOutputStream out = new CRCOutputStream(output, new CRC8());

        // Write Type and Numbits
        out.write(BitmapFactory.TYPE_BITMAP_PLAIN);
        VByte.encode(out, numbits);

        // Write CRC
        out.writeCRC();

        // Setup new CRC
        out.setCRC(new CRC32());
        int numwords = (int) numWords(numbits);
        for(int i=0;i<numwords-1;i++) {
            IOUtil.writeLong(out, words.get(i));
        }

        if(numwords>0) {
            // Write only used bits from last entry (byte aligned, little endian)
            int lastWordUsed = lastWordNumBits(numbits);
            BitUtil.writeLowerBitsByteAligned(words.get(numwords-1), lastWordUsed, out);
        }

        out.writeCRC();
    }

    @SuppressWarnings("resource")
    public void load(InputStream input, ProgressListener listener) throws IOException { }
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

    public long getRealSizeBytes() {
        return words.length()*8;
    }
}