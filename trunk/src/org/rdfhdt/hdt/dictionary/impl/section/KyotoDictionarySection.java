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

import java.io.File;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

import kyotocabinet.Cursor;
import kyotocabinet.DB;

import org.rdfhdt.hdt.dictionary.TempDictionarySection;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.string.ByteStringUtil;


/**
 * This is a class implementing a modifiable section of the dictionary that uses
 * KyotoCabinet key-value database to store dictionary entries for a section (subject, objects etc.)
 * 
 * It uses 2 file backed databases:
 *  - a Tree-based (using B+ tree) for mapping ID's to string values
 *  - a Hash-based (using hash-map) for mapping string values to their ID's
 *  
 * @author Eugen Rozic
 *
 */
public class KyotoDictionarySection implements TempDictionarySection {
	public static final int TYPE_INDEX = 5;

	private File homeDir;
	private String sectionID;

	private DB map_StringToID;
	private String dbFileName;

	private Integer IDcounter;
	private int numElements;
	private long size;

	private boolean changed = false;
	private boolean sorted = false;

	public KyotoDictionarySection(File homeDir, String sectionName) throws Exception {
		this(new HDTSpecification(), homeDir, sectionName);
	}

	public KyotoDictionarySection(HDTSpecification spec, File homeDir, String sectionID) {

		this.homeDir = homeDir;
		this.sectionID = sectionID;

		setupDatabases(spec);

		this.IDcounter = 0;
		this.numElements = 0;
		this.size = 0;
	}

	private void setupDatabases(HDTSpecification spec) {
		
		//TODO read from specs...
		//TODO catch exceptions...

		this.map_StringToID = new DB(DB.GEXCEPTIONAL);
		
		this.dbFileName = homeDir.getPath()+"/"+sectionID+"_map_StringToID.kct";
		String logParameters = "#log=-#logkinds=debug#logpx="+sectionID;
		//setup for a lot of RAM, performance optimization
		String dbParameters = "#apow=2#opts=l#bnum=???#msiz=1L<<29#";
			//apow def=8 ; bnum=10%rec ; msiz=RAM(TODO from specs) ; pccap(page cache)~= msiz/30 (pow2)
		
		map_StringToID.open(dbFileName+logParameters+dbParameters, DB.OWRITER | DB.OTRUNCATE);
		
	}

	@Override
	public int add(CharSequence charSeq) {
		byte[] str = charSeq.toString().getBytes(ByteStringUtil.STRING_ENCODING);
		byte[] pos = map_StringToID.get(str);
		if(pos!=null) {
			// found, return existing ID.
			return IOUtil.byteArrayToInt(pos);
		}
		// Not found, insert new
		IDcounter++;
		pos = IOUtil.intToByteArray(IDcounter);
		boolean success = map_StringToID.add(str, pos);
		if (!success)
			throw new RuntimeException("Add failed in "+sectionID+"?!"); 

		numElements++;
		size += str.length;
		changed = true;
		sorted = false;

		return IDcounter;
	}

	@Override
	public void remove(CharSequence charSeq) {
		byte[] str = charSeq.toString().getBytes(ByteStringUtil.STRING_ENCODING);
		byte[] ID = map_StringToID.seize(str);
		if (ID==null) return;

		numElements--;
		size -= str.length;
		changed = true;
		sorted = false;
	}

	@Override
	public void sort() {

		IDcounter = 0;
		boolean more = true;
		Cursor cursor = map_StringToID.cursor();
		cursor.jump();
		while (more){
			IDcounter++;
			more = cursor.set_value(IOUtil.intToByteArray(IDcounter), true); //set value and move on
		}
		cursor.disable();

		changed = true;
		sorted = true;
	}
	
	@Override
	public boolean isSorted() {
		return sorted;
	}

	@Override
	public int locate(CharSequence charSeq) {
		byte[] ID = map_StringToID.get(charSeq.toString().getBytes(ByteStringUtil.STRING_ENCODING));
		if(ID==null) return 0;
		return IOUtil.byteArrayToInt(ID);
	}

	@Override
	public Iterator<? extends CharSequence> getSortedEntries() {
		if (!sorted) return null; //best this way to avoid any misunderstandings
		return new KeyIterator(map_StringToID.cursor());
	}

	/**
	 * Does the same as getSortedEntries(), so returns current strings in
	 * lexicographical order, only it doesn't guarantee that sort was called
	 * before and that the dictionary is sorted (order by ID's matches the 
	 * lexicographical order).
	 */
	@Override
	public Iterator<? extends CharSequence> getEntries() {
		return new KeyIterator(map_StringToID.cursor());
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
		if (!map_StringToID.clear())
			throw new RuntimeException("Clear of "+sectionID+" failed?!");
		IDcounter = 0;
		numElements = 0;
		size = 0;
		changed = true;
		sorted = false;
	}

	/**
	 * Closes the created databases and cleans up the database files.
	 */
	public void cleanup() throws Exception {
		
		changed = true;

		map_StringToID.close();
		new File(dbFileName).delete();

	}

	//----------------------------------------------------------------------
	//---classes------------------------------------------------------------
	//----------------------------------------------------------------------

	/**
	 * An Iterator wrapper for the kyotocabinet.Cursor object
	 * 
	 * Removing while iterating is not supported and changing the underlying maps ( i.e. 
	 * calling add, remove, sort or clear (+ load and cleanup) methods of the DictionarySectionKyoto) will
	 * cause a ConcurrentModificationException to be thrown.
	 * 
	 * @author Eugen Rozic
	 *
	 */
	private class KeyIterator implements Iterator<CharSequence>{

		private Cursor cursor;
		
		private byte[] next = null;
		private boolean loaded = false;

		public KeyIterator(Cursor cursor) {
			this.cursor = cursor;
			changed = false;
			
			cursor.jump();
		}

		@Override
		public boolean hasNext() {
			if (changed)
				throw new ConcurrentModificationException();

			if (!loaded){
				load();
			}
			
			if (next==null){
				cursor.disable();
				return false;
			} else {
				return true;
			}
		}
		
		private void load(){
			next = cursor.get_key(true); //gets key, and advances
			loaded = true;
		}

		@Override
		public CharSequence next() {
			if (changed)
				throw new ConcurrentModificationException();

			if (hasNext()) { //loads next here
				loaded = false; //unload if next!=null and return it
				return new String(next, ByteStringUtil.STRING_ENCODING);
			} else {
				return null; //otherwise just stay loaded (with null) and return it
			}
		}

		@Override
		public void remove() {
			throw new NotImplementedException();
		}

	}

	@Override
	public CharSequence extract(int pos) {
		throw new NotImplementedException();
	}
}
