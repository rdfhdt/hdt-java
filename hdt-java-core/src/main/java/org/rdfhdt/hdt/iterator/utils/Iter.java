package org.rdfhdt.hdt.iterator.utils;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class Iter {

	public static <T, R> Iterable<R> mapIterable(final Iterable<T> iter1, final Transform<T, R> converter) {
		return new Iterable<R>() {
			@Override
			public Iterator<R> iterator() {
				return Iter.map(iter1, converter);
			}
		};
	}


	public static <T, R> Iterator<R> map(Iterable<? extends T> stream, Transform<T, R> converter) {
		return map(stream.iterator(), converter) ;
	}

	public static <T, R> Iterator<R> map(final Iterator<? extends T> stream, final Transform<T, R> converter) {
		final Iterator<R> iter = new Iterator<R>() {
			@Override
			public boolean hasNext() {
				return stream.hasNext() ;
			}

			@Override
			public R next() {
				return converter.convert(stream.next()) ;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("map.remove") ;
			}
		} ;
		return iter ;
	}


	public static <T> Iterator<T> filter(Iterable<? extends T> stream, Filter<T> filter) {
		return filter(stream.iterator(), filter) ;
	}

	public static <T> Iterator<T> filter(final Iterator<? extends T> stream, final Filter<T> filter) {
		final Iterator<T> iter = new Iterator<T>() {

			boolean finished     = false ;
			boolean slotOccupied = false ;
			T       slot ;

			@Override
			public boolean hasNext() {
				if ( finished )
					return false ;
				while (!slotOccupied) {
					if ( !stream.hasNext() ) {
						finished = true ;
						break ;
					}
					T nextItem = stream.next() ;
					if ( filter.accept(nextItem) ) {
						slot = nextItem ;
						slotOccupied = true ;
						break ;
					}
				}
				return slotOccupied ;
			}

			@Override
			public T next() {
				if ( hasNext() ) {
					slotOccupied = false ;
					return slot ;
				}
				throw new NoSuchElementException("filter.next") ;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("filter.remove") ;
			}
		} ;

		return iter ;
	}
}
