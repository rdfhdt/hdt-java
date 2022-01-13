/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/triples/impl/BitmapTriplesIteratorZ.java $
 * Revision: $Rev: 191 $
 * Last modified: $Date: 2013-03-03 11:41:43 +0000 (dom, 03 mar 2013) $
 * Last modified by: $Author: mario.arias $
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 3.0 of the License.
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

package org.rdfhdt.hdt.triples.impl;

import org.rdfhdt.hdt.compact.bitmap.AdjacencyList;
import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;

/**
 * @author mario.arias
 *
 */
public class BitmapTriplesIteratorZ implements IteratorTripleID {
	private final BitmapTriples triples;
	private final TripleID pattern;
    private final TripleID returnTriple;
	private final long patZ;
	
	private AdjacencyList adjY, adjZ;
	long posZ;
	private long x, y, z;
	
	BitmapTriplesIteratorZ(BitmapTriples triples, TripleID pattern) {
		this.triples = triples;
		this.pattern = new TripleID(pattern);
		this.returnTriple = new TripleID();
		
		TripleOrderConvert.swapComponentOrder(this.pattern, TripleComponentOrder.SPO, triples.order);
		patZ = this.pattern.getObject();
		if(patZ==0) {
			throw new IllegalArgumentException("This structure is not meant to process this pattern");
		}
		
		adjY = triples.adjY;
		adjZ = triples.adjZ;
				
		goToStart();
	}
	
	private void updateOutput() {
		returnTriple.setAll(x, y, z);
		if(pattern.isWithIndex())
			returnTriple.setAllPlusIndex(x, y, z,posZ+1);
		else
			returnTriple.setAll(x,y,z);

		TripleOrderConvert.swapComponentOrder(returnTriple, triples.order, TripleComponentOrder.SPO);
	}
	
	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#hasNext()
	 */
	@Override
	public boolean hasNext() {
		return posZ!=-1;
	}
	
	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#next()
	 */
	@Override
	public TripleID next() {
		// Find posY from posZ
		long posY = adjZ.findListIndex(posZ);
		
		// Set full triple
		z = adjZ.get(posZ);
		y = adjY.get(posY);
		x = adjY.findListIndex(posY)+1;

		updateOutput();

		// Go forward finding next appearance
		posZ = adjZ.findNextAppearance(posZ+1, patZ);

		
		return returnTriple;
	}

	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#hasPrevious()
	 */
	@Override
	public boolean hasPrevious() {
		throw new NotImplementedException();
	}

	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#previous()
	 */
	@Override
	public TripleID previous() {
		throw new NotImplementedException();
	}

	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#goToStart()
	 */
	@Override
	public void goToStart() {
		posZ = adjZ.findNextAppearance(0, patZ);
	}

	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#estimatedNumResults()
	 */
	@Override
	public long estimatedNumResults() {
		return adjZ.getNumberOfElements();
	}

	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#numResultEstimation()
	 */
	@Override
	public ResultEstimationType numResultEstimation() {
	    return ResultEstimationType.UNKNOWN;
	}

	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#canGoTo()
	 */
	@Override
	public boolean canGoTo() {
		return false;
	}

	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#goTo(int)
	 */
	@Override
	public void goTo(long pos) {
		if(!canGoTo()) {
			throw new IllegalAccessError("Cannot goto on this bitmaptriples pattern");
		}
	}
	
	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#getOrder()
	 */
	@Override
	public TripleComponentOrder getOrder() {
		return triples.order;
	}
	
	/* (non-Javadoc)
	 * @see java.util.Iterator#remove()
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
