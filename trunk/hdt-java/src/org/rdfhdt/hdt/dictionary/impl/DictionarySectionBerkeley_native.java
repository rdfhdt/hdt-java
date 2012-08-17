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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

import org.rdfhdt.hdt.dictionary.DictionarySection;
import org.rdfhdt.hdt.dictionary.DictionarySectionModifiable;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.util.string.ByteStringUtil;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.collections.StoredMap;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.db.Database;
import com.sleepycat.db.DatabaseConfig;
import com.sleepycat.db.DatabaseException;
import com.sleepycat.db.DatabaseType;
import com.sleepycat.db.Environment;

/**
 * This is a class implementing a modifiable section of the dictionary that uses
 * BerkeleyDB (native version) key-value database to store dictionary entries for a section (subject, objects etc.)
 * 
 * It uses 2 collections backed on disc:
 *  - a SortedMap (StoredSortedMap...using BTree probably) for mapping ID's to string values
 *  - a Map (StoredMap...using HTree probably) for mapping string values to their ID's
 * 
 *  This class is almost identical to DictionarySectionBerkeley, the only
 *  difference is in the "setupDatabases" and the "cleanup" methods, and of course
 *  in the imports (which is why I can just extend that class). 
 *  
 * @author Eugen Rozic
 */
public class DictionarySectionBerkeley_native implements DictionarySectionModifiable {
	public static final int TYPE_INDEX = 4;

	private Environment env;
	private String sectionID;
	
	private Database db_StringToID;
	private Database db_IDToString;

	private StoredSortedMap<String, Integer> map_StringToID;
	private StoredMap<Integer, String> map_IDToString;

	private Integer IDcounter;
	private int numElements;
	private long size;

	private boolean sorted = false;

	public DictionarySectionBerkeley_native(Environment env, String sectionName) throws DatabaseException, FileNotFoundException {
		this(new HDTSpecification(), env, sectionName);
	}
	
	public DictionarySectionBerkeley_native(HDTSpecification spec, Environment env, String sectionID) throws DatabaseException, FileNotFoundException {
		
		this.env = env;
		this.sectionID = sectionID;
		
		setupDatabases(spec);

		this.map_StringToID = new StoredSortedMap<String, Integer>(db_StringToID, 
				TupleBinding.getPrimitiveBinding(String.class), TupleBinding.getPrimitiveBinding(Integer.class), true);
		this.map_IDToString = new StoredMap<Integer, String>(db_IDToString,
				TupleBinding.getPrimitiveBinding(Integer.class), TupleBinding.getPrimitiveBinding(String.class), true);
		

		this.IDcounter = 0;
		this.numElements = 0;
		this.size = 0;
	}
	
	private void setupDatabases(HDTSpecification spec) throws DatabaseException, FileNotFoundException {
		
		DatabaseConfig dbConf = new DatabaseConfig();
		dbConf.setExclusiveCreate(true); //so that it throws exception if already exists
		dbConf.setAllowCreate(true);
		dbConf.setTransactional(false);
		//dbConf.setBtreeComparatorVoid(new StringUnicodeComparator()); //TODO byte by byte default ok??
		dbConf.setType(DatabaseType.BTREE);
		
		this.db_StringToID = env.openDatabase(null, sectionID, "map_StringToID", dbConf);
		//SecondaryDatabase worse solution because it keeps the key and primaryKey anyway (int and string), and THEN fetches the data (which is the same as secondaryKey) from the primary database... and less control over the structure and so on...
		this.db_IDToString = env.openDatabase(null, sectionID, "map_IDToString", dbConf);
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
		//map_IDToString.put(IDcounter, str); FIXME optimization, no need for this info before sorting (because only two-pass way of working supported) 

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
		//map_IDToString.remove(ID); FIXME optimization, no real need for this (only for consistency)

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
			map_IDToString.put(IDcounter, e.getKey());
		}

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
		return map_IDToString.get(ID); //if not exsisting will return null which is fine
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
		map_IDToString.clear();
		
		sorted = false;
	}
	
	/**
	 * Closes the created databases, nulls the references and deletes the database file.
	 */
	public void cleanup() throws DatabaseException {
		
		db_IDToString.close();
		db_IDToString = null;
		
		db_StringToID.close();
		db_StringToID = null;
		
		new File("./DB/"+sectionID).delete();
	}
	
	//----------------------------------------------------------------------
	//---classes------------------------------------------------------------
	//----------------------------------------------------------------------
	
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
	
	/*
	private class IDfromStringKeyCreator implements SecondaryKeyCreator {
		
		@Override
		public boolean createSecondaryKey(SecondaryDatabase secDB,
				DatabaseEntry key, DatabaseEntry data, DatabaseEntry secKey) {
			
			secKey.setData(data.getData()); //ID that was data is now key
			return true;
		}
	}
	*/

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
