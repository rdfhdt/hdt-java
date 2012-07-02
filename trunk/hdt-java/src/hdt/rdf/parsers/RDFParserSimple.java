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

package hdt.rdf.parsers;

import hdt.enums.RDFNotation;
import hdt.exceptions.ParserException;
import hdt.rdf.RDFParserCallback;
import hdt.triples.TripleString;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

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
			BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
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
