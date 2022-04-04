/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/rdf/RDFParserFactory.java $
 * Revision: $Rev: 191 $
 * Last modified: $Date: 2013-03-03 11:41:43 +0000 (dom, 03 mar 2013) $
 * Last modified by: $Author: mario.arias $
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 3.0 of the License.
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

package org.rdfhdt.hdt.rdf;

import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.iterator.utils.PipedCopyIterator;
import org.rdfhdt.hdt.rdf.parsers.RDFParserDir;
import org.rdfhdt.hdt.rdf.parsers.RDFParserHDT;
import org.rdfhdt.hdt.rdf.parsers.RDFParserList;
import org.rdfhdt.hdt.rdf.parsers.RDFParserRAR;
import org.rdfhdt.hdt.rdf.parsers.RDFParserRIOT;
import org.rdfhdt.hdt.rdf.parsers.RDFParserSimple;
import org.rdfhdt.hdt.rdf.parsers.RDFParserTar;
import org.rdfhdt.hdt.rdf.parsers.RDFParserZip;
import org.rdfhdt.hdt.triples.TripleString;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * @author mario.arias
 *
 */
public class RDFParserFactory {
	public static RDFParserCallback getParserCallback(RDFNotation notation) {
		return getParserCallback(notation, false);
	}
	public static RDFParserCallback getParserCallback(RDFNotation notation, boolean useSimple) {
		switch(notation) {
			case NTRIPLES:
				if (useSimple) {
					return new RDFParserSimple();
				}
			case NQUAD:
			case TURTLE:
			case N3:
			case RDFXML:
				return new RDFParserRIOT();
			case DIR:
				return new RDFParserDir(useSimple);
			case LIST:
				return new RDFParserList();
			case ZIP:
				return new RDFParserZip(useSimple);
			case TAR:
				return new RDFParserTar(useSimple);
			case RAR:
				return new RDFParserRAR(useSimple);
			case HDT:
				return new RDFParserHDT();
			case JSONLD:
				// FIXME: Implement
				throw new NotImplementedException("RDFParserJSONLD not implemented");
			default:
				break;
		}

		throw new NotImplementedException("Parser not found for notation: "+notation);		
	}

	/**
	 * convert a stream to a triple iterator
	 * @param parser the parser to convert the stream
	 * @param stream the stream to parse
	 * @param baseUri the base uri to parse
	 * @param notation the rdf notation to parse
	 * @return iterator
	 */
	public static Iterator<TripleString> readAsIterator(RDFParserCallback parser, InputStream stream, String baseUri, boolean keepBNode, RDFNotation notation) {
		return PipedCopyIterator.createOfCallback(TripleStringParser.INSTANCE, pipe -> parser.doParse(stream, baseUri, notation, keepBNode, (triple, pos) -> pipe.addElement(triple)));
	}

	private static class TripleStringParser implements PipedCopyIterator.Parser<TripleString> {
		private static final TripleStringParser INSTANCE = new TripleStringParser();
		@Override
		public void write(TripleString tripleString, OutputStream stream) throws IOException {
			PipedCopyIterator.Parser.writeString(tripleString.getSubject(), stream);
			PipedCopyIterator.Parser.writeString(tripleString.getPredicate(), stream);
			PipedCopyIterator.Parser.writeString(tripleString.getObject(), stream);
		}

		@Override
		public TripleString read(InputStream stream) throws IOException {
			String s = PipedCopyIterator.Parser.readString(stream);
			String p = PipedCopyIterator.Parser.readString(stream);
			String o = PipedCopyIterator.Parser.readString(stream);
			return new TripleString(s, p, o);
		}
	}
}
