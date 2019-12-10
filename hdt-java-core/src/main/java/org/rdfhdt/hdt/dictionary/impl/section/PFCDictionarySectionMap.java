/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/dictionary/impl/section/PFCDictionarySection.java $
 * Revision: $Rev: 94 $
 * Last modified: $Date: 2012-11-20 23:44:36 +0000 (mar, 20 nov 2012) $
 * Last modified by: $Author: mario.arias $
 *
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
 *   Mario Arias:               mario.arias@deri.org
 *   Javier D. Fernandez:       jfergar@infor.uva.es
 *   Miguel A. Martinez-Prieto: migumar2@infor.uva.es
 *   Alejandro Andres:          fuzzy.alej@gmail.com
 */

package org.rdfhdt.hdt.dictionary.impl.section;

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
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;

import org.rdfhdt.hdt.compact.integer.VByte;
import org.rdfhdt.hdt.compact.sequence.Sequence;
import org.rdfhdt.hdt.compact.sequence.SequenceFactory;
import org.rdfhdt.hdt.dictionary.DictionarySectionPrivate;
import org.rdfhdt.hdt.dictionary.TempDictionarySection;
import org.rdfhdt.hdt.exceptions.CRCException;
import org.rdfhdt.hdt.exceptions.IllegalFormatException;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.util.crc.CRC8;
import org.rdfhdt.hdt.util.crc.CRCInputStream;
import org.rdfhdt.hdt.util.io.CountInputStream;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.string.ByteStringUtil;
import org.rdfhdt.hdt.util.string.CompactString;
import org.rdfhdt.hdt.util.string.ReplazableString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mario.arias
 * @author Dennis Diefenbach
 */
public class PFCDictionarySectionMap implements DictionarySectionPrivate,Closeable {
	private static final Logger log = LoggerFactory.getLogger(PFCDictionarySectionMap.class);

	public static final int TYPE_INDEX = 2;
	public static final int DEFAULT_BLOCK_SIZE = 16;
	
	static final int BLOCKS_PER_BYTEBUFFER = 50000;
	protected FileChannel ch;
	protected ByteBuffer [] buffers; // Encoded sequence
	long [] posFirst;	// Global byte position of the start of each buffer
	protected int blocksize;
	protected long numstrings;
	protected Sequence blocks;
	protected FileInputStream fis; 
	protected long dataSize;

	private final File f;
	private final long startOffset;
    private long endOffset;

	@SuppressWarnings("resource")
	public PFCDictionarySectionMap(CountInputStream input, File f) throws IOException {
		this.f = f;
		startOffset=input.getTotalBytes();

		CRCInputStream crcin = new CRCInputStream(input, new CRC8());
		
		// Read type
		int type = crcin.read();
		if(type!=TYPE_INDEX) {
			throw new IllegalFormatException("Trying to read a DictionarySectionPFC from data that is not of the suitable type");
		}
		
		// Read vars
		numstrings = VByte.decode(crcin);
		dataSize = VByte.decode(crcin);
		blocksize = (int) VByte.decode(crcin);		
	
		if(!crcin.readCRCAndCheck()) {
			throw new CRCException("CRC Error while reading Dictionary Section Plain Front Coding Header.");
		}
		
		// Read blocks
		blocks = SequenceFactory.createStream(input, f);
//		blocks = SequenceFactory.createStream(input);
//		blocks.load(input, null);
		
		long base = input.getTotalBytes();
		IOUtil.skip(crcin, dataSize+4); // Including CRC32

		endOffset = input.getTotalBytes();

		// Read packed data
		ch = FileChannel.open(Paths.get(f.toString()));
		long block = 0;
		int buffer = 0;
		long numBlocks = blocks.getNumberOfElements();
		long bytePos = 0;
		long numBuffers = 1L+numBlocks/BLOCKS_PER_BYTEBUFFER;
		buffers = new ByteBuffer[(int)numBuffers ];
		posFirst = new long[(int)numBuffers];
		
//		System.out.println("Buffers "+buffers.length);
		while(block<numBlocks-1) {
			int nextBlock = (int) Math.min(numBlocks-1, block+BLOCKS_PER_BYTEBUFFER);
			long nextBytePos = blocks.get(nextBlock);
			
//			System.out.println("From block "+block+" to "+nextBlock);
//			System.out.println("From pos "+ bytePos+" to "+nextBytePos);
//			System.out.println("Total size: "+ (nextBytePos-bytePos));
			buffers[buffer] = ch.map(MapMode.READ_ONLY, base+bytePos, nextBytePos-bytePos);
			buffers[buffer].order(ByteOrder.LITTLE_ENDIAN);
			
			posFirst[buffer] = bytePos;
			
			bytePos = nextBytePos;
			block+=BLOCKS_PER_BYTEBUFFER;
			buffer++;
		}
	}

