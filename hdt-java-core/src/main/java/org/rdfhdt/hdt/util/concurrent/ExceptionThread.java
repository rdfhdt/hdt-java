package org.rdfhdt.hdt.util.concurrent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread allowing exception and returning it when joining it with {@link #joinAndCrashIfRequired()} or by using
 * {@link #getException()}, can be attached to other threads to crash the others if an exception occurs in one of
 * them with {@link #attach(ExceptionThread...)}.
 *
 * @author Antoine Willerval
 */
public class ExceptionThread extends Thread {
	private static final AtomicLong ID_COUNT = new AtomicLong();
	static boolean debug;
	static final Stack<Map<Long, Throwable>> DEBUG_STACK = new Stack<>();

	/**
	 * start the debug of the thread
	 */
	public static void startDebug() {
		debug = true;
		if (!DEBUG_STACK.isEmpty()) {
			throw new IllegalArgumentException("non empty debug stack, bad config?");
		}
		pushDebugPoint();
	}

	/**
	 * push a new sub-set of debug thread
	 */
	public static void pushDebugPoint() {
		DEBUG_STACK.push(Collections.synchronizedMap(new HashMap<>()));
	}

	/**
	 * push a new sub-set of debug thread
	 *
	 * @param name name to id the pop
	 */
	public static void popDebugPoint(String name) {
		if (DEBUG_STACK.isEmpty()) {
			throw new IllegalArgumentException("empty debug stack, bad config?");
		}

		Map<Long, Throwable> map = DEBUG_STACK.pop();
		if (map.isEmpty()) {
			return;
		}

		AssertionError error = new AssertionError("Non empty stack at point " + name);

		map.values().forEach(error::addSuppressed);

		throw error;
	}

	/**
	 * end the debug of the thread
	 */
	public static void endDebug() {
		debug = false;
		popDebugPoint("end debug");
		DEBUG_STACK.clear();
	}

	/**
	 * create exception threads of multiple runnables
	 *
	 * @param name      common name
	 * @param runnables the runnables list, can't be empty
	 * @return exception thread attached with other runnables
	 * @throws java.lang.IllegalArgumentException if the array is empty
	 * @throws java.lang.NullPointerException     if an argument is null
	 */
	public static ExceptionThread async(String name, ExceptionRunnable... runnables) {
		Objects.requireNonNull(name, "name can't be null!");
		Objects.requireNonNull(runnables, "runnables can't be null");
		for (int i = 0; i < runnables.length; i++) {
			Objects.requireNonNull(runnables[i], "runnable#" + i + " is null!");
		}
		if (runnables.length == 0) {
			throw new IllegalArgumentException("empty runnable list");
		}

		ExceptionThread thread = new ExceptionThread(runnables[0], name + "#" + 0);

		for (int i = 1; i < runnables.length; i++) {
			thread.attach(new ExceptionThread(runnables[i], name + "#" + i));
		}

		return thread;
	}


	/**
	 * Version of {@link java.lang.Runnable} with an exception
	 */
	@FunctionalInterface
	public interface ExceptionRunnable {
		/**
		 * Runnable used in an {@link org.rdfhdt.hdt.util.concurrent.ExceptionThread}, can throw an exception
		 *
		 * @throws java.lang.Exception if any
		 * @see org.rdfhdt.hdt.util.concurrent.ExceptionThread#ExceptionThread(org.rdfhdt.hdt.util.concurrent.ExceptionThread.ExceptionRunnable, String)
		 */
		void run() throws Exception;
	}

	private Throwable exception = null;
	private final ExceptionRunnable target;
	private ExceptionThread next;
	private ExceptionThread prev;
	private final Map<Long, Throwable> debugMap;
	private final long debugId;

	public ExceptionThread(String name) {
		this(null, name);
	}

	public ExceptionThread(ExceptionRunnable target, String name) {
		super(name);
		debugId = ID_COUNT.getAndIncrement();

		if (debug) {
			debugMap = DEBUG_STACK.peek();
			if (debugMap != null) {
				// debug
				debugMap.put(debugId, new Throwable("ExceptionThread #" + name));
			}
		} else {
			debugMap = null;
		}

		this.target = Objects.requireNonNullElse(target, this::runException);
	}

	/**
	 * attach another threads to wait with this one
	 *
	 * @param threads others
	 * @return this
	 */
	public ExceptionThread attach(ExceptionThread... threads) {
		Objects.requireNonNull(threads, "can't attach null thread");
		for (ExceptionThread thread : threads) {
			if (thread.prev != null) {
				throw new IllegalArgumentException("Thread " + thread.getName() + " already attached");
			}
			if (this.next != null) {
				this.next.attach(thread);
				continue;
			}
			this.next = thread;
			thread.prev = this;
		}
		return this;
	}

	/**
	 * start this thread and all attached thread
	 *
	 * @return this
	 */
	public ExceptionThread startAll() {
		ExceptionThread prev = this.prev;
		while (prev != null) {
			prev.start();
			prev = prev.prev;
		}
		start();
		ExceptionThread next = this.next;
		while (next != null) {
			next.start();
			next = next.next;
		}
		return this;
	}

	/**
	 * implementation used if the runnable is null
	 * @throws Exception exception
	 */
	public void runException() throws Exception {
		// to impl
	}

	@Override
	public final void run() {
		try {
			target.run();
		} catch (Throwable t) {
			if (exception != null) {
				exception.addSuppressed(t);
				return; // another attached thread crashed, probably interruption exception
			}
			exception = t;
			if (this.next != null) {
				this.next.interruptForward(t);
			}
			if (this.prev != null) {
				this.prev.interruptBackward(t);
			}
		} finally {
			if (debugMap != null) {
				debugMap.remove(debugId);
			}
		}
	}

	private void interruptBackward(Throwable t) {
		exception = t;
		if (this.prev != null) {
			this.prev.interruptBackward(t);
		}
		interrupt();
	}

	private void interruptForward(Throwable t) {
		exception = t;
		if (this.next != null) {
			this.next.interruptForward(t);
		}
		interrupt();
	}

	/**
	 * @return the exception returned by this thread, another attached thread or null if no exception occurred
	 */
	public Throwable getException() {
		return exception;
	}

	/**
	 * join this thread and create an exception if required, will convert it to a runtime exception if it can't be
	 * created. If the thread returned an exception while the current thread is interrupted, the exception will be
	 * suppressed in the {@link java.lang.InterruptedException}.
	 *
	 * @throws InterruptedException     interruption while joining the thread
	 * @throws ExceptionThreadException if the thread or any attached thread returned an exception
	 */
	public void joinAndCrashIfRequired() throws InterruptedException {
		try {
			join();
			ExceptionThread next = this.next;
			while (next != null) {
				next.join();
				next = next.next;
			}
			ExceptionThread prev = this.prev;
			while (prev != null) {
				prev.join();
				prev = prev.prev;
			}
		} catch (InterruptedException ie) {
			// we got an exception in the thread while this thread was interrupted
			if (exception != null) {
				ie.addSuppressed(exception);
			}
			throw ie;
		}
		if (exception == null) {
			return;
		}
		if (exception instanceof ExceptionThreadException) {
			throw (ExceptionThreadException) exception;
		}
		throw new ExceptionThreadException(exception);
	}

	/**
	 * Exception returned by {@link #joinAndCrashIfRequired()}, will always have a cause
	 *
	 * @author Antoine Willerval
	 */
	public static class ExceptionThreadException extends RuntimeException {
		public ExceptionThreadException(Throwable cause) {
			super(cause);
		}
	}

}
