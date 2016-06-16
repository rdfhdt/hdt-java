/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/rdf/parsers/RDFParserRIOT.java $
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
 */

package org.rdfhdt.hdt.rdf.parsers;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.jena.atlas.lib.Tuple;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Node_Literal;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.rdf.RDFParserCallback;
import org.rdfhdt.hdt.triples.TripleString;

/**
 * @author mario.arias
 *
 */
public class RDFParserRIOT implements RDFParserCallback, StreamRDF {
	private RDFCallback callback;
	private TripleString triple = new TripleString();
	
	/* (non-Javadoc)
	 * @see hdt.rdf.RDFParserCallback#doParse(java.lang.String, java.lang.String, hdt.enums.RDFNotation, hdt.rdf.RDFParserCallback.Callback)
	 */
	@Override
	public void doParse(String fileName, String baseUri, RDFNotation notation, RDFCallback callback) throws ParserException {
		this.callback = callback;
		try {
			InputStream input;
			if(fileName.equals("-")) {
				input = new BufferedInputStream(System.in);
			} else if(fileName.endsWith(".gz")) {
				input = new BufferedInputStream(new GZIPInputStream(new FileInputStream(fileName)));
			} else {
				input = new BufferedInputStream(new FileInputStream(fileName));
			}
			switch(notation) {
				case NTRIPLES:
					RDFDataMgr.parse(this, input, Lang.NTRIPLES);
					break;
				case RDFXML:
					RDFDataMgr.parse(this, input, baseUri, Lang.RDFXML);
					break;
				case TURTLE:
					RDFDataMgr.parse(this, input, baseUri, Lang.TURTLE);
					break;
				default:
					throw new NotImplementedException("Parser not found for format "+notation);	
			}
		} catch (FileNotFoundException e) {
			throw new ParserException();
		} catch (Exception e) {
			throw new ParserException();
		}	
	}
	
	public void doParse(InputStream input, String baseUri, RDFNotation notation, RDFCallback callback) throws ParserException {
		this.callback = callback;
		try {
			switch(notation) {
				case NTRIPLES:
					RDFDataMgr.parse(this, input, Lang.NTRIPLES);
					break;
				case RDFXML:
					RDFDataMgr.parse(this, input, baseUri, Lang.RDFXML);
					break;
				case N3:
				case TURTLE:
					RDFDataMgr.parse(this, input, baseUri, Lang.TURTLE);
					break;
				default:
					throw new NotImplementedException("Parser not found for format "+notation);	
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new ParserException();
		}	
	}

	 @Override
     public void start() {
             // TODO Auto-generated method stub
             
     }

     @Override
     public void triple(Triple parsedTriple) {
             triple.setAll(parsedTriple.getSubject().toString(), parsedTriple.getPredicate().toString(), parsedTriple.getObject().toString());
             callback.processTriple(triple, 0);              
     }

     @Override
     public void quad(Quad quad) {
             triple.setAll(quad.getSubject().toString(), quad.getPredicate().toString(), quad.getObject().toString());
             callback.processTriple(triple, 0);              
     }

     @Override
     public void base(String base) {
//           System.out.println("Base: "+base);
     }

     @Override
     public void prefix(String prefix, String iri) {
//           System.out.println("Prefix: "+prefix+" iri "+iri);
     }

     @Override
     public void finish() {
             // TODO Auto-generated method stub
             
     }


}
