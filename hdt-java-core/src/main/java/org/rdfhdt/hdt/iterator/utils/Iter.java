package org.rdfhdt.hdt.iterator.utils;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class Iter {
	
	private Iter() {}

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

	public static <T> Iterator<T> dedup(Iterator<? extends T> it) {
		return new DedupIterator<T>(it);
	}

	public static <T> Iterator<T> filter(Iterable<? extends T> stream, Filter<T> filter) {
		return filter(stream.iterator(), filter) ;
	}
	
	public static <T> Iterator<T> filterNotEqual(final Iterator<? extends T> stream, final T sampleObject) {
		return filter(stream, new Filter<T>(){
			@Override
			public boolean accept(T item) {
				return !sampleObject.equals(item);
			}});
	}
	
	
	public static <T> Iterator<T> filter(final Iterator<? extends T> stream, final Filter<T> filter) {
		final Iterator<T> iter = new Iterator<T>() {

			boolean finished;
			boolean slotOccupied;
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


	public static <T> Iterator<T> limit(final Iterator<T> it, final int limit) {
		return new Iterator<T>() {
			
			int pos;

			@Override
			public boolean hasNext() {
				return pos<limit && it.hasNext();
			}

			@Override
			public T next() {
				return it.next();
			}

			@Override
			public void remove() {
				it.remove();
			}
		};
	}

	public static <T> Iterator<T> empty() {
		return new Iterator<T>() {
			@Override
			public boolean hasNext() {
				return false;
			}

			@Override
			public T next() {
				throw new NoSuchElementException();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
	public static <T> Iterator<T> single(final T element) {
		return new Iterator<T>() {
			boolean used = false;
			
			@Override
			public boolean hasNext() {
				return !used;
			}

			@Override
			public T next() {
				if(!used) {
					used=true;
					return element;
				}
				throw new NoSuchElementException();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
	public static <T> Iterator<T> side(final Iterator<T> it, final SideEffect<T> side) {
		return new Iterator<T>() {

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public T next() {
				T el = it.next();
				side.call(el);
				return el;
			}

			@Override
			public void remove() {
				
			}
		};
	}
	
	public static <T> void traverse(final Iterator<T> it) {
		while(it.hasNext()) {
			it.next();
		}
	}
	
	public static <T> long count(final Iterator<T> it) {
		long count=0;
		while(it.hasNext()){
			it.next();
			count++;
		}
		return count;
	}
}
