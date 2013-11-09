package org.rdfhdt.hdt.hdt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.rdfhdt.hdt.listener.ProgressListener;

/**
 * HDT Operations that are using internally from the implementation.
 * @author mario.arias
 *
 */

public interface HDTPrivate extends HDT {
	/**
	 * Loads a HDT file
	 * 
	 * @param input
	 *            InputStream to read from
	 */
	public void loadFromHDT(InputStream input, ProgressListener listener) throws IOException;

	/**
	 * Loads a HDT file
	 * 
	 * @param input
	 *            InputStream to read from
	 */
	public void loadFromHDT(String fileName, ProgressListener listener) throws IOException;
	
	public void mapFromHDT(File f, long offset, ProgressListener listener) throws IOException;
	
	/**
	 * Generates any additional index needed to solve all triple patterns more efficiently
	 * 
	 * @param listener A listener to be notified of the progress.
	 */
	public void loadOrCreateIndex(ProgressListener listener);
	
	public void populateHeaderStructure(String baseUri);
}
