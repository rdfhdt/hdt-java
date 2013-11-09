/**
 * File: $HeadURL$
 * Revision: $Rev$
 * Last modified: $Date$
 * Last modified by: $Author$
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.rdf.RDFParserCallback;
import org.rdfhdt.hdt.triples.TripleString;

/**
 * @author mario.arias
 *
 */
public class RDFParserSimple implements RDFParserCallback {

	/* (non-Javadoc)
	 * @see hdt.rdf.RDFParserCallback#doParse(java.lang.String, java.lang.String, hdt.enums.RDFNotation, hdt.rdf.RDFParserCallback.RDFCallback)
	 */
	@Override
	public void doParse(String fileName, String baseUri, RDFNotation notation, RDFCallback callback) throws ParserException {
		try {
			BufferedReader reader; 
		
			if(fileName.equals("-")) {
				reader = new BufferedReader(new InputStreamReader(System.in));
			} else if(fileName.endsWith(".gz")) {
				reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(fileName))));
			} else {
				reader = new BufferedReader(new FileReader(new File(fileName)));
			}
			String line;
			long numLine = 1;
			TripleString triple = new TripleString();
			while((line=reader.readLine())!=null) {
				try {
					triple.read(line);
				} catch (Exception e) {
					System.err.println("Warning: Could not parse triple at line "+numLine+", ignored and not processed.\n Line: "+line+ "\nError: "+e.getMessage());
					
				}
				if(!triple.hasEmpty()) {
//					System.out.println(triple);
					callback.processTriple(triple, 0);
				} else {
					System.err.println("Warning: Could not parse triple at line "+numLine+", ignored and not processed.\n"+line);
				}
				numLine++;
			}
			reader.close();
		} catch(Exception e) {
			e.printStackTrace();
			throw new ParserException();
		}
	}

	public void doParse(InputStream input, RDFNotation notation, RDFCallback callback) throws ParserException {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));
			String line;
			long numLine = 1;
			TripleString triple = new TripleString();
			while((line=reader.readLine())!=null) {
				triple.read(line);
				if(!triple.hasEmpty()) {
//					System.out.println(triple);
					callback.processTriple(triple, 0);
				} else {
					System.err.println("Warning: Could not parse triple at line "+numLine+", ignored and not processed.\n"+line);
				}
				numLine++;
			}
			reader.close();
		}catch(Exception e) {
			e.printStackTrace();
			throw new ParserException();
		}
	}
	
}
