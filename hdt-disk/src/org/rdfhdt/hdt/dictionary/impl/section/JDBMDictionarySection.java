/**
 * File: $HeadURL: http://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/dictionary/impl/DictionarySectionHash.java $
 * Revision: $Rev: 17 $
 * Last modified: $Date: 2012-07-03 21:43:15 +0100 (Tue, 03 Jul 2012) $
 * Last modified by: $Author: mario.arias@gmail.com $
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

package org.rdfhdt.hdt.dictionary.impl.section;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;

import org.apache.jdbm.DB;
import org.rdfhdt.hdt.dictionary.TempDictionarySection;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.util.string.ByteStringUtil;

/**
 * This is a class implementing a modifiable section of the dictionary that uses
 * JDBM key-value database to store dictionary entries for a section (subject, objects etc.)
 * 
 * It uses 2 collections backed on disc:
 *  - a SortedMap (TreeMap) for mapping ID's to string values
 *  - a Map (HashMap) for mapping string values to their ID's
 *  
 * @author Eugen Rozic
 *
 */
public class JDBMDictionarySection implements TempDictionarySection {
	public static final int TYPE_INDEX = 3;

	private DB db;
	@SuppressWarnings("unused")
	private String sectionID;

	private SortedMap<String, Integer> map_StringToID;

	private Integer IDcounter;
	private int numElements;
	private long size;

	private boolean sorted = false;

	public JDBMDictionarySection(DB db, String sectionName) {
		this(new HDTSpecification(), db, sectionName);
	}

	public JDBMDictionarySection(HDTOptions spec, DB db, String sectionID) {

		this.db = db;
		this.sectionID = sectionID;

		this.map_StringToID = db.<String, Integer>createTreeMap(sectionID+"_map_StringToID"); //no need for custom comparator, String are sorted lexicographically by their default Comparator

		this.IDcounter = 0;
		this.numElements = 0;
		this.size = 0;
	}

	@Override
	public int add(CharSequence charSeq) {
		String str = charSeq.toString();
		Integer pos = map_StringToID.get(str);
		if(pos!=null) {
			// found, return existing ID.
			return pos;
		}
		// Not found, insert new
		IDcounter++;
		map_StringToID.put(str, IDcounter);

		numElements++;
		size += str.getBytes(ByteStringUtil.STRING_ENCODING).length;
		sorted = false;

		return IDcounter;
	}

	@Override
	public void remove(CharSequence charSeq) {
		String str = charSeq.toString();
		Integer ID = map_StringToID.remove(str);
		if (ID==null) return; //string not in dictionary

		numElements--;
		size -= str.getBytes(ByteStringUtil.STRING_ENCODING).length;
		sorted = false;
	}

	@Override
	public void sort() {

		IDcounter = 0;	
		for (Map.Entry<String, Integer> e : map_StringToID.entrySet()){
			IDcounter++;
			e.setValue(IDcounter);
		}

		sorted = true;
	}
	
	@Override
	public boolean isSorted() {
		return sorted;
	}

	@Override
	public int locate(CharSequence charSeq) {
		Integer ID = map_StringToID.get(charSeq.toString());
		if(ID==null) return 0;
		return ID.intValue();
	}
	
	@Override
	public CharSequence extract(int pos) {
		throw new NotImplementedException();
	}
	
	@Override
	public Iterator<? extends CharSequence> getSortedEntries() {
		if (!sorted) return null; //best this way to avoid any misunderstandings
		return map_StringToID.keySet().iterator();
	}

	/**
	 * Does the same as getSortedEntries(), so returns current strings in
	 * lexicographical order, only it doesn't guarantee that sort was called
	 * before and that the dictionary is sorted (order by ID's matches the 
	 * lexicographical order).
	 */
	@Override
	public Iterator<? extends CharSequence> getEntries() {
		return map_StringToID.keySet().iterator();
	}

	@Override
	public int getNumberOfElements() {
		return numElements;
	}

	@Override
	public long size() {
		return this.size;
	}

	@Override
	public void clear() {
		map_StringToID.clear();
		db.commit();
		IDcounter = 0;
		numElements = 0;
		size = 0;
		sorted = false;
	}

	/*
	 * A custom iterator for the mappings. It gives strings from the current dictionary
	 * ordered by the current ID's (value with smallest ID first etc.)
	 * 
	 * Removing while iterating is not supported and changing the underlying maps ( i.e. 
	 * calling add, remove, sort or clear methods of the DictionarySectionJDBM) will
	 * cause a ConcurrentModificationException to be thrown.
	 * 
	 * @author Eugen Rozic
	 *
	 *
	private class IteratorByID implements Iterator<CharSequence>{

		private Integer numElements;
		private Integer iterated;
		private Integer currID;

		public IteratorByID() {
			this.numElements = getNumberOfElements();
			this.iterated = 0;
			currID = 1;
			changesFlag = false;
		}

		@Override
		public boolean hasNext() {
			if (changesFlag)
				throw new ConcurrentModificationException();

			return this.iterated<this.numElements;
		}

		@Override
		public CharSequence next() {
			if (changesFlag)
				throw new ConcurrentModificationException();

			CharSequence value = null;
			while (value==null){
				value = map_IDToString.get(currID++);
				if (currID>IDcounter) { //guard for endless looping, just in case
					throw new ConcurrentModificationException();
				}
			}
			iterated++;
			return value;
		}

		@Override
		public void remove() {
			throw new NotImplementedException();
		}

	}
	 */
}
