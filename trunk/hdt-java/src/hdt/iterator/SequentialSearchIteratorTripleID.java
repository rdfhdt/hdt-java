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

package hdt.iterator;

import hdt.enums.ResultEstimationType;
import hdt.enums.TripleComponentOrder;
import hdt.triples.TripleID;

/**
 * Given an iterator of TripleID's, provides a new iterator that filters only triples that match the supplied pattern.
 * 
 * @author mario.arias
 *
 */
public class SequentialSearchIteratorTripleID implements IteratorTripleID {
	private TripleID pattern, nextTriple, previousTriple, returnTriple;
	IteratorTripleID iterator;
	boolean hasMoreTriples, hasPreviousTriples;
	boolean goingUp;
	
	public SequentialSearchIteratorTripleID(TripleID pattern, IteratorTripleID other) {
		this.pattern = pattern;
		this.iterator = other;
		hasPreviousTriples = false;
		goingUp = true;
		nextTriple = new TripleID();
		doFetchNext();
	}
	
	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#hasNext()
	 */
	@Override
	public boolean hasNext() {
		return hasMoreTriples;
	}
	
	private void doFetchNext() {
		hasMoreTriples = false;
		
		while(iterator.hasNext()) {
			TripleID next = iterator.next();
			
			if(next.match(pattern)) {
				hasMoreTriples = true;
				hasPreviousTriples = true;
				nextTriple.set(next);
				break;
			}
		}
	}
	

	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#next()
	 */
	@Override
	public TripleID next() {
		if(!goingUp) {
			goingUp = true;
			if(hasPreviousTriples) {
				doFetchNext();
			}
			doFetchNext();
		}
		returnTriple = nextTriple;
		
		doFetchNext();
		
		return returnTriple;
	}
	
	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#hasPrevious()
	 */
	@Override
	public boolean hasPrevious() {
		return hasPreviousTriples;
	}
	
	private void doFetchPrevious() {
		hasPreviousTriples = false;
        //cout << "Seq PREV: " << iterator->hasPrevious() << ", " << iterator->hasNext() << endl;

        while(iterator.hasPrevious()){
                TripleID previous = iterator.previous();

                if(previous.match(pattern)) {
                        hasPreviousTriples = true;
                        hasMoreTriples = true;
                        previousTriple = previous;
                        break;
                }
        }
	}

	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#previous()
	 */
	@Override
	public TripleID previous() {
		if(goingUp) {
			goingUp = false;
			if(hasMoreTriples) {
				doFetchPrevious();
			}
			doFetchPrevious();
		}
		returnTriple = previousTriple;

		doFetchPrevious();

		return returnTriple;
	}

	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#goToStart()
	 */
	@Override
	public void goToStart() {
		iterator.goToStart();
		doFetchNext();
	}

	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#estimatedNumResults()
	 */
	@Override
	public long estimatedNumResults() {
		return iterator.estimatedNumResults();
	}

	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#numResultEstimation()
	 */
	@Override
	public ResultEstimationType numResultEstimation() {
		return ResultEstimationType.UP_TO;
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
		throw new IllegalArgumentException("Called goTo() on an unsupported implementation");
	}
	
	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#getOrder()
	 */
	@Override
	public TripleComponentOrder getOrder() {
		return iterator.getOrder();
	}
	

	/* (non-Javadoc)
	 * @see java.util.Iterator#remove()
	 */
	@Override
	public void remove() {
		
	}
	
	
}
