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

package org.rdfhdt.hdt.dictionary.impl;

import org.rdfhdt.hdt.compact.array.LogArray64;
import org.rdfhdt.hdt.compact.integer.VByte;
import org.rdfhdt.hdt.dictionary.DictionarySection;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.util.BitUtil;
import org.rdfhdt.hdt.util.StopWatch;
import org.rdfhdt.hdt.util.StringUtil;
import org.rdfhdt.hdt.util.io.CountOutputStream;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.string.ByteStringUtil;
import org.rdfhdt.hdt.util.string.CompactString;
import org.rdfhdt.hdt.util.string.ReplazableString;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.zip.DeflaterOutputStream;

/**
 * @author mck
 *
 */
public class DictionarySectionDeflate implements DictionarySection {
	public static final byte TYPE_INDEX = 4;
	protected int blocksize=32;
	protected int numstrings;
	protected byte [] text;
	protected LogArray64 blocks;
	
	
	private int locateBlock(CharSequence str) {
		if(blocks.getNumberOfElements()==0) {
			return 0;
		}
		
		int left=0, right=(int)blocks.getNumberOfElements()-1, center=0;
		int cmp=0;
		
		while(left<=right) {
			center = (left+right)/2;
		
			cmp = ByteStringUtil.strcmp(str, text, (int)blocks.get(center));
//			System.out.println("Comparing against block: "+ center + " which is "+ ByteStringUtil.asString(text, (int)blocks.get(center))+ " Result: "+cmp);
			
			if(cmp<0) {
				right = center-1;
			} else {
				if(cmp>0) {
					left = center+1;
				} else {
					return center;
				}
			}	
		}
		
		return Math.max(0, cmp>0 ? center : center-1);
	}

	private void dumpAll() {
		for(int i=0;i<blocks.getNumberOfElements();i++) {
			dumpBlock(i);
		}
	}
	
	private void dumpBlock(int block) {
		if(text==null || blocks==null || block>=blocks.getNumberOfElements()) {
			return;
		}
		
//		System.out.println("Dump block "+block);
//		ReplazableString tempString = new ReplazableString();
//
//		int idInBlock = 0;
//			
//		int pos = (int)blocks.get(block);
//		
//		// Copy first string
//		int len = ByteStringUtil.strlen(text, pos);
//		tempString.append(text, pos, len);
//		pos+=len+1;
//		
//		System.out.println((block*blocksize+idInBlock)+ " ("+idInBlock+") => "+ tempString);
//		idInBlock++;
//		
//		while( (idInBlock<blocksize) && (pos<text.length)) {
//			pos += VByte.decode(text, pos, delta);
//			
//			len = ByteStringUtil.strlen(text, pos);
//			tempString.replace(delta.getValue(), text, pos, len);
//			
//			System.out.println((block*blocksize+idInBlock)+ " ("+idInBlock+") => "+ tempString + " Delta="+delta.getValue()+ " Len="+len);
//			
//			pos+=len+1;
//			idInBlock++;
//		}
	}
	
	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#locate(java.lang.CharSequence)
	 */
	@Override
	public int locate(CharSequence s) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#extract(int)
	 */
	@Override
	public CharSequence extract(int pos) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#size()
	 */
	@Override
	public long size() {
		return text.length+blocks.size();
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
	public Iterator<CharSequence> getEntries() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#save(java.io.OutputStream, hdt.listener.ProgressListener)
	 */
	@Override
	public void save(OutputStream output, ProgressListener listener)
			throws IOException {
		output.write(TYPE_INDEX);
		
		VByte.encode(output, numstrings);
		VByte.encode(output, text.length);
		//VByte.encode(output, blocksize);
		
		IOUtil.writeBuffer(output, text, 0, text.length, listener);

		blocks.save(output, listener);
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#load(java.io.InputStream, hdt.listener.ProgressListener)
	 */
	@Override
	public void load(InputStream input, ProgressListener listener) throws IOException {
		numstrings = VByte.decode(input);
		int bytes = VByte.decode(input);
		//blocksize = VByte.decode(input);
		
		text = IOUtil.readBuffer(input, bytes, listener);
		blocks = new LogArray64();
		blocks.load(input, listener);
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#load(hdt.dictionary.DictionarySection, hdt.listener.ProgressListener)
	 */
	@Override
	public void load(DictionarySection other, ProgressListener listener) {
		// TODO Auto-generated method stub
		this.blocks = new LogArray64(BitUtil.log2(other.size()), other.getNumberOfElements()/blocksize);
		this.numstrings = 0;
		long plainSize = 0;
		
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		CountOutputStream countOut = new CountOutputStream(byteOut);
		DeflaterOutputStream deflateOutputStream = new DeflaterOutputStream(countOut);
		
		blocksize=1000;
		
		try {
			StopWatch st = new StopWatch();
			Iterator<CharSequence> it = other.getEntries();
			while(it.hasNext()) {
				CharSequence str = it.next();
//				System.out.println(str);
				
				byte [] bytes;
				if(str instanceof CompactString) {
					bytes = ((CompactString) str).getData();
				} else if(str instanceof ReplazableString) {
					bytes = ((ReplazableString) str).getBuffer();
				} else {
					bytes = str.toString().getBytes();
				}
			
				if(numstrings%blocksize==0) {
					// Flush previous block.
					deflateOutputStream.finish();
					countOut.flush();
					
					// save position.
					blocks.append(countOut.getTotalBytes());
					
					// Write first string of block in plain format.
					countOut.write(bytes);
					
					// Start new deflater for the rest of the block.
					deflateOutputStream = new DeflaterOutputStream(countOut) { { def.setLevel(1); } };
				} else {
					deflateOutputStream.write(bytes);
				}
				
				numstrings++;
				plainSize+=bytes.length;
			}
			
			// Close output
			countOut.close();
			blocks.aggresiveTrimToSize();
			
			this.text = byteOut.toByteArray();
			
//			System.out.println("Plain size: "+plainSize);
//			System.out.println("Total size: "+countOut.getTotalBytes());
			System.out.println("DeflateDictionary Compression: "+StringUtil.getPercent(countOut.getTotalBytes(), plainSize)+" in "+st.stopAndShow());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
