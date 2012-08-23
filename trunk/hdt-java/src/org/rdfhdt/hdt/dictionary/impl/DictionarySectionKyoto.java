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
import java.util.ConcurrentModificationException;
import java.util.Iterator;

import kyotocabinet.Cursor;
import kyotocabinet.DB;

import org.rdfhdt.hdt.dictionary.DictionarySection;
import org.rdfhdt.hdt.dictionary.DictionarySectionModifiable;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTSpecification;
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
public class DictionarySectionKyoto implements DictionarySectionModifiable {
	public static final int TYPE_INDEX = 5;

	private File homeDir;
	private String sectionID;

	private DB map_StringToID;
	private DB map_IDToString;

	private Integer IDcounter;
	private int numElements;
	private long size;

	private boolean changed = false;
	private boolean sorted = false;

	public DictionarySectionKyoto(File homeDir, String sectionName) throws Exception {
		this(new HDTSpecification(), homeDir, sectionName);
	}

	public DictionarySectionKyoto(HDTSpecification spec, File homeDir, String sectionID) {

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
		//TODO setup and open the database in homeDir;
		map_StringToID.open(homeDir.getPath()+"/"+sectionID+"_map_StringToID.kct", DB.OWRITER | DB.OTRUNCATE);

		this.map_IDToString = new DB(DB.GEXCEPTIONAL);
		//TODO setup and open the database in homeDir;
		map_IDToString.open(homeDir.getPath()+"/"+sectionID+"_map_IDToString.kch", DB.OWRITER | DB.OTRUNCATE);
	}

	private byte[] intToByteArray(int value) {
		
		byte[] b = new byte[4];
		for (int i=0; i<4; i++) {
			int offset = (3 - i) * 8;
			b[i] = (byte) ((value >>> offset) & 0xFF);
		}
		return b;
	}
	
	private int byteArrayToInt(byte[] value){
		
		int res = 0;
		for (int i=0; i<4; i++){
			int offset = (3 - i) * 8;
			res |= ((value[i] & 0xFF) << offset);
		}
		return res;
	}

	@Override
	public int add(CharSequence charSeq) {
		byte[] str = charSeq.toString().getBytes(ByteStringUtil.STRING_ENCODING);
		byte[] pos = map_StringToID.get(str);
		if(pos!=null) {
			// found, return existing ID.
			return byteArrayToInt(pos);
		}
		// Not found, insert new
		IDcounter++;
		pos = intToByteArray(IDcounter);
		boolean success = map_StringToID.add(str, pos);
		if (!success)
			throw new RuntimeException("Add failed in "+sectionID+"?!");
		//map_IDToString.put(pos, str); FIXME optimization, no need for this info before sorting (because only two-pass way of working supported) 

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
		//map_IDToString.remove(ID); FIXME optimization, no real need for this (only for consistency)

		numElements--;
		size -= str.length;
		changed = true;
		sorted = false;
	}

	@Override
	public void sort() {

		IDcounter = 0;
		byte[] str = null;
		byte[] currID = null;
		Cursor cursor = map_StringToID.cursor();
		cursor.jump();
		while ((str = cursor.get_key(false))!=null){ //get key but don't move on
			IDcounter++;
			currID = intToByteArray(IDcounter);
			cursor.set_value(currID, true); //set value and move on
			map_IDToString.set(currID, str);
		}
		cursor.disable();

		changed = true;
		sorted = true;
	}

	@Override
	public int locate(CharSequence charSeq) {
		byte[] ID = map_StringToID.get(charSeq.toString().getBytes(ByteStringUtil.STRING_ENCODING));
		if(ID==null) return 0;
		return byteArrayToInt(ID);
	}

	@Override
	public CharSequence extract(int ID) {
		if (ID>IDcounter) return null; //after remove and sort... not necessary to remove extra values from disc map_IDToString, just made inaccesible this way (so to outside like they don't exist)
		byte[] str = map_IDToString.get(intToByteArray(ID)); //if not exsisting will return null
		return str==null? null : new String(str, ByteStringUtil.STRING_ENCODING);
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

		changed = true;
		sorted = false;
	}

	@Override
	public long size() {
		return this.size;
	}

	@Override
	public void clear() {
		if (!(map_StringToID.clear() && map_IDToString.clear()))
			throw new RuntimeException("Clear of "+sectionID+" failed?!");
		
		changed = true;
		sorted = false;
	}

	/**
	 * Closes the created databases and cleans up the database files.
	 */
	public void cleanup() throws Exception {
		
		changed = true;

		map_StringToID.close();
		//TODO ... is this the filename??
		new File(homeDir.getPath()+"/"+sectionID+"_map_StringToID.kct").delete();

		map_IDToString.close();
		//TODO ... is this the filename??
		new File(homeDir.getPath()+"/"+sectionID+"_map_IDToString.kch").delete();

		//TODO are there any other files made?
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
			
			return next==null? false : true; //if exsists then not null ergo true, else null ergo false
		}
		
		private void load(){
			next = cursor.get_key(true); //gets key, does not advance
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
}
