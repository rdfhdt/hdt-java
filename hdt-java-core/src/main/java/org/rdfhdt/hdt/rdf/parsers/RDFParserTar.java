package org.rdfhdt.hdt.rdf.parsers;

import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.rdf.RDFParserCallback;
import org.rdfhdt.hdt.rdf.RDFParserFactory;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.io.NonCloseInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses a tar file (optionally .tgz or .tar.gz or .tar.bz2) directly, processing each file that contains rdf separately.
 * 
 * It uses RDFNotation.guess() to guess the format of each specific file. If not recognised, each file of the tar is ignored.
 *  
 *  
 * @author 
 *
 */

public class RDFParserTar implements RDFParserCallback {
	private static final Logger log = LoggerFactory.getLogger(RDFParserTar.class);

	
	/* (non-Javadoc)
	 * @see hdt.rdf.RDFParserCallback#doParse(java.lang.String, java.lang.String, hdt.enums.RDFNotation, hdt.rdf.RDFParserCallback.Callback)
	 */
	@Override
	public void doParse(String fileName, String baseUri, RDFNotation notation, RDFCallback callback) throws ParserException {
		try {
			InputStream input = IOUtil.getFileInputStream(fileName);
			this.doParse(input, baseUri, notation, callback);
			input.close();
		} catch (Exception e) {
			log.error("Unexpected exception parsing file: {}", fileName, e);
			throw new ParserException();
		} 
	}

	@Override
	public void doParse(InputStream input, String baseUri, RDFNotation notation, RDFCallback callback) throws ParserException {
		try {

			final TarArchiveInputStream debInputStream = (TarArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream("tar", input);
			TarArchiveEntry entry = null;
			
			// Make sure that the parser does not close the Tar Stream so we can read the rest of the files.
			NonCloseInputStream nonCloseIn = new NonCloseInputStream(debInputStream);

			while((entry = (TarArchiveEntry)debInputStream.getNextEntry()) != null) {

				if(entry.isFile() && !entry.getName().contains("DS_Store")) {
					try {
						RDFNotation guessnot = RDFNotation.guess(entry.getName());
						log.info("Parse from tar: {} as {}", entry.getName(), guessnot);
						RDFParserCallback parser = RDFParserFactory.getParserCallback(guessnot);

						parser.doParse(nonCloseIn, baseUri, guessnot, callback);
					}catch (IllegalArgumentException | ParserException e1) {
						log.error("Unexpected exception.", e1);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new ParserException();
		} catch (Exception e) {
			e.printStackTrace();
			throw new ParserException();
		}
	}

}
