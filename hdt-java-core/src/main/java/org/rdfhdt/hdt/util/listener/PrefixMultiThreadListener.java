package org.rdfhdt.hdt.util.listener;

import org.rdfhdt.hdt.listener.MultiThreadListener;
import org.rdfhdt.hdt.listener.ProgressListener;

/**
 * Simple implementation of {@link org.rdfhdt.hdt.listener.MultiThreadListener} redirecting all the progression to
 * a progression listener with a prefix
 *
 * @author Antoine Willerval
 */
public class PrefixMultiThreadListener implements MultiThreadListener {

	private final ProgressListener progressListener;

	public PrefixMultiThreadListener(ProgressListener progressListener) {
		this.progressListener = progressListener;
	}

	@Override
	public void notifyProgress(String thread, float level, String message) {
		progressListener.notifyProgress(level, "[" + thread + "]" + message);
	}
}
