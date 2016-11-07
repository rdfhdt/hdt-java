/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/dictionary/impl/DictionaryIDMapping.java $
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

package org.rdfhdt.hdt.dictionary.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mario.arias
 *
 */
public class DictionaryIDMapping {
	class Entry {
		int newid;
		final CharSequence str;
		
		Entry(CharSequence str) {
			this.str = str;
			this.newid = 0;
		}
	}
	
	final List<Entry> list;
	
	public DictionaryIDMapping(int numentries) {
		list = new ArrayList<>(numentries);
	}
	
	public void add(CharSequence str) {
		list.add(new Entry(str));
	}
	
	public void setNewID(int oldId, int newID) {
		list.get(oldId).newid = newID;
	}
	
	public int getNewID(int oldId) {
		//System.out.println("GetNewID old: "+oldId+"/"+list.size());
		return list.get(oldId).newid;
	}
	
	public CharSequence getString(int oldId) {
		return list.get(oldId).str;
	}
	
	public int size() {
		return list.size();
	}
}
