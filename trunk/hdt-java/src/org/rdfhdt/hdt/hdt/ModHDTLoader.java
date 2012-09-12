/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/dictionary/Dictionary.java $
 * Revision: $Rev: 57 $
 * Last modified: $Date: 2012-08-24 01:26:52 +0100 (Fri, 24 Aug 2012) $
 * Last modified by: $Author: simpsonim13@gmail.com $
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

package org.rdfhdt.hdt.hdt;

import java.io.IOException;

import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTSpecification;

/**
 * Interface for classes that want to support loading data from
 * RDF to a modifiable HDT.
 * 
 * @author mario.arias, Eugen
 *
 */
public interface ModHDTLoader {
	
	ModifiableHDT loadFromRDF(HDTSpecification spec, String filename, String baseUri, RDFNotation notation, ProgressListener listener) throws IOException, ParserException;
	
}
