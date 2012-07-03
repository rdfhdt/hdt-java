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

import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.rdf.RDFParserCallback;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.util.StopWatch;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;


/**
 * @author mario.arias
 *
 */
public class RDFParserJena implements RDFParserCallback {

	/* (non-Javadoc)
	 * @see hdt.rdf.RDFParserCallback#doParse(java.lang.String, java.lang.String, datatypes.RDFNotation, hdt.rdf.RDFParserCallback.Callback)
	 */
	@Override
	public void doParse(String fileName, String baseUri, RDFNotation notation, RDFCallback callback) throws ParserException {
		try {
			Model model = ModelFactory.createDefaultModel();
			InputStream input = new BufferedInputStream(new FileInputStream(fileName));

			// read the RDF file and store it in "model"
			StopWatch st = new StopWatch();
			model.read(input, null, "N-TRIPLE");
			System.out.println("Load time: "+st.stopAndShow());
			input.close();

			StmtIterator iter = model.listStatements();
			TripleString triple = new TripleString("","","");
			int count = 0;
			while (iter.hasNext()) {
				Statement stm = iter.nextStatement();

				triple.setAll(
						stm.getSubject().toString(), 
						stm.getPredicate().toString(),
						stm.getObject().toString());
				callback.processTriple(triple, count++);
			}
		} catch (IOException e) {
			throw new ParserException();
		}
	}

}
