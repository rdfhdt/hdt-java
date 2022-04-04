package org.rdfhdt.hdt.listener;

import org.rdfhdt.hdt.listener.ProgressListener;

/**
 * version of {@link org.rdfhdt.hdt.listener.ProgressListener} for multi-thread logging
 */
@FunctionalInterface
public interface MultiThreadListener extends ProgressListener {

	/**
	 * Send progress notification
	 * @param thread thread name
	 * @param level percent of the task accomplished
	 * @param message Description of the operation
	 */
	void notifyProgress(String thread, float level, String message);

	/**
	 * Send progress notification, should call {@link #notifyProgress(String, float, String)}
	 * @param level percent of the task accomplished
	 * @param message Description of the operation
	 */
	default void notifyProgress(float level, String message) {
		notifyProgress(Thread.currentThread().getName(), level, message);
	}

	/**
	 * unregister all the thread
	 */
	default void unregisterAllThreads() {
		// should be filled by implementation if required
	}

	/**
	 * register a thread
	 * @param threadName the thread name
	 */
	default void registerThread(String threadName) {
		// should be filled by implementation if required
	}

	/**
	 * unregister a thread
	 * @param threadName the thread name
	 */
	default void unregisterThread(String threadName) {
		// should be filled by implementation if required
	}
}
