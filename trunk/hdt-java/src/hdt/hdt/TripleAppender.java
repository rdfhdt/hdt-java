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
package hdt.hdt;

import hdt.dictionary.ModifiableDictionary;
import hdt.enums.TripleComponentRole;
import hdt.rdf.RDFParserCallback.RDFCallback;
import hdt.triples.ModifiableTriples;
import hdt.triples.TripleID;
import hdt.triples.TripleString;

class TripleAppender implements RDFCallback {
	ModifiableDictionary dict;
	ModifiableTriples triples;
	TripleID tripleID;

	public TripleAppender(ModifiableDictionary dict, ModifiableTriples triples) {
		this.dict = dict;
		this.triples = triples;
		tripleID = new TripleID();
	}

	public void processTriple(TripleString triple, long pos) {
		tripleID.setAll(
				dict.insert(triple.getSubject(), TripleComponentRole.SUBJECT),
				dict.insert(triple.getPredicate(), TripleComponentRole.PREDICATE),
				dict.insert(triple.getObject(), TripleComponentRole.OBJECT)
		);

		triples.insert(tripleID);
	}
}