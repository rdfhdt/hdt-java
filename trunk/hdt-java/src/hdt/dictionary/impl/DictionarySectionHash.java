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

package hdt.dictionary.impl;

import hdt.dictionary.DictionarySection;
import hdt.exceptions.NotImplementedException;
import hdt.listener.ProgressListener;
import hdt.options.HDTSpecification;
import hdt.util.string.CharSequenceComparator;
import hdt.util.string.CompactString;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * @author mario.arias
 *
 */
public class DictionarySectionHash implements DictionarySection {
	public static final int TYPE_INDEX = 1;

	private HashMap<CharSequence, Integer> map;
	private List<CharSequence> list;
	private int size;
	
	/**
	 * 
	 */
	public DictionarySectionHash() {
		this(new HDTSpecification());
	}
	
	public DictionarySectionHash(HDTSpecification spec) {
		map = new HashMap<CharSequence, Integer>();
		list = new ArrayList<CharSequence>();
		size=0;
	}
	
	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#locate(java.lang.CharSequence)
	 */
	@Override
	public int locate(CharSequence s) {
		Integer val = map.get(s);
		if(val==null) {
			return 0;
		}
		return val.intValue();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#extract(int)
	 */
	@Override
	public CharSequence extract(int pos) {
		if(pos<=0) {
			return null;
		}
		return list.get(pos-1);
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
		return list.size();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#getEntries()
	 */
	@Override
	public Iterator<CharSequence> getEntries() {
		return list.iterator();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#save(java.io.OutputStream, hdt.ProgressListener)
	 */
	@Override
	public void save(OutputStream stream, ProgressListener listener) throws IOException {
		throw new NotImplementedException();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#load(java.io.InputStream, hdt.ProgressListener)
	 */
	@Override
	public void load(InputStream stream, ProgressListener listener) throws IOException {
		throw new NotImplementedException();
	}

	public int add(CharSequence entry) {
		Integer pos = map.get(entry);
		if(pos!=null) {
			// Found return existing ID.
			return pos;
		}
		// Not found, insert new
		CharSequence compact = new CompactString(entry);
		//CharSequence compact = entry;
		list.add(compact);
		map.put(compact, list.size());
		size+=compact.length();
		return list.size();
	}

	public void remove(CharSequence seq) {
		map.remove(seq);
	}
	
	public void sort() {
		// Update list.
		list.clear();
		for(CharSequence str : map.keySet()) {
			list.add(str);
		}
		
		// Sort list
		Collections.sort(list, new CharSequenceComparator());
		
		// Update map indexes
		for(int i=1;i<=getNumberOfElements();i++) {
			map.put(extract(i), i);
		}
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#load(hdt.dictionary.DictionarySection)
	 */
	@Override
	public void load(DictionarySection other, ProgressListener listener) {
		Iterator<CharSequence> it = other.getEntries();
		while(it.hasNext()) {
			CharSequence str = it.next();
			this.add(str);
		}
	}
}
