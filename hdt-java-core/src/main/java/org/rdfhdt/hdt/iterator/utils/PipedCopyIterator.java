package org.rdfhdt.hdt.iterator.utils;

import org.rdfhdt.hdt.compact.integer.VByte;
import org.rdfhdt.hdt.util.io.IOUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.function.Function;

/**
 * a utility class to create an iterator from the value returned by another Thread
 *
 * @param <T> the iterator type
 * @author Antoine Willerval
 */

public class PipedCopyIterator<T> implements Iterator<T> {
	/**
	 * RuntimeException generated by the PipedCopyIterator
	 *
	 * @author Antoine Willerval
	 */
	public static class PipedIteratorException extends RuntimeException {
		public PipedIteratorException(String message, Throwable t) {
			super(message, t);
		}
	}


	/**
	 * Callback for the {@link #createOfCallback(PipedCopyIterator.Parser, PipedCopyIterator.PipeCallBack)}  method
	 *
	 * @param <T> the iterator type
	 * @author Antoine Willerval
	 */
	@FunctionalInterface
	public interface PipeCallBack<T> {
		/**
		 * method called from the new thread to generate the new data, at the end of the callback, the pipe is closed
		 * with or without exception
		 *
		 * @param pipe the pipe to fill
		 * @throws Exception any exception returned by the generator
		 */
		void createPipe(PipedCopyIterator<T> pipe) throws Exception;
	}

	/**
	 * create a piped iterator from a callback runner, the call to the callback should be made in the callbackRunner
	 *
	 * @param serializer serializer to pass the data
	 * @param callbackRunner the callback runner
	 * @param <T>            type of the iterator
	 * @return the iterator
	 */
	public static <T> PipedCopyIterator<T> createOfCallback(Parser<T> serializer, PipeCallBack<T> callbackRunner) {
		PipedCopyIterator<T> pipe = new PipedCopyIterator<>(serializer);

		Thread thread = new Thread(() -> {
			try {
				callbackRunner.createPipe(pipe);
				pipe.closePipe();
			} catch (Throwable e) {
				pipe.closePipe(e);
			}
		}, "PipeIterator");
		thread.start();

		return pipe;
	}
	public interface Parser<T> {
		static void writeString(CharSequence s, OutputStream out) throws IOException {
			byte[] bytes = s.toString().getBytes(StandardCharsets.UTF_8);
			VByte.encode(out, bytes.length);
			out.write(bytes);
		}
		static String readString(InputStream in) throws IOException {
			int size = (int) VByte.decode(in);
			byte[] bytes = IOUtil.readBuffer(in, size, null);
			return new String(bytes, StandardCharsets.UTF_8);
		}
		void write(T t, OutputStream stream) throws IOException;
		T read(InputStream stream) throws IOException;
	}

	private final PipedInputStream in;
	private final PipedOutputStream out;
	private final Parser<T> serializer;
	private T next;
	private boolean end;
	private PipedIteratorException exception;

	public PipedCopyIterator(Parser<T> serializer) {
		this.serializer = serializer;
		try {
			in = new PipedInputStream();
			out = new PipedOutputStream();
			in.connect(out);
		} catch (IOException e) {
			throw new PipedIteratorException("can't connect pipe", e);
		}
	}
	private int readByte() {
		try {
			return in.read();
		} catch (IOException e) {
			throw new PipedIteratorException("Can't read byte", e);
		}
	}

	@Override
	public boolean hasNext() {
		if (end) {
			return false;
		}
		if (next != null) {
			return true;
		}

		int b = readByte();
		if (b == 0) {
			end = true;
			if (exception != null) {
				throw exception;
			}
			return false;
		}
		try {
			next = serializer.read(in);
		} catch (IOException e) {
			throw new PipedIteratorException("Can't read pipe", e);
		}
		return true;
	}

	@Override
	public T next() {
		if (!hasNext()) {
			return null;
		}
		T next = this.next;
		this.next = null;
		return next;
	}

	public void closePipe() {
		closePipe(null);
	}
	public void closePipe(Throwable e) {
		if (e != null) {
			if (e instanceof PipedIteratorException) {
				this.exception = (PipedIteratorException) e;
			} else {
				this.exception = new PipedIteratorException("closing exception", e);
			}
		}
		try {
			// end byte
			out.write(0);
		} catch (IOException ee) {
			throw new PipedIteratorException("Can't close pipe", ee);
		}
	}

	/**
	 * map this iterator to another type
	 * @param mappingFunction the mapping function
	 * @param <E> the future type
	 * @return mapped iterator
	 */
	public <E> Iterator<E> map(Function<T, E> mappingFunction) {
		return new MapIterator<>(this, mappingFunction);
	}
	/**
	 * map this iterator to another type
	 * @param mappingFunction the mapping function
	 * @param <E> the future type
	 * @return mapped iterator
	 */
	public <E> Iterator<E> mapWithId(MapIterator.MapWithIdFunction<T, E> mappingFunction) {
		return new MapIterator<>(this, mappingFunction);
	}

	public void addElement(T node) {
		try {
			// not end byte
			out.write(1);
			serializer.write(node, out);
		} catch (IOException ee) {
			throw new PipedIteratorException("Can't add element to pipe", ee);
		}
	}
}
