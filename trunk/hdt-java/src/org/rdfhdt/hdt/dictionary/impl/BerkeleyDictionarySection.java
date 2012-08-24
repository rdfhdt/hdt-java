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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

import org.rdfhdt.hdt.dictionary.DictionarySection;
import org.rdfhdt.hdt.dictionary.ModifiableDictionarySection;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.util.string.ByteStringUtil;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Environment;


/**
 * This is a class implementing a modifiable section of the dictionary that uses
 * BerkeleyDB (JE version) key-value database to store dictionary entries for a section (subject, objects etc.)
 * 
 * It uses 2 collections backed on disc:
 *  - a SortedMap (StoredSortedMap...using BTree probably) for mapping ID's to string values
 *  - a Map (StoredMap...using HTree probably) for mapping string values to their ID's
 *  
 * @author Eugen Rozic
 *
 */
public class BerkeleyDictionarySection implements ModifiableDictionarySection {
	public static final int TYPE_INDEX = 4;

	private Environment env;
	private String sectionID;
	
	private Database db_StringToID;

	private StoredSortedMap<String, Integer> map_StringToID;

	private Integer IDcounter;
	private int numElements;
	private long size;

	private boolean sorted = false;

	public BerkeleyDictionarySection(Environment env, String sectionName) throws Exception {
		this(new HDTSpecification(), env, sectionName);
	}
	
	public BerkeleyDictionarySection(HDTSpecification spec, Environment env, String sectionID) throws Exception {
		
		this.env = env;
		this.sectionID = sectionID;
		
		setupDatabases(spec);

		this.map_StringToID = new StoredSortedMap<String, Integer>(db_StringToID, 
				TupleBinding.getPrimitiveBinding(String.class), TupleBinding.getPrimitiveBinding(Integer.class), true);

		this.IDcounter = 0;
		this.numElements = 0;
		this.size = 0;
	}
	
	private void setupDatabases(HDTSpecification spec) throws Exception {
		
		DatabaseConfig dbConf = new DatabaseConfig();
		dbConf.setExclusiveCreateVoid(true); //so that it throws exception if already exists
		dbConf.setAllowCreateVoid(true);
		dbConf.setTransactionalVoid(false);
		dbConf.setTemporaryVoid(true);
		//dbConf.setBtreeComparatorVoid(new StringUnicodeComparator()); //TODO byte by byte default ok??
		
		this.db_StringToID = env.openDatabase(null, sectionID+"_map_StringToID", dbConf);
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
		if (ID==null) return;

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
		//TODO DiscOrderedCursor...?
		return map_StringToID.keySet().iterator();
	}

	@Override
	public int getNumberOfElements() {
		return numElements;
	}

	@Override
	public void save(OutputStream output, ProgressListener listener)
			throws IOException {
		throw new NotImplementedException();
	}

	@Override
	public void load(InputStream input, ProgressListener listener)
			throws IOException {
		throw new NotImplementedException();
	}

	@Override
	public void load(DictionarySection other, ProgressListener listener) {
		Iterator<? extends CharSequence> it = other.getSortedEntries();
		while(it.hasNext()) {
			this.add(it.next());
		}
		
		sorted = false;
	}

	@Override
	public long size() {
		return this.size;
	}

	@Override
	public void clear() {
		map_StringToID.clear();
		
		sorted = false;
	}
	
	/**
	 * Closes the created databases, nulls the references (and deletes the databases,
	 * currently implicit because they are temporary).
	 */
	public void cleanup() throws Exception {
		
		db_StringToID.close();
		db_StringToID = null;
		//env.removeDatabase(null, sectionID+"_map_StringToID"); //to delete files // no need because temporary
	}
	
	
	/*
	private class StringUnicodeComparator implements Comparator<byte[]> {
		
		@Override
		public int compare(byte[] str1, byte[] str2) {

	        String s1 = new String(str1, ByteStringUtil.STRING_ENCODING);
	        String s2 = new String(str2, ByteStringUtil.STRING_ENCODING);
	        return s1.compareTo(s2);
	    }
	}
	*/
}