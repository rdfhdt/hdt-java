/**
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
/**
 * 
 */
package hdt.triples;

import hdt.hdt.HDTVocabulary;
import hdt.options.ControlInformation;
import hdt.options.HDTSpecification;
import hdt.triples.impl.BitmapTriples;
import hdt.triples.impl.PlainTriples;
import hdt.triples.impl.TriplesList;

/**
 * Factory that creates Triples objects
 * 
 */
public class TriplesFactory {

	/**
	 * Creates a new ModifiableTriples (writable triples structure)
	 * 
	 * @return ModifiableTriples
	 */
	static public ModifiableTriples createModifiableTriples() {
		return new TriplesList();
	}

	/**
	 * Creates a new Triples based on an HDTSpecification
	 * 
	 * @param specification
	 *            The HDTSpecification to read
	 * @return Triples
	 */
	static public Triples createTriples(HDTSpecification spec) {
		return create(spec.get("triples.type"));
	}
	
	/**
	 * Creates a new Triples based on a ControlInformation
	 * 
	 * @param specification
	 *            The HDTSpecification to read
	 * @return Triples
	 */
	public static Triples createTriples(ControlInformation ci) {
		return create(ci.get("triples.type"));
	}
	
	private static Triples create(String type) {
		if(type==null) {
			return new BitmapTriples();
		} else if(HDTVocabulary.TRIPLES_TYPE_TRIPLESLIST.equals(type)) {
			return new TriplesList();
		} else if(HDTVocabulary.TRIPLES_TYPE_PLAIN.equals(type)) {
			return new PlainTriples();
		} else if(HDTVocabulary.TRIPLES_TYPE_BITMAP.equals(type)) {
			return new BitmapTriples();
		} else {
			return new BitmapTriples();
		}
	}

}
