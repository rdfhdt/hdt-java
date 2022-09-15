package org.rdfhdt.hdt.rdf;

import org.rdfhdt.hdt.triples.TripleString;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Rdf flux stopper descriptor
 * @author Antoine Willerval
 */
public interface RDFFluxStop {
	/**
	 * @return basic implementation without any limit
	 */
	static RDFFluxStop noLimit() {
		return new RDFFluxStop() {
			@Override
			public boolean canHandle(TripleString ts) {
				return true;
			}

			@Override
			public void restart() {
				// nothing
			}
		};
	}

	/**
	 * implementation of flux stop stopping after a maximum triple count
	 *
	 * @param maxTriple maximum count
	 * @return FluxStop
	 */
	static RDFFluxStop countLimit(long maxTriple) {
		if (maxTriple <= 0) {
			throw new IllegalArgumentException("Can't have a limit of 0 or a negative value!");
		}
		return new RDFFluxStop() {
			long current = 0;

			@Override
			public boolean canHandle(TripleString ts) {
				return current++ < maxTriple;
			}

			@Override
			public void restart() {
				current = 0;
			}
		};
	}

	/**
	 * implementation of flux stop stopping after a maximum NTriple size
	 *
	 * @param maxSize maximum size
	 * @return FluxStop
	 */
	static RDFFluxStop sizeLimit(long maxSize) {
		if (maxSize <= 0) {
			throw new IllegalArgumentException("Can't have a limit of 0 or a negative value!");
		}
		return new RDFFluxStop() {
			long size = 0;

			@Override
			public boolean canHandle(TripleString ts) {
				long tsSize;
				try {
					tsSize = ts.asNtriple().toString().getBytes(StandardCharsets.UTF_8).length;
				} catch (IOException e) {
					throw new RuntimeException("Can't estimate the size of the triple " + ts, e);
				}
				try {
					return size < maxSize;
				} finally {
					size += tsSize;
				}
			}

			@Override
			public void restart() {
				size = 0;
			}
		};
	}

	/**
	 * should we stop the flux after this triple or not?
	 *
	 * @param ts the triple
	 * @return true if the flux can handle this triple, false otherwise
	 */
	boolean canHandle(TripleString ts);

	/**
	 * restart the flux stop
	 */
	void restart();
}
