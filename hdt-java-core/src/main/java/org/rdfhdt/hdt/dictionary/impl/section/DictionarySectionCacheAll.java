/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/dictionary/impl/section/DictionarySectionCacheAll.java $
 * Revision: $Rev: 191 $
 * Last modified: $Date: 2013-03-03 11:41:43 +0000 (dom, 03 mar 2013) $
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

package org.rdfhdt.hdt.dictionary.impl.section;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.rdfhdt.hdt.dictionary.DictionarySectionPrivate;
import org.rdfhdt.hdt.dictionary.TempDictionarySection;
import org.rdfhdt.hdt.listener.ProgressListener;

/**
 * DictionarySection that caches results returned by a child DictionarySection to increase performance.
 * 
 * @author mario.arias
 *
 */
public class DictionarySectionCacheAll implements DictionarySectionPrivate {
	
	final int CACHE_ENTRIES = 128;
	private final DictionarySectionPrivate child;
	private final boolean preload;
	
	Map<CharSequence, Integer> cacheString;
	CharSequence [] cacheID; 
	
	
	public DictionarySectionCacheAll(DictionarySectionPrivate child, boolean preload) {
		this.child = child;
		this.preload = preload;
		
		cacheString = new HashMap<>(child.getNumberOfElements() * 2);
		cacheID = new CharSequence[child.getNumberOfElements()];

		if(preload) {
			Iterator <? extends CharSequence> it = child.getSortedEntries();
			int pos=0;
			while(it.hasNext()) {
				cacheID[pos] = it.next();
				cacheString.put(cacheID[pos], pos);
				pos++;
			}
		}
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#locate(java.lang.CharSequence)
	 */
	@Override
	public int locate(CharSequence s) {
		Integer o = cacheString.get(s);
		if(o==null) {
			o = child.locate(s);
			cacheString.put(s, o);
		}
 		return o;
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#extract(int)
	 */
	@Override
	public final CharSequence extract(int pos) {
		if(preload) {
			return cacheID[pos-1];
		} else {
			if(pos==0) {
				return null;
			}
			CharSequence o = cacheID[pos-1];
			if(o==null) {
				o = child.extract(pos);
				cacheID[pos-1] = o;
				//cacheString.put(o, pos);
			}
			return o;
		}
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#size()
	 */
	@Override
	public long size() {
		return child.size();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#getNumberOfElements()
	 */
	@Override
	public int getNumberOfElements() {
		return child.getNumberOfElements();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#getEntries()
	 */
	@Override
	public Iterator<? extends CharSequence> getSortedEntries() {
		return child.getSortedEntries();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#save(java.io.OutputStream, hdt.listener.ProgressListener)
	 */
	@Override
	public void save(OutputStream output, ProgressListener listener)
			throws IOException {
		child.save(output, listener);
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#load(java.io.InputStream, hdt.listener.ProgressListener)
	 */
	@Override
	public void load(InputStream input, ProgressListener listener)
			throws IOException {
		child.load(input, listener);

	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#load(hdt.dictionary.DictionarySection, hdt.listener.ProgressListener)
	 */
	@Override
	public void load(TempDictionarySection other, ProgressListener listener) {
		child.load(other, listener);
	}

	@Override
	public void close() throws IOException {
		cacheString=null;
		cacheID=null;
		child.close();
	}
}
