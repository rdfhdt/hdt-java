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
	public static final String MOD_TRIPLES_IMPL_LIST = "list";
	public static final String MOD_TRIPLES_IMPL_SET = "set";
	public static final String MOD_TRIPLES_IMPL_JDBM = "jdbm";
	public static final String MOD_TRIPLES_IMPL_BERKELEY = "berkeleyJE";
	public static final String MOD_TRIPLES_IMPL_BERKELEY_NATIVE = "berkeley";
	public static final String MOD_TRIPLES_IMPL_KYOTO = "kyoto";

//	private static void requireTwoPass(HDTOptions spec) {
//		String loaderType = spec.get("loader.type");
//		if (!HDTFactory.LOADER_TWO_PASS.equals(loaderType)){
//			String errmsg = "tempTriples.type cannot be \"on-disk\" if loader.type is not set to two-pass!";
//			System.err.println(errmsg);
//			throw new RuntimeException(errmsg);
//		}
//	}
	
	/**
	 * Creates a new TempTriples (writable triples structure)
	 * 
	 * @return TempTriples
	 */
	static public TempTriples createTempTriples(HDTOptions spec) {
		
		String triplesImpl = spec.get("tempTriples.impl");

		// FIXME: Load Disk Implementations
//		if (MOD_TRIPLES_IMPL_JDBM.equals(triplesImpl)) {
//			requireTwoPass(spec);
//			return new TriplesJDBM(spec);
//		} else if (MOD_TRIPLES_IMPL_BERKELEY.equals(triplesImpl)) {
//			requireTwoPass(spec);
//			return new TriplesBerkeley(spec);
//		} else if (MOD_TRIPLES_IMPL_BERKELEY_NATIVE.equals(triplesImpl)) {
//			requireTwoPass(spec);
//			//return new TriplesBerkeleyNative(spec);
//			throw new NotImplementedException();
//		} else if (MOD_TRIPLES_IMPL_KYOTO.equals(triplesImpl)) {
//			requireTwoPass(spec);
//			return new TriplesKyoto(spec);
//		} else {
			spec.set("tempTriples.impl", MOD_TRIPLES_IMPL_LIST);
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
