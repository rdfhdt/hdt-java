/**
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
/**
 * 
 */
package hdt.triples.impl;

import hdt.enums.ResultEstimationType;
import hdt.enums.TripleComponentOrder;
import hdt.exceptions.NotImplementedException;
import hdt.iterator.IteratorTripleID;
import hdt.triples.TripleID;

/**
 * @author mck
 *
 */
public class CompactTriplesIterator implements IteratorTripleID {
	private CompactTriples triples;
	private int posY, posZ;
	private int x, y, z;
	private TripleID returnTriple;
	private boolean goingUp;
	
	
	public CompactTriplesIterator(CompactTriples triples) {
		this.triples = triples;
		this.goToStart();
	}
	
	private void updateOutput() {
		returnTriple.setAll(x, y, z);
		TripleOrderConvert.swapComponentOrder(returnTriple, triples.order, TripleComponentOrder.SPO);
	}
	
	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#hasNext()
	 */
	@Override
	public boolean hasNext() {
		return posZ<triples.arrayZ.getNumberOfElements();
	}

	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#next()
	 */
	@Override
	public TripleID next() {
		if(!goingUp) {
			posY++;
			goingUp = true;
		}
		
		if(z==0) {
			z = (int)triples.arrayZ.get(posZ++);
			y = (int)triples.arrayY.get(posY++);
            if(y==0) {
                y = (int) triples.arrayY.get(posY++);
                x++;
            } 
        }

		updateOutput();
		
		return returnTriple;
	}

	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#hasPrevious()
	 */
	@Override
	public boolean hasPrevious() {
		return posZ>0;
	}

	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#previous()
	 */
	@Override
	public TripleID previous() {
		z = (int)triples.arrayZ.get(--posZ);

        if(goingUp) {
                posY--;
                goingUp = false;
        }

        if(z==0) {
                z = (int)triples.arrayZ.get(--posZ);

                y = (int)triples.arrayY.get(--posY);
                if(y==0) {
                        y = (int)triples.arrayY.get(--posY);
                        x--;
                }
        }

        updateOutput();
        
        return returnTriple;
	}

	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#goToStart()
	 */
	@Override
	public void goToStart() {
		posY = posZ = 0;
		x = 1;
		y = (int)triples.arrayY.get(posY++);
		goingUp = true;
	}

	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#estimatedNumResults()
	 */
	@Override
	public long estimatedNumResults() {
		return triples.numTriples;
	}

	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#numResultEstimation()
	 */
	@Override
	public ResultEstimationType numResultEstimation() {
		return ResultEstimationType.EXACT;
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
		throw new IllegalArgumentException();
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
		throw new NotImplementedException();
	}
}