	private long locateBlock(CharSequence str) {
		if(blocks.getNumberOfElements()==0) {
			return -1;
		}
		
		long low = 0;
		long high = blocks.getNumberOfElements()-1;
		long max = high;
		
		while (low <= high) {
			long mid = low + (high - low)/2;
		
			int cmp;
			if(mid==max) {
				cmp=-1;
			} else {
				ByteBuffer buffer = buffers[(int) (mid/BLOCKS_PER_BYTEBUFFER)];
				cmp = ByteStringUtil.strcmp(str, buffer, (int)(blocks.get(mid)-posFirst[(int) (mid/BLOCKS_PER_BYTEBUFFER)]));
			}
			if (cmp<0) {
				high = mid - 1;
			} else if (cmp > 0) {
				low = mid + 1;
			} else {
				return mid; // key found
			}
		}
		return -(low + 1);  // key not found.
	}
	
	
	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#locate(java.lang.CharSequence)
	 */
	@Override
	public long locate(CharSequence str) {
		if(buffers==null || blocks==null) {
			return 0;
		}
		
		long blocknum = locateBlock(str);
		if(blocknum>=0) {
			// Located exactly
			return (blocknum*blocksize)+1;
		} else {
			// Not located exactly.
			blocknum = -blocknum-2;
			
			if(blocknum>=0) {
				long idblock = locateInBlock(blocknum, str);

				if(idblock != 0) {
					return (blocknum*blocksize)+idblock+1;
				}
			}
		}
		
		return 0;
	}
	
	public int locateInBlock(long block, CharSequence str) {
		if(block>=blocks.getNumberOfElements()) {
			return 0;
		}
		
		ReplazableString tempString = new ReplazableString();
		
		int idInBlock = 0;
		int cshared=0;
		
//		dumpBlock(block);

		ByteBuffer buffer = buffers[(int) (block/BLOCKS_PER_BYTEBUFFER)].duplicate();
		buffer.position((int)(blocks.get(block)-posFirst[(int) (block/BLOCKS_PER_BYTEBUFFER)]));
		
		try {
			if(!buffer.hasRemaining()) {
				return 0;
			}
			
			// Read the first string in the block
			tempString.replace(buffer, 0);

			idInBlock++;

			while( (idInBlock<blocksize) && buffer.hasRemaining()) 
			{
				// Decode prefix
				long delta = VByte.decode(buffer);

				//Copy suffix
				tempString.replace(buffer, (int) delta);

				if(delta>=cshared)
				{
					// Current delta value means that this string
					// has a larger long common prefix than the previous one
					cshared += ByteStringUtil.longestCommonPrefix(tempString, str, cshared);

					if((cshared==str.length()) && (tempString.length()==str.length())) {
						return idInBlock;
					}
				} else {
					// We have less common characters than before, 
					// this string is bigger that what we are looking for.
					// i.e. Not found.
					return 0;
				}
				idInBlock++;
			}
			return 0;
		} catch (IOException e) {
			log.error("Unexpected exception.", e);
			return 0;
		}
	}
	
	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#extract(int)
	 */
	@Override
	public CharSequence extract(long id) {
		if(buffers==null || blocks==null) {
			return null;
		}
		
		if(id<1 || id>numstrings) {
			return null;
		}
		
		long block = (id-1)/blocksize;
		ByteBuffer buffer = buffers[(int) (block/BLOCKS_PER_BYTEBUFFER)].duplicate();
		buffer.position((int)(blocks.get(block)-posFirst[(int) (block/BLOCKS_PER_BYTEBUFFER)]));
		
		try {
			ReplazableString tempString = new ReplazableString();
			tempString.replace(buffer,0);

			long stringid = (id-1)%blocksize;
			for(long i=0;i<stringid;i++) {
				long delta = VByte.decode(buffer);
				tempString.replace(buffer, (int) delta);
			}
			return new CompactString(tempString).getDelayed();
		} catch (IOException e) {
			log.error("Unexpected exception.", e);
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#size()
	 */
	@Override
	public long size() {
		return dataSize+blocks.size();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#getNumberOfElements()
	 */
	@Override
	public long getNumberOfElements() {
		return numstrings;
	}

	@Override
	public Iterator<CharSequence> getSortedEntries() {
		if (buffers[0]==null){
			return Collections.emptyIterator();
		} else {
			return new Iterator<CharSequence>() {
				long id = 0;

				final ReplazableString tempString = new ReplazableString();
				int bytebufferIndex;

				ByteBuffer buffer = buffers[0].duplicate();

				@Override
				public boolean hasNext() {
					return id<getNumberOfElements();
				}

				@Override
				public CharSequence next() {
					if(!buffer.hasRemaining()) {
						buffer = buffers[++bytebufferIndex].duplicate();
						buffer.rewind();
					}
					try {
						if((id%blocksize)==0) {
							tempString.replace(buffer, 0);
						} else {
							long delta = VByte.decode(buffer);
							tempString.replace(buffer, (int) delta);
						}
						id++;
						return new CompactString(tempString).getDelayed();
//					return tempString.toString();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
	}

	@Override
	public void close() throws IOException {
		blocks.close();
		buffers = null;
		ch.close();
	}

	@Override
	public void load(TempDictionarySection other, ProgressListener listener) {
		throw new NotImplementedException();
	}

	@Override
	public void save(OutputStream output, ProgressListener listener) throws IOException {
		InputStream in = new BufferedInputStream(new FileInputStream(f));
		IOUtil.skip(in, startOffset);
		IOUtil.copyStream(in, output, endOffset-startOffset);
		in.close();
	}

	@Override
	public void load(InputStream input, ProgressListener listener)
			throws IOException {
		throw new NotImplementedException();
	}
}
