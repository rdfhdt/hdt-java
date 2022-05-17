package org.rdfhdt.hdt.rdf.parsers;

import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.rdf.RDFParserCallback;
import org.rdfhdt.hdt.rdf.RDFParserFactory;
import org.rdfhdt.hdt.util.ContainerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * @author Antoine Willerval
 */
public class RDFParserDir implements RDFParserCallback {
	private static final Logger log = LoggerFactory.getLogger(RDFParserDir.class);
	private final boolean simple;

	public RDFParserDir(boolean simple) {
		this.simple = simple;
	}

	public RDFParserDir() {
		this(false);
	}

	@Override
	public void doParse(String fileName, String baseUri, RDFNotation notation, boolean keepBNode, RDFCallback callback) throws ParserException {
		try {
			doParse(Path.of(fileName), baseUri, notation, keepBNode, callback);
		} catch (InvalidPathException e) {
			throw new ParserException(e);
		}
	}

	private void doParse(Path p, String baseUri, RDFNotation notation, boolean keepBNode, RDFCallback callback) throws ParserException {
		if (notation != RDFNotation.DIR) {
			throw new IllegalArgumentException("Can't parse notation different than " + RDFNotation.DIR + "!");
		}
		try {
			Files.list(p).forEach(child -> {
				try {
					if (Files.isDirectory(child)) {
						doParse(child, baseUri, RDFNotation.DIR, keepBNode, callback);
						return;
					}
					RDFParserCallback rdfParserCallback;
					RDFNotation childNotation;
					try {
						// get the notation of the file
						childNotation = RDFNotation.guess(child.toFile());
						rdfParserCallback = RDFParserFactory.getParserCallback(childNotation, simple);
					} catch (IllegalArgumentException e) {
						log.warn("Ignore file {}", child, e);
						return;
					}
					log.debug("parse {}", child);
					// we can parse it, parsing it
					rdfParserCallback.doParse(child.toAbsolutePath().toString(), baseUri, childNotation, keepBNode, callback);
				} catch (ParserException e) {
					throw new ContainerException(e);
				}
			});
		} catch (IOException | SecurityException e) {
			throw new ParserException(e);
		} catch (ContainerException e) {
			throw (ParserException) e.getCause();
		}
	}

	@Override
	public void doParse(InputStream in, String baseUri, RDFNotation notation, boolean keepBNode, RDFCallback callback) throws ParserException {
		throw new NotImplementedException("Can't parse a stream of directory!");
	}
}
