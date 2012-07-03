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

import org.rdfhdt.hdt.dictionary.DictionarySection;
import org.rdfhdt.hdt.dictionary.DictionarySectionModifiable;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.util.string.CompactString;
import org.rdfhdt.hdt.util.string.ReplazableString;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author mck
 *
 */
public class DictionarySectionLiterals implements DictionarySection {
	public static final byte TYPE_INDEX = 3;
	
	private Map<CharSequence, Integer> sectionMap;
	private List<DictionarySection> sectionList;
	private List<CharSequence> sectionLabel;
	int [] accumLength;
	int numElements;
	
	/**
	 * 
	 */
	public DictionarySectionLiterals() {
		sectionMap = new HashMap<CharSequence, Integer>();
		sectionList = new ArrayList<DictionarySection>();
		sectionLabel = new ArrayList<CharSequence>();
	}
	
	private void updateSectionCount() {
		int count = 0;
		numElements = 0;
		accumLength = new int[sectionList.size()+1];
		for(DictionarySection section : sectionList) {
			count++;
			numElements += section.getNumberOfElements();
			accumLength[count] = numElements;
		}
	}

	private int getSectionID(int globalid) {
		int block = 0;
		while(block<accumLength.length && accumLength[block]<globalid) {
			block++;
		}
		return block-1;
		
		// FIXME: Do it using binary search!
//		int pos = Arrays.binarySearch(accumLength, globalid);
//		if(pos<0) {
//			// Not found exactly, gives the position where it should be inserted
//			pos = -pos-2;
//		} else if(pos>0){
//			// If found exact, we need to check previous block.
//		//	pos--;
//		}
//		return pos;
	}
	
	private int getGlobalID(int sectionNum, int localid) {
		return accumLength[sectionNum]+localid;
	}
	
