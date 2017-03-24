/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/rdf/parsers/RDFParserSimple.java $
 * Revision: $Rev: 191 $
 * Last modified: $Date: 2013-03-03 11:41:43 +0000 (dom, 03 mar 2013) $
 * Last modified by: $Author: mario.arias $
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Contacting the authors:
 *   Mario Arias:               mario.arias@deri.org
 *   Javier D. Fernandez:       jfergar@infor.uva.es
 *   Miguel A. Martinez-Prieto: migumar2@infor.uva.es
 *   Alejandro Andres:          fuzzy.alej@gmail.com
 */

package org.rdfhdt.hdt.rdf.parsers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.rdf.RDFParserCallback;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mario.arias
 *
 */
public class RDFParserSimple implements RDFParserCallback {
	private static final Logger log = LoggerFactory.getLogger(RDFParserSimple.class);

	/* (non-Javadoc)
	 * @see hdt.rdf.RDFParserCallback#doParse(java.lang.String, java.lang.String, hdt.enums.RDFNotation, hdt.rdf.RDFParserCallback.RDFCallback)
	 */
	@Override
	public void doParse(String fileName, String baseUri, RDFNotation notation, RDFCallback callback) throws ParserException {
		BufferedReader reader;
		try {
			reader = IOUtil.getFileReader(fileName);
		} catch (IOException e) {
			e.printStackTrace();
			throw new ParserException(e);
		}
		try {
			doParse(reader, baseUri, notation, callback);
		} finally {
			IOUtil.closeQuietly(reader);
		}
	}
	
	public void doParse(InputStream input, String baseUri, RDFNotation notation, RDFCallback callback) throws ParserException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		try {
			doParse(reader, baseUri, notation, callback);
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
			}
		}
	}

	private void doParse(BufferedReader reader, String baseUri, RDFNotation notation, RDFCallback callback) throws ParserException {
		try {
			String line;
			long numLine = 1;
			TripleString triple = new TripleString();
			while((line=reader.readLine())!=null) {
				
				line = line.trim().replaceAll("\\t"," ");
				if(!line.startsWith("#")) {
					triple.read(line);
					if(!triple.hasEmpty()) {
						//System.out.println(triple);
						callback.processTriple(triple, 0);
					} else {
						System.err.println("Warning: Could not parse triple at line "+numLine+", ignored and not processed.\n"+line);
					}
				}
				numLine++;
			}
			reader.close();
		}catch(Exception e) {
			log.error("Unexpected exception.", e);
			throw new ParserException(e);
		}
	}
}
