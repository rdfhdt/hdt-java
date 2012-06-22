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

package hdt.triples.impl;

import hdt.compact.bitmap.AdjacencyList;
import hdt.enums.ResultEstimationType;
import hdt.enums.TripleComponentOrder;
import hdt.exceptions.NotImplementedException;
import hdt.iterator.IteratorTripleID;
import hdt.triples.TripleID;

/**
 * @author mario.arias
 *
 */
public class BitmapTriplesIteratorZFOQ implements IteratorTripleID {
	private BitmapTriples triples;
	private TripleID pattern, returnTriple;
	
	private AdjacencyList adjY, adjIndex;
	private long posIndex, minIndex, maxIndex;
	private int x, y, z;
	
	private int patY, patZ;
	
	BitmapTriplesIteratorZFOQ(BitmapTriples triples, TripleID pattern) {
		this.triples = triples;
		this.pattern = new TripleID(pattern);
		this.returnTriple = new TripleID();
		
		TripleOrderConvert.swapComponentOrder(this.pattern, TripleComponentOrder.SPO, triples.order);
		patZ = this.pattern.getObject();
		if(patZ==0) {
			throw new IllegalArgumentException("This structure is not meant to process this pattern");
		}
		
	    patY = pattern.getPredicate();
	    patZ = pattern.getObject();
		
		adjY = new AdjacencyList(triples.arrayY, triples.bitmapY);
		adjIndex = new AdjacencyList(triples.indexZ, triples.bitmapIndexZ);
		
		calculateRange();
		goToStart();
	}
	
	private long getY(long index) {
		return adjY.get(adjIndex.get(index));
	}
	
	private void calculateRange() {
		minIndex = adjIndex.find(patZ-1);
		maxIndex = adjIndex.last(patZ-1);

		if(patY!=0) {
			while (minIndex <= maxIndex) {
				long mid = (minIndex + maxIndex) / 2;
				long predicate=getY(mid);        

				if (patY > predicate) {
					minIndex = mid + 1;
				} else if (patY < predicate) {
					maxIndex = mid - 1;
				} else {
					// Binary Search to find left boundary
					long left=minIndex;
					long right=mid;
					long pos=0;

					while(left<=right) {
						pos = (left+right)/2;

						predicate = getY(pos);

						if(predicate!=patY) {
							left = pos+1;
						} else {
							right = pos-1;
						}
					}
					minIndex = predicate==patY ? pos : pos+1;

					// Binary Search to find right boundary
					left = mid;
					right= maxIndex;

					while(left<=right) {
						pos = (left+right)/2;
						predicate = getY(pos);

						if(predicate!=patY) {
							right = pos-1;
						} else {
							left = pos+1;
						}
					}
					maxIndex = predicate==patY ? pos : pos-1;

					break;
				}
			}
		}
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
		return posIndex<=maxIndex;
	}
	
	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#next()
	 */
	@Override
	public TripleID next() {
	    long posY = adjIndex.get(posIndex);

	    z = patZ;
	    y = patY!=0 ? patY : (int) adjY.get(posY);
	    x = (int) adjY.findListIndex(posY)+1;

	    posIndex++;

	    updateOutput();
	    return returnTriple;
	}

	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#hasPrevious()
	 */
	@Override
	public boolean hasPrevious() {
		return posIndex>minIndex;
	}

	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#previous()
	 */
	@Override
	public TripleID previous() {
		posIndex--;

		long posY = adjIndex.get(posIndex);

		z = patZ;
		y = patY!=0 ? patY : (int) adjY.get(posY);
		x = (int) adjY.findListIndex(posY)+1;

		updateOutput();
		return returnTriple;
	}

	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#goToStart()
	 */
	@Override
	public void goToStart() {
		posIndex = minIndex;
	}

	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#estimatedNumResults()
	 */
	@Override
	public long estimatedNumResults() {
		return maxIndex-minIndex+1;
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
		return true;
	}

	/* (non-Javadoc)
	 * @see hdt.iterator.IteratorTripleID#goTo(int)
	 */
	@Override
	public void goTo(long pos) {
		if(pos>maxIndex-minIndex || pos<0) {
			throw new IndexOutOfBoundsException();
		}
		posIndex = minIndex+pos;
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
