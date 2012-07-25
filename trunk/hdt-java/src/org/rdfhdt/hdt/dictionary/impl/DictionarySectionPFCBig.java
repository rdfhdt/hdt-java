/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/dictionary/impl/DictionarySectionPFC.java $
 * Revision: $Rev: 30 $
 * Last modified: $Date: 2012-07-23 11:59:21 +0100 (lun, 23 jul 2012) $
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

package org.rdfhdt.hdt.dictionary.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import org.rdfhdt.hdt.compact.array.LogArray64;
import org.rdfhdt.hdt.compact.integer.VByte;
import org.rdfhdt.hdt.dictionary.DictionarySection;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.util.Mutable;
import org.rdfhdt.hdt.util.string.ByteStringUtil;
import org.rdfhdt.hdt.util.string.ReplazableString;

/**
 * Implementation of Plain Front Coding that stores each block in its own array, therefore
 * overcoming Java's limitation of 2Gb for a single array.
 * 
 *  It allows loading much bigger files, but wastes a lot of memory in pointers to the blocks.
 *  
 *  TODO: Make it gather a few blocks in each array.
 *  
 * @author mario.arias
 *
 */
public class DictionarySectionPFCBig implements DictionarySection {
	public static final int TYPE_INDEX = 2;
	public static final int DEFAULT_BLOCK_SIZE = 32;
	
	byte [][] data;
	protected int blocksize;
	protected int numstrings;
	protected long size;
	
	public DictionarySectionPFCBig(HDTSpecification spec) {

	}
	
	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#load(hdt.dictionary.DictionarySection)
	 */
	@Override
	public void load(DictionarySection other, ProgressListener listener) {
		throw new NotImplementedException();
	}
	
	/**
	 * Locate the block of a string doing binary search.
	 */
	private int locateBlock(CharSequence str) {	
		int low = 0;
		int high = data.length - 1;
		
		while (low <= high) {
			int mid = (low + high) >>> 1;

			int cmp = ByteStringUtil.strcmp(str, data[mid], 0);
			//System.out.println("Comparing against block: "+ mid + " which is "+ ByteStringUtil.asString(data[mid], 0)+ " Result: "+cmp);

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
	public int locate(CharSequence str) {

		int blocknum = locateBlock(str);
		if(blocknum>=0) {
			// Located exactly
			return (blocknum*blocksize)+1;
		} else {
			// Not located exactly.
			blocknum = -blocknum-2;
			
			if(blocknum>=0) {
				int idblock = locateInBlock(data[blocknum], str);

				if(idblock != 0) {
					return (blocknum*blocksize)+idblock+1;
				}
			}
		}
		
		// Not found
		return 0;
	}
		
	private int locateInBlock(byte[] block, CharSequence str) {
		
		int pos = 0;
		ReplazableString tempString = new ReplazableString();
		
		Mutable<Long> delta = new Mutable<Long>(0L);
		int idInBlock = 0;
		int cshared=0;
		
		// Read the first string in the block
		int slen = ByteStringUtil.strlen(block, pos);
		tempString.append(block, pos, slen);
		pos+=slen+1;
		idInBlock++;
		
		while( (idInBlock<blocksize) && (pos<block.length)) 
		{
			// Decode prefix
			pos += VByte.decode(block, pos, delta);
			
			// Copy suffix
			slen = ByteStringUtil.strlen(block, pos);
			tempString.replace(delta.getValue().intValue(), block, pos, slen);
			
			if(delta.getValue()>=cshared)
			{
				// Current delta value means that this string
				// has a larger long common prefix than the previous one
				cshared += ByteStringUtil.longestCommonPrefix(tempString, str, cshared);
				
				if((cshared==str.length()) && (tempString.length()==str.length())) {
					break;
				}
			} else {
				// We have less common characters than before, 
				// this string is bigger that what we are looking for.
				// i.e. Not found.
				idInBlock = 0;
				break;
			}
			pos+=slen+1;
			idInBlock++;
			
		}

		// Not found
		if(pos==block.length || idInBlock== blocksize) {
			idInBlock=0;
		}
		
		return idInBlock;
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#extract(int)
	 */
	@Override
	public CharSequence extract(int id) {
		
		if(id<1 || id>numstrings) {
			return null;
		}
		
		// Locate block
		int nblock = (id-1)/blocksize;
		int nstring = (id-1)%blocksize;
		int pos = 0;
		byte [] block=data[nblock];
		
		// Copy first string
 		int len = ByteStringUtil.strlen(block, pos);
		
		Mutable<Long> delta = new Mutable<Long>(0L);
		ReplazableString tempString = new ReplazableString();
		tempString.append(block, pos, len);
		
		// Copy strings untill we find our's.
		for(int i=0;i<nstring;i++) {
			pos+=len+1;
			pos += VByte.decode(block, pos, delta);
			len = ByteStringUtil.strlen(block, pos);
			tempString.replace(delta.getValue().intValue(), block, pos, len);
		}
		return tempString;
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#size()
	 */
	@Override
	public long size() {
		return size;
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#getNumberOfElements()
	 */
	@Override
	public int getNumberOfElements() {
		return numstrings;
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#getEntries()
	 */
	@Override
	public Iterator<CharSequence> getSortedEntries() {
		return new Iterator<CharSequence>() {
			int pos = 0;

			@Override
			public boolean hasNext() {
				return pos<getNumberOfElements();
			}

			@Override
			public CharSequence next() {
				// FIXME: It is more efficient to go through each block, each entry.
				pos++;
				return extract(pos);
			}

			@Override
			public void remove() {
				throw new NotImplementedException();
			}
		};
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#save(java.io.OutputStream, hdt.ProgressListener)
	 */
	@Override
	public void save(OutputStream output, ProgressListener listener) throws IOException {
		throw new NotImplementedException();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#load(java.io.InputStream, hdt.ProgressListener)
	 */
	@Override
	public void load(InputStream input, ProgressListener listener) throws IOException {
		numstrings = (int) VByte.decode(input);
		this.size = VByte.decode(input);
		blocksize = (int)VByte.decode(input);
		
		// Load block pointers
		LogArray64 blocks = new LogArray64();
		blocks.load(input, listener);
		
		// Initialize global block array
		int nblocks = (int)blocks.getNumberOfElements()-1;
		data = new byte[nblocks][];
		
		// Read block by block
		long previous = 0;
		long current = 0;
		for(int i=0;i<nblocks;i++) {
			current = blocks.get(i+1);
			//System.out.println("Loding block: "+i+" from "+previous+" to "+ current+" of size "+ (current-previous));
			data[i]=new byte[(int)(current-previous)];
			
			int read = input.read(data[i]);
			if(read!=data[i].length) {
				throw new IOException("Error reading from input");
			}
			previous=current;	
		}
	}
}
