/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/dictionary/DictionarySection.java $
 * Revision: $Rev: 35 $
 * Last modified: $Date: 2012-07-24 22:16:23 +0100 (Tue, 24 Jul 2012) $
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

package org.rdfhdt.hdt.dictionary;


/**
 * Interface that defines a dictionary section that can be queried for an ID of an entry or for an
 * entry by its ID. This kind of dictionary section can also be saved to or loaded from a stream.
 * 
 * @author Eugen
 *
 */
public interface QueryableDictionarySection extends DictionarySection {
	
	/**
	 * Find the String associated to an ID
	 * @param pos
	 * @return
	 */
	public CharSequence extract(int pos);
	
}
