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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.jena.atlas.lib.Tuple;
import org.apache.jena.riot.RiotReader;
import org.apache.jena.riot.lang.LangNTriples;
import org.apache.jena.riot.lang.LangRDFXML;
import org.apache.jena.riot.lang.LangTurtle;
import org.apache.jena.riot.system.StreamRDF;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.rdf.RDFParserCallback;
import org.rdfhdt.hdt.triples.TripleString;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.core.Quad;

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
			if(fileName.endsWith(".gz")) {
				input = new BufferedInputStream(new GZIPInputStream(new FileInputStream(fileName)));
			} else {
				input = new BufferedInputStream(new FileInputStream(fileName));
			}
			switch(notation) {
				case NTRIPLES:
					LangNTriples langNtriples = RiotReader.createParserNTriples(input,this);
					langNtriples.parse();
					break;
				case RDFXML:
					LangRDFXML langRdfxml = RiotReader.createParserRDFXML(input, baseUri, this);
					langRdfxml.parse();
					break;
				case TURTLE:
					LangTurtle langTurtle = RiotReader.createParserTurtle(input, baseUri, this);
					langTurtle.parse();
					break;
				default:
					throw new NotImplementedException("Parser not found for format "+notation);	
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
//			throw new ParserException();
		} catch (Exception e) {
			e.printStackTrace();
//			throw new ParserException(	);
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
	public void tuple(Tuple<Node> tuple) {
		List<Node> l = tuple.asList();
		if(l.size()<3) {
			throw new RuntimeException(new ParserException("Received tuple with less than three components"));
		}
		triple.setSubject(l.get(0).toString());
		triple.setPredicate(l.get(1).toString());
		triple.setObject(l.get(2).toString());
		callback.processTriple(triple, 0);
	}

	@Override
	public void base(String base) {
//		System.out.println("Base: "+base);
	}

	@Override
	public void prefix(String prefix, String iri) {
//		System.out.println("Prefix: "+prefix+" iri "+iri);
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub
		
	}

}
