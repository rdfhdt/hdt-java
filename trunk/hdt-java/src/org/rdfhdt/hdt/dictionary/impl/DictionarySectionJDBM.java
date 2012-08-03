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

package org.rdfhdt.hdt.dictionary.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;

import org.apache.jdbm.DB;
import org.rdfhdt.hdt.dictionary.DictionarySection;
import org.rdfhdt.hdt.dictionary.DictionarySectionModifiable;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTSpecification;

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
public class DictionarySectionJDBM implements DictionarySectionModifiable {
	public static final int TYPE_INDEX = 3;

	private DB db;
	@SuppressWarnings("unused")
	private String sectionID;

	private Map<Integer, String> map_IDToString;
	private SortedMap<String, Integer> map_StringToID;

	private Integer IDcounter;
	private int numElements;

	//private boolean changesFlag = false;
	private boolean sorted = false;

	public DictionarySectionJDBM(DB db, String sectionName) {
		this(new HDTSpecification(), db, sectionName);
	}

	public DictionarySectionJDBM(HDTSpecification spec, DB db, String sectionID) {

		this.db = db;
		this.sectionID = sectionID;

		//Strings have to be used because CHarSequance does not implement Comparable and thus cannot be a key to a TreeMap (I have no choice)
		this.map_IDToString = db.<Integer, String>createHashMap(sectionID+"_map_IDToString");
		this.map_StringToID = db.<String, Integer>createTreeMap(sectionID+"_map_StringToID"); //no need for custom comparator, String are sorted lexicographically by their default Comparator

		this.IDcounter = 0;
		this.numElements = 0;
	}

	@Override
	public int add(CharSequence charSeq) {
		String str = charSeq.toString();
		Integer pos = map_StringToID.get(str);
		if(pos!=null) {
			// Found return existing ID.
			return pos;
		}
		// Not found, insert new
		IDcounter++;
		map_IDToString.put(IDcounter, str);
		map_StringToID.put(str, IDcounter);

		numElements++;
		
		//changesFlag = true;
		sorted = false;

		return IDcounter;
	}

	@Override
	public void remove(CharSequence charSeq) {
		Integer ID = map_StringToID.remove(charSeq.toString());
		if (ID==null) return; //string not in dictionary
		map_IDToString.remove(ID); //TODO quasi-unnecessary... only for consistency

		numElements--;
		
		//changesFlag = true;
		sorted = false;
	}

	@Override
	public void sort() {

		// first way - persisted linked list instead of map_IDToString (offered by JDBM)
		// bad sides:
		//		1) when sorted is it all in memory or is it done somehow with elements partly(mostly) on disc??
		//		2) this way of sorting very very expencive and slow??

		IDcounter = 0;
		/* FIXME something stupid going on... */
		PrintStream fout = null;
		try {
			File file = new File("DB/sort_jdbm_"+getNumberOfElements()+".txt");
			file.createNewFile();
			fout = new PrintStream(file, "UTF-8");
		/**/
		for (String key : map_StringToID.keySet()){
			IDcounter++;
			fout.println(key+" , ID= "+IDcounter);
			
			map_StringToID.put(key, IDcounter);
			map_IDToString.put(IDcounter, key); 
		}
		/* FIXME */
		} catch (Exception e){
			fout.println("IDcounter= "+IDcounter+" ; numElem= "+numElements);
			throw new RuntimeException(e);
		} finally {
			fout.close();
		}
		/**/

		//changesFlag = true;
		sorted = true;
	}

	@Override
	public int locate(CharSequence charSeq) {
		Integer ID = map_StringToID.get(charSeq.toString());
		if(ID==null) return 0;
		return ID.intValue();
	}

	@Override
	public CharSequence extract(int ID) {
		if (ID>IDcounter) return null; //after remove and sort... not necessary to remove extra values from disc map_IDToString, just made inaccesible this way (so to outside like they don't exist)
		CharSequence string = map_IDToString.get(ID);
		return string; //if not exsisting will return null which is fine
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
	public void save(OutputStream output, ProgressListener listener)
			throws IOException {
		// TODO
		throw new NotImplementedException();
	}

	@Override
	public void load(InputStream input, ProgressListener listener)
			throws IOException {
		// TODO
		throw new NotImplementedException();
	}

	@Override
	public void load(DictionarySection other, ProgressListener listener) {
		Iterator<? extends CharSequence> it = other.getSortedEntries();
		while(it.hasNext()) {
			this.add(it.next());
		}
		db.commit();
		//changesFlag = true;
		sorted = false;
	}

	/**
	 * Should return the size in bytes of the structure.
	 * 
	 * It is not implemented in this implementation of DictionarySection because it cannot be
	 * measured. The correct value would be the sum of the sizes of the DB files on disc that the
	 * object uses to persist the maps in, but that information is not obtainable... and it is
	 * irrelevant for a DicitonarySectionModifiable anyway.
	 */
	@Override
	public long size() {
		throw new NotImplementedException();
	}

	@Override
	public void clear() {
		map_IDToString.clear();
		map_StringToID.clear();
		db.commit();
		//changesFlag = true;
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