	private int getLocalID(int section, int globalid) {
		return globalid-accumLength[section];
	}
	
	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#locate(java.lang.CharSequence)
	 */
	@Override
	public int locate(CharSequence str) {
		CharSequence dataType = getDatatype(str);
		CharSequence compact = getCompact(str, dataType);
		
		Integer sectionNum = sectionMap.get(dataType);
		if(sectionNum==null) {
			return 0;
		}
		
		DictionarySection section = sectionList.get(sectionNum);
		int localid = section.locate(compact);
		if(localid==0) {
			return 0;
		}
		return getGlobalID(sectionNum, localid);
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#extract(int)
	 */
	@Override
	public CharSequence extract(int pos) {
		int sectionNum = getSectionID(pos);
		if(sectionNum==-1) {
			return "";
		}
		if(sectionNum>=sectionList.size()) {
			System.out.println("HERE");
			return "**********************************************************";
		}
		ReplazableString str = new ReplazableString();
		DictionarySection section = sectionList.get(sectionNum); 
		int localID = getLocalID(sectionNum, pos);
		CharSequence literal = section.extract(localID);
		str.append(literal);
		
		CharSequence dataType = sectionLabel.get(sectionNum);
		str.append(dataType);
		return str;
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#size()
	 */
	@Override
	public long size() {
		long s = 0;
		for(DictionarySection section : sectionList) {
			s+=section.size();
		}
		return s;
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#getNumberOfElements()
	 */
	@Override
	public int getNumberOfElements() {
		return numElements;
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#getEntries()
	 */
	@Override
	public Iterator<CharSequence> getEntries() {
		return null;
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#save(java.io.OutputStream, hdt.listener.ProgressListener)
	 */
	@Override
	public void save(OutputStream output, ProgressListener listener) throws IOException {
		
		DataOutputStream dout = new DataOutputStream(output);
		dout.writeByte(TYPE_INDEX);
		dout.writeInt(sectionList.size());
		for(int i=0;i<sectionList.size();i++) {
			CharSequence dataType = sectionLabel.get(i);
			dout.writeUTF(dataType.toString());
			sectionList.get(i).save(dout, listener);
		}
		
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#load(java.io.InputStream, hdt.listener.ProgressListener)
	 */
	@Override
	public void load(InputStream input, ProgressListener listener) throws IOException {
		DataInputStream din = new DataInputStream(input);
		int numSections = din.readInt();
		for(int i=0;i<numSections;i++) {
			CharSequence dataType = new CompactString(din.readUTF());
			sectionLabel.add(dataType);
			
			DictionarySection section = DictionarySectionFactory.createInstance(din, null);
			section.load(din, listener);
			sectionList.add(section);
			
			sectionMap.put(dataType, i);
		}
		
		updateSectionCount();
		
//		dump();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#load(hdt.dictionary.DictionarySection, hdt.listener.ProgressListener)
	 */
	@Override
	public void load(DictionarySection other, ProgressListener listener) {
		
		if(other instanceof DictionarySectionLiterals) {
			DictionarySectionLiterals ol = ((DictionarySectionLiterals) other); 
			this.sectionList = ol.sectionList;
			this.accumLength = ol.accumLength;
			this.numElements = ol.numElements;
			this.sectionLabel = ol.sectionLabel;
			this.sectionMap = ol.sectionMap;
			return;
		}
		
		Iterator<CharSequence> it = other.getEntries();
		numElements=0;
		while(it.hasNext()) {
			CharSequence entry = it.next();
			
			// Extract dataType and Compact Representation
			CharSequence dataType = getDatatype(entry);
			CharSequence compact = getCompact(entry, dataType);
	
			// Find section.
			Integer sectionNum = sectionMap.get(dataType);
			DictionarySection section;
			if(sectionNum==null) {
				// Not extisting, create new section
				section = new DictionarySectionHash();
				sectionList.add(section);
				sectionLabel.add(dataType);
				sectionNum = sectionList.size()-1;
				sectionMap.put(dataType, sectionNum);
			} else {
				section = sectionList.get(sectionNum);
			}
			((DictionarySectionModifiable)section).add(compact);
			numElements++;
		}
		
		// Sort each section
		for(DictionarySection section : sectionList) {
			((DictionarySectionModifiable)section).sort();
		}
		
		// Convert each section to final format.
		for(int i=0;i<sectionList.size();i++) {
			DictionarySection newSection = new DictionarySectionPFC(new HDTSpecification());
			newSection.load(sectionList.get(i), null);
			sectionList.set(i, newSection);
		}
		
		// Count number of elements for each section
		updateSectionCount();
		
		dump();
	}
	
	private void dump() {
		System.out.println("Num Sections: "+sectionList.size());
		System.out.println("Num entries: "+numElements);
		System.out.println("Array: ");
		for(int i=0;i<accumLength.length;i++) {
			System.out.print(accumLength[i]+", ");
		}
		System.out.println();
		
		// DEBUG
//		for(int i=0;i<sectionList.size();i++) {
//			System.out.println("\n*************************************************");
//			System.out.println("Section: "+i+" => "+sectionLabel.get(i));
//			DictionarySection dict = sectionList.get(i);
//			for(int j=1;j<=dict.getNumberOfElements();j++) {
//				System.out.println(getGlobalID(i, j)+"/"+j+" => "+dict.extract(j));
//			}
//		}
//		System.out.println("\n\n");
		
		for(int i=1;i<=getNumberOfElements();i++) {
			CharSequence str = this.extract(i);
			int id = this.locate(str);
			if(id!=i) {
				int newid = this.locate(str);
				System.out.println("i:"+i+" => "+str+ " Locate: "+ id + " Again: "+newid);
			}
			
		}
	}
	
	private static final CharSequence getCompact(CharSequence str, CharSequence dataType) {
		if(dataType.length()==0) {
			if(str instanceof CompactString) {
				return str;
			}
			return new CompactString(str);
		}
		char first = dataType.charAt(0);
		if(first=='@') {
			return new CompactString(str, 0, str.length()-dataType.length()-1);
		}
		if(first=='^') {
			return new CompactString(str, 0, str.length()-dataType.length()-1);
		}
		return new CompactString(str);
	}
	
	private static final CharSequence getDatatype(CharSequence entry) {
		CharSequence dataType=null;
		if(entry.charAt(0)=='"') {
			for(int i=entry.length()-1;i>0; i--) {
				char curr = entry.charAt(i);
				
				if(curr=='"') {
					// The datatype or lang must be after the last " symbol.
					break;
				}
				
				char prev = entry.charAt(i-1);
				if(curr=='@' && prev=='"') {
					//System.out.println(entry);
					dataType = entry.subSequence(i, entry.length()-1);
					//System.out.println("\tLANG: "+dataType);
					break;
				}
				if(curr=='^' && prev=='^') {
					//System.out.println(entry);
					dataType = entry.subSequence(i-1, entry.length()-1);
					//System.out.println("\tDATATYPE: "+ dataType);
					break;
				}
			}
		}
		return dataType!=null ? dataType : CompactString.EMPTY;
	}

}
