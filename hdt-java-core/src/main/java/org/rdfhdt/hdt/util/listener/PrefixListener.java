package org.rdfhdt.hdt.util.listener;

import org.rdfhdt.hdt.listener.ProgressListener;

/**
 * Simple {@link org.rdfhdt.hdt.listener.ProgressListener} implementation concatenating a prefix to each message
 *
 * @author Antoine Willerval
 */
public class PrefixListener implements ProgressListener {
	/**
	 * create a prefix listener from another listener
	 *
	 * @param prefix   prefix to concat to the messages
	 * @param listener the listener
	 * @return null if listener is null, listener if prefix is null or empty or a prefix listener
	 */
	public static ProgressListener of(String prefix, ProgressListener listener) {
		if (listener == null) {
			return null;
		}

		if (prefix == null || prefix.isEmpty()) {
			return listener;
		}

		return new PrefixListener(prefix, listener);
	}

	private final String prefix;
	private final ProgressListener listener;

	private PrefixListener(String prefix, ProgressListener listener) {
		this.prefix = prefix;
		this.listener = listener;
	}

	@Override
	public void notifyProgress(float level, String message) {
		listener.notifyProgress(level, prefix + message);
	}
}
