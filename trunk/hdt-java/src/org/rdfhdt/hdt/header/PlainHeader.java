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

package org.rdfhdt.hdt.header;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.rdfhdt.hdt.compact.integer.VByte;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.IllegalFormatException;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.iterator.IteratorTripleString;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.rdf.RDFParserCallback.RDFCallback;
import org.rdfhdt.hdt.rdf.parsers.RDFParserSimple;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.util.io.IOUtil;

/**
 * @author mario.arias
 *
 */
public class PlainHeader extends HeaderBase implements RDFCallback {
	
	protected HDTSpecification spec;
	protected List<TripleString> triples= new ArrayList<TripleString>();
	
	public PlainHeader() {
		spec = new HDTSpecification();
	}
	
	public PlainHeader(HDTSpecification spec) {
		this.spec = spec;
	}

	/* (non-Javadoc)
	 * @see hdt.rdf.RDFStorage#insert(hdt.triples.TripleString[])
	 */
	@Override
	public void insert(TripleString... newTriples) {
		for(TripleString triple : newTriples) {
			this.triples.add(triple);
		}
	}

	/* (non-Javadoc)
	 * @see hdt.rdf.RDFStorage#insert(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void insert(String subject, String predicate, String object) {
		if(object.startsWith("http://")||object.startsWith("file://")) {
			triples.add(new TripleString(subject, predicate, object));
		} else {
			triples.add(new TripleString(subject, predicate, '"'+object+'"'));
		}
	}

	/* (non-Javadoc)
	 * @see hdt.rdf.RDFStorage#insert(java.lang.String, java.lang.String, long)
	 */
	@Override
	public void insert(String subject, String predicate, long object) {
		triples.add(new TripleString(subject, predicate, '"'+Long.toString(object)+'"'));
	}

	/* (non-Javadoc)
	 * @see hdt.rdf.RDFStorage#remove(hdt.triples.TripleString[])
	 */
	@Override
	public void remove(TripleString... removedTriples) {
		for(TripleString triple : removedTriples) {
			triples.remove(triple);
		}	
	}

	/* (non-Javadoc)
	 * @see hdt.header.Header#save(java.io.OutputStream, hdt.ControlInformation, hdt.ProgressListener)
	 */
	@Override
	public void save(OutputStream output, ControlInformation ci, ProgressListener listener) throws IOException {
		ci.clear();
		ci.set("codification", HDTVocabulary.HEADER_PLAIN);
		ci.save(output);
		
		ByteArrayOutputStream headerData = new ByteArrayOutputStream();
		
		IteratorTripleString iterator = this.search("", "", "");
		while(iterator.hasNext()) {
			TripleString next = iterator.next();
			IOUtil.writeLine(headerData, next.asNtriple().toString());
		}
		
		VByte.encode(output, headerData.size());
		output.write(headerData.toByteArray());
	}

	/* (non-Javadoc)
	 * @see hdt.header.Header#load(java.io.InputStream, hdt.ControlInformation, hdt.ProgressListener)
	 */
	@Override
	public void load(InputStream input, ControlInformation ci, ProgressListener listener) throws IOException {
		String codification = ci.get("codification");
		if(!codification.equals(HDTVocabulary.HEADER_PLAIN)) {
			throw new IllegalArgumentException("Unexpected PlainHeader format");
		}
		
		int headerSize = (int)VByte.decode(input);
		
		byte [] headerData = IOUtil.readBuffer(input, headerSize, listener);
		
		try {
			RDFParserSimple parser = new RDFParserSimple();
			parser.doParse(new ByteArrayInputStream(headerData), RDFNotation.NTRIPLES, this);
		} catch (ParserException e) {
			e.printStackTrace();
			throw new IllegalFormatException("Error parsing header");
		}
	}

	/* (non-Javadoc)
	 * @see hdt.header.Header#getNumberOfElements()
	 */
	@Override
	public int getNumberOfElements() {
		return triples.size();
	}

	/* (non-Javadoc)
	 * @see hdt.header.Header#search(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public IteratorTripleString search(CharSequence subject, CharSequence predicate, CharSequence object) {
		TripleString pattern = new TripleString(subject, predicate, object);
		return new PlainHeaderIterator(this, pattern);
	}

	@Override
	public void processTriple(TripleString triple, long pos) {
		this.insert(triple);
	}

}
