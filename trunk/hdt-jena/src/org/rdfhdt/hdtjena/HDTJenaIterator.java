/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/compact/array/ArrayFactory.java $
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

package org.rdfhdt.hdtjena;

import org.rdfhdt.hdt.iterator.IteratorTripleID;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Filter;
import com.hp.hpl.jena.util.iterator.Map1;

/**
 * @author mck
 *
 */
public class HDTJenaIterator implements ExtendedIterator<Triple> {

	long numTriples=0;
	long numGenerated=0;
	HDTGraph graph;
	IteratorTripleID iterator;

	HDTJenaIterator(HDTGraph graph, IteratorTripleID iterator) {
		this.graph = graph;
		this.iterator = iterator;
	}
	
	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.util.iterator.ClosableIterator#close()
	 */
	@Override
	public void close() {
		if(numTriples>0) System.out.println("Triples served: "+numTriples);
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#hasNext()
	 */
	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#next()
	 */
	@Override
	public Triple next() {
		numTriples++;
		return graph.getJenaTriple(iterator.next());
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#remove()
	 */
	@Override
	public void remove() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.util.iterator.ExtendedIterator#andThen(java.util.Iterator)
	 */
	@Override
	public <X extends Triple> ExtendedIterator<Triple> andThen(Iterator<X> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.util.iterator.ExtendedIterator#filterDrop(com.hp.hpl.jena.util.iterator.Filter)
	 */
	@Override
	public ExtendedIterator<Triple> filterDrop(Filter<Triple> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.util.iterator.ExtendedIterator#filterKeep(com.hp.hpl.jena.util.iterator.Filter)
	 */
	@Override
	public ExtendedIterator<Triple> filterKeep(Filter<Triple> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.util.iterator.ExtendedIterator#mapWith(com.hp.hpl.jena.util.iterator.Map1)
	 */
	@Override
	public <U> ExtendedIterator<U> mapWith(Map1<Triple, U> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.util.iterator.ExtendedIterator#removeNext()
	 */
	@Override
	public Triple removeNext() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.util.iterator.ExtendedIterator#toList()
	 */
	@Override
	public List<Triple> toList() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.util.iterator.ExtendedIterator#toSet()
	 */
	@Override
	public Set<Triple> toSet() {
		// TODO Auto-generated method stub
		return null;
	}

}
