/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/enums/RDFNotation.java $
 * Revision: $Rev: 17 $
 * Last modified: $Date: 2012-07-03 21:43:15 +0100 (mar, 03 jul 2012) $
 * Last modified by: $Author: mario.arias@gmail.com $
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

package org.rdfhdt.hdt.enums;

import java.io.File;

/**
 * Enumeration of the different valid notations for RDF data.
 * 
 * It is used for read/write operations by Jena.
 */
public enum RDFNotation {
	/**
	 * XML notation
	 * 
	 * @see <a href="http://en.wikipedia.org/wiki/RDF/XML">Wikipedia</a>
	 */
	RDFXML,
	/**
	 * N-TRIPLES notation
	 * 
	 * @see <a href="http://en.wikipedia.org/wiki/N-Triples">Wikipedia</a>
	 */
	NTRIPLES,
	/**
	 * TURTLE notation
	 * 
	 * @see <a href="http://en.wikipedia.org/wiki/Turtle_(syntax)">Wikipedia</a>
	 */
	TURTLE,
	/**
	 * Notation 3 notation
	 * 
	 * @see <a href="http://en.wikipedia.org/wiki/Notation_3">Wikipedia</a>
	 */
	N3;
	
	public static RDFNotation parse(String str) {
		if(str==null || str.isEmpty()) {
			return NTRIPLES;
		}
		str = str.toLowerCase();
		if(str.equals("ntriples")||str.equals("nt")) {
			return NTRIPLES;
		} else if(str.equals("n3")) {
			return N3;
		} else if(str.equals("rdfxml")||str.equals("rdf-xml")) {
			return RDFXML;
		} else if(str.equals("turtle")) {
			return TURTLE;
		}
		throw new IllegalArgumentException();
	}
	
	public static RDFNotation guess(File fileName) throws IllegalArgumentException {
		String str = fileName.getName().toLowerCase();
		if(str.endsWith(".gz")) {
			str = str.substring(0, str.length()-3);
		}
		if(str.endsWith(".bz2")) {
			str = str.substring(0, str.length()-4);
		}
		if(str.endsWith("nt")) {
			return NTRIPLES;
		} else if(str.endsWith("n3")) {
			return N3;
		} else if(str.endsWith("rdf")||str.endsWith("xml")||str.endsWith("owl")) {
			return RDFXML;
		} else if(str.endsWith("ttl")) {
			return TURTLE;
		}
		throw new IllegalArgumentException("Could not guess the format for "+fileName+" Using NTriples");
	}
}
