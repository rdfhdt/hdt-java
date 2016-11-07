/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-jena/src/org/rdfhdt/hdtjena/cache/DictionaryCacheLRU.java $
 * Revision: $Rev: 190 $
 * Last modified: $Date: 2013-03-03 11:30:03 +0000 (dom, 03 mar 2013) $
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
 */

package org.rdfhdt.hdtjena.cache;

import org.apache.jena.graph.Node;

import org.rdfhdt.hdt.util.LRUCache;

/**
 * @author mario.arias
 *
 */
public class DictionaryCacheLRU implements DictionaryCache {

	private final LRUCache<Integer, Node> lru;
	
	public DictionaryCacheLRU(int size) {
		lru = new LRUCache<>(size);
	}
	
	/* (non-Javadoc)
	 * @see hdt.jena.DictionaryNodeCache#getNode(int)
	 */
	@Override
	public Node get(int id) {
		return lru.get(id);
	}

	/* (non-Javadoc)
	 * @see hdt.jena.DictionaryNodeCache#setNode(int, com.hp.hpl.jena.graph.Node)
	 */
	@Override
	public void put(int id, Node node) {
		lru.put(id, node);
	}

	@Override
	public int size() {
		return lru.size();
	}

	@Override
	public void clear() {
		lru.clear();
	}

}
