/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/triples/TriplesFactory.java $
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

package org.rdfhdt.hdt.triples;

import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.options.ControlInfo;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.impl.BitmapTriples;
import org.rdfhdt.hdt.triples.impl.TriplesList;

/**
 * Factory that creates Triples objects
 * 
 */
public class TriplesFactory {
		
	public static final String TEMP_TRIPLES_IMPL_LIST = "list";

	private TriplesFactory() {}
	
	/**
	 * Creates a new TempTriples (writable triples structure)
	 * 
	 * @return TempTriples
	 */
	static public TempTriples createTempTriples(HDTOptions spec) {		
//		String triplesImpl = spec.get("tempTriples.impl");

		// Implementations available in the Core
//		if (triplesImpl==null || triplesImpl.equals("") || TEMP_TRIPLES_IMPL_LIST.equals(triplesImpl)) {
			return new TriplesList(spec);
//		}
	}
	
	/**
	 * Creates a new Triples based on an HDTOptions
	 * 
	 * @param specification
	 *            The HDTOptions to read
	 * @return Triples
	 */
	static public TriplesPrivate createTriples(HDTOptions spec) {
		String type = spec.get("triples.format");
		
		if(type==null) {
			return new BitmapTriples(spec);
		} else if(HDTVocabulary.TRIPLES_TYPE_TRIPLESLIST.equals(type)) {
			return new TriplesList(spec);
		} else if(HDTVocabulary.TRIPLES_TYPE_BITMAP.equals(type)) {
			return new BitmapTriples(spec);
		} else {
			return new BitmapTriples(spec);
		}
	}
	
	/**
	 * Creates a new Triples based on a ControlInformation
	 * 
	 * @param specification
	 *            The HDTOptions to read
	 * @return Triples
	 */
	public static TriplesPrivate createTriples(ControlInfo ci) {
		String format = ci.getFormat();
		
		if(HDTVocabulary.TRIPLES_TYPE_TRIPLESLIST.equals(format)) {
			return new TriplesList(new HDTSpecification());
		} else if(HDTVocabulary.TRIPLES_TYPE_BITMAP.equals(format)) {
			return new BitmapTriples();
		} else {
			throw new IllegalArgumentException("No implementation for Triples type: "+format);
		}
	}

}
