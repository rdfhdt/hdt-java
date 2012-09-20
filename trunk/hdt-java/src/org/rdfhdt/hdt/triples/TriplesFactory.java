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

import org.rdfhdt.hdt.hdt.HDTFactory;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.impl.BitmapTriples;
import org.rdfhdt.hdt.triples.impl.TriplesBerkeley;
import org.rdfhdt.hdt.triples.impl.TriplesJDBM;
import org.rdfhdt.hdt.triples.impl.TriplesList;
import org.rdfhdt.hdt.triples.impl.TriplesSet;

/**
 * Factory that creates Triples objects
 * 
 */
public class TriplesFactory {
	
	public static final String MOD_TRIPLES_TYPE_IN_MEM = "in-memory";
	public static final String MOD_TRIPLES_TYPE_ON_DISC = "on-disc";
	
	public static final String MOD_TRIPLES_IMPL_LIST = "list";
	public static final String MOD_TRIPLES_IMPL_SET = "set";
	public static final String MOD_TRIPLES_IMPL_JDBM = "jdbm";
	public static final String MOD_TRIPLES_IMPL_BERKELEY = "berkeleyJE";
	public static final String MOD_TRIPLES_IMPL_BERKELEY_NATIVE = "berkeley";
	public static final String MOD_TRIPLES_IMPL_KYOTO = "kyoto";

	/**
	 * Creates a new ModifiableTriples (writable triples structure)
	 * 
	 * @return ModifiableTriples
	 */
	static public ModifiableTriples createModifiableTriples(HDTSpecification spec) {
		
		String triplesType = spec.get("tempTriples.type");
		String triplesImpl = spec.get("tempTriples.impl");
		String loaderType = spec.get("loader.type");
		
		//TODO switch-case can use String in 1.7 and after...
		if (MOD_TRIPLES_TYPE_IN_MEM.equals(triplesType)){
			if (MOD_TRIPLES_IMPL_LIST.equals(triplesImpl)){
				return new TriplesList(spec);
			} else if (MOD_TRIPLES_IMPL_SET.equals(triplesImpl)){
				if (!HDTFactory.LOADER_TWO_PASS.equals(loaderType)){
					String errmsg = "tempTriples.impl cannot be \"set\" if loader.type is not set to two-pass!";
					System.err.println(errmsg);
					throw new RuntimeException(errmsg);
				}
				return new TriplesSet(spec);
			} else {
				System.err.println("Unknown in-memory triples implementation, using list.");
				spec.set("tempTriples.impl", MOD_TRIPLES_IMPL_LIST);
				return new TriplesList(spec);
			}
		} else if (MOD_TRIPLES_TYPE_ON_DISC.equals(triplesType)) {
			if (!HDTFactory.LOADER_TWO_PASS.equals(loaderType)){
				String errmsg = "tempTriples.type cannot be \"on-disc\" if loader.type is not set to two-pass!";
				System.err.println(errmsg);
				throw new RuntimeException(errmsg);
			}
			if (MOD_TRIPLES_IMPL_JDBM.equals(triplesImpl)) {
				return new TriplesJDBM(spec);
			} else if (MOD_TRIPLES_IMPL_BERKELEY.equals(triplesImpl)) {
				return new TriplesBerkeley(spec);
			} /*else if (MOD_TRIPLES_IMPL_BERKELEY_NATIVE.equals(triplesImpl)) {
				return new TriplesBerkeleyNative(spec);
			} else if (MOD_TRIPLES_IMPL_KYOTO.equals(triplesImpl)) {
				return new TriplesKyoto(spec);
			} */else {
				System.err.println("Unknown on-disc triples implementation, using jdbm.");
				spec.set("tempTriples.impl", MOD_TRIPLES_IMPL_JDBM);
				return new TriplesJDBM(spec);
			} 
		} else {
			System.err.println("Unknown triples type, using in-memory list.");
			spec.set("tempTriples.type", MOD_TRIPLES_TYPE_IN_MEM);
			spec.set("tempTriples.impl", MOD_TRIPLES_IMPL_LIST);
			return new TriplesList(spec);
		}
	}
	
	/**
	 * Creates a new Triples based on an HDTSpecification
	 * 
	 * @param specification
	 *            The HDTSpecification to read
	 * @return Triples
	 */
	static public Triples createTriples(HDTSpecification spec) {
		String type = spec.get("codification");
		
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
	 *            The HDTSpecification to read
	 * @return Triples
	 */
	public static Triples createTriples(ControlInformation ci) {
		String type = ci.get("codification");
		
		if(HDTVocabulary.TRIPLES_TYPE_TRIPLESLIST.equals(type)) {
			return new TriplesList(new HDTSpecification());
		} else if(HDTVocabulary.TRIPLES_TYPE_BITMAP.equals(type)) {
			return new BitmapTriples();
		} else {
			throw new IllegalArgumentException("No implementation for Triples type: "+type);
		}
	}

}
