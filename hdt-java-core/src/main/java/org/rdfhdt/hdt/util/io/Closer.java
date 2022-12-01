package org.rdfhdt.hdt.util.io;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Class to close many {@link java.io.Closeable} objects at once without having to do a large try-finally tree
 *
 * @author Antoine Willerval
 */
public class Closer implements Iterable<Closeable>, Closeable {
	private final List<Closeable> list;

	private Closer(Closeable... other) {
		list = new ArrayList<>(Arrays.asList(other));
	}

	/**
	 * create closer with closeables
	 * @param other closeables
	 * @return closer
	 */
	public static Closer of(Closeable... other) {
		return new Closer(other);
	}

	/**
	 * add closeables to this closer
	 *
	 * @param other closeables
	 * @return this
	 */
	public Closer with(Closeable... other) {
		return with(List.of(other));
	}

	/**
	 * add closeables iterable to this closer
	 *
	 * @param iterable closeables
	 * @return this
	 */
	public Closer with(Iterable<? extends Closeable> iterable) {
		list.addAll(StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList()));
		return this;
	}

	@Override
	public Iterator<Closeable> iterator() {
		return list.iterator();
	}

	@Override
	public void close() throws IOException {
		IOUtil.closeAll(list);
	}
}
