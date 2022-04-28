package org.rdfhdt.hdt.rdf.parsers;

import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.rdf.RDFParserCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Antoine Willerval
 */
public class RDFParserHDT implements RDFParserCallback {
	private static final Logger log = LoggerFactory.getLogger(RDFParserHDT.class);

	@Override
	public void doParse(String fileName, String baseUri, RDFNotation notation, boolean keepBNode, RDFCallback callback) throws ParserException {
		try (HDT hdt = HDTManager.mapHDT(fileName)) {
			hdt.search("", "", "").forEachRemaining(t -> callback.processTriple(t, 0));
		} catch (IOException | NotFoundException e) {
			log.error("Unexpected exception.", e);
			throw new ParserException(e);
		}
	}

	@Override
	public void doParse(InputStream in, String baseUri, RDFNotation notation, boolean keepBNode, RDFCallback callback) throws ParserException {
		try {
			// create a temp
			Path tempFile = Files.createTempFile("hdtjava-reader", ".hdt");
			log.warn("Create temp file to store the HDT stream {}", tempFile);
			try {
				Files.copy(in, tempFile);
				doParse(tempFile.toAbsolutePath().toString(), baseUri, notation, keepBNode, callback);
			} finally {
				Files.deleteIfExists(tempFile);
			}
		} catch (IOException e) {
			log.error("Unexpected exception.", e);
			throw new ParserException(e);
		}
	}

}
