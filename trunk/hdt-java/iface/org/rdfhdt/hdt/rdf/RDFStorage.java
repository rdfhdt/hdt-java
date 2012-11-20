/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/rdf/RDFStorage.java $
 * Revision: $Rev: 71 $
 * Last modified: $Date: 2012-09-21 18:31:19 +0100 (vie, 21 sep 2012) $
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

package org.rdfhdt.hdt.rdf;

/**
 * Represent an RDFStorage that can insert and remove triples in addition of searching.
 * 
 * @author mario.arias
 *
 */
public interface RDFStorage extends RDFAccess {

	void insert(CharSequence subject, CharSequence predicate, CharSequence object);
	void insert(CharSequence subject, CharSequence predicate, long object);
		
	void remove(CharSequence subject, CharSequence predicate, CharSequence object);
}
