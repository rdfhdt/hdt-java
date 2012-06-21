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
package hdt.iterator;

import hdt.exceptions.NotImplementedException;

import java.util.Comparator;
import java.util.Iterator;

/**
 * Given two iterators of sorted elements, returns the merge of the two in order, discarding duplicates.
 * 
 * @author mck
 *
 */
public class MergedIterator<T> implements Iterator<T> {
	private Iterator<T> left, right;
	private T currentLeft, currentRight;
	private Comparator<T> comparator;
	
	public MergedIterator(Iterator<T> left, Iterator<T> right, Comparator<T> comparator) {
		this.left = left;
		this.right = right;
		this.comparator = comparator;
		
		advanceLeft();
		advanceRight();
	}
	
	/* (non-Javadoc)
	 * @see java.util.Iterator#hasNext()
	 */
	@Override
	public boolean hasNext() {
		return currentLeft!=null || currentRight!=null;
	}

	private T advanceLeft() {
		T tmp = currentLeft;
		if(left.hasNext()) {
			currentLeft = left.next();
		} else {
			currentLeft = null;
		}
		return tmp;
	}
	
	private T advanceRight() {
		T tmp = currentRight;
		if(right.hasNext()) {
			currentRight = right.next();
		} else {
			currentRight = null;
		}
		return tmp;
	}
	
	/* (non-Javadoc)
	 * @see java.util.Iterator#next()
	 */
	@Override
	public T next() {
		// No more on right
		if(currentLeft!=null && currentRight==null) {			
			return advanceLeft();
		}
		// No more on left
		if(currentLeft==null && currentRight!=null) {
			return advanceRight();
		}
		
		// Compare
		if(comparator.compare(currentLeft, currentRight)>0) {
			return advanceLeft();
		} else {
			return advanceRight();
		}
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#remove()
	 */
	@Override
	public void remove() {
		throw new NotImplementedException();
	}

}
