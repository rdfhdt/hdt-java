package org.rdfhdt.hdt.hdt;

import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTOptionsKeys;
import org.rdfhdt.hdt.triples.TripleString;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * Interface describing an HDT generator method
 *
 * @author Antoine Willerval
 */
@FunctionalInterface
public interface HDTSupplier {
	/**
	 * @return implementation using in-memory hdt
	 */
	static org.rdfhdt.hdt.hdt.HDTSupplier memory() {
		return (iterator, baseURI, hdtFormat, listener, location) -> {
			try (HDT hdt = HDTManager.generateHDT(iterator, baseURI, hdtFormat, listener)) {
				hdt.saveToHDT(location.toAbsolutePath().toString(), listener);
			}
		};
	}

	/**
	 * @return implementation using in-memory hdt
	 */
	static org.rdfhdt.hdt.hdt.HDTSupplier disk() {
		return (iterator, baseURI, hdtFormat, listener, location) -> {
			hdtFormat.set(HDTOptionsKeys.LOADER_DISK_FUTURE_HDT_LOCATION_KEY, location.toAbsolutePath().toString());
			HDTManager.generateHDTDisk(iterator, baseURI, hdtFormat, listener).close();
		};
	}

	/**
	 * Generate the HDT
	 *
	 * @param iterator  the iterator to create the hdt
	 * @param baseURI   the base URI (useless, but asked by some methods)
	 * @param hdtFormat the HDT options to create the HDT
	 * @param listener  listener
	 * @param location where to write the HDT
	 * @throws IOException     io exception while creating the HDT
	 * @throws ParserException parser exception while retrieving the triples
	 */
	void doGenerateHDT(Iterator<TripleString> iterator, String baseURI, HDTOptions hdtFormat, ProgressListener listener, Path location) throws IOException, ParserException;
}
