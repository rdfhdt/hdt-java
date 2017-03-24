/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-jena/src/org/rdfhdt/hdtjena/cache/DictionaryCacheArray.java $
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

/**
 * @author mario.arias
 *
 */
public class DictionaryCacheArray implements DictionaryCache {

	private Node[] array;
	final int capacity;
	int numentries;
	
	public DictionaryCacheArray(int capacity) {
		array = null;
		numentries=0;
		this.capacity=capacity;
	}
	
	/* (non-Javadoc)
	 * @see hdt.jena.DictionaryNodeCache#getNode(int)
	 */
	@Override
	public Node get(int id) {
		if(array==null) {
			return null;
		}
		if(id>array.length) {
			throw new IndexOutOfBoundsException();
		}
		return array[id-1];
	}
	
	@Override
    public void put(int id, Node node) {
		if(array==null) {
			array = new Node[capacity];
		}
		if(array[id-1]==null) {
			numentries++;
		}
		array[id-1] = node;
	}

	@Override
	public int size() {
		return numentries;
	}

	@Override
	public void clear() {
		array=null;
	}

}
