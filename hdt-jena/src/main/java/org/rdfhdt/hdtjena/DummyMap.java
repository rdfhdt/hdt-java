package org.rdfhdt.hdtjena;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class DummyMap<T,K> implements Map<T,K> {

	@SuppressWarnings("rawtypes")
	private static DummyMap instance= new DummyMap();
	
	@SuppressWarnings("unchecked")
	public static <K,V> Map<K,V> getInstance() {
		return (Map<K,V>) instance;
	}
		
	@Override
	public int size() {
		return 0;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public boolean containsKey(Object key) {		
		return false;
	}

	@Override
	public boolean containsValue(Object value) {
		return false;
	}

	@Override
	public K get(Object key) {
		return null;
	}

	@Override
	public K put(T key, K value) {
		return null;
	}

	@Override
	public K remove(Object key) {
		return null;
	}

	@Override
	public void putAll(Map<? extends T, ? extends K> m) {

	}

	@Override
	public void clear() {
		
	}

	@Override
	public Set<T> keySet() {
		return Collections.emptySet();
	}

	@Override
	public Collection<K> values() {
		return Collections.emptyList();
	}

	@Override
	public Set<java.util.Map.Entry<T, K>> entrySet() {
		return Collections.emptySet();
	}

}
