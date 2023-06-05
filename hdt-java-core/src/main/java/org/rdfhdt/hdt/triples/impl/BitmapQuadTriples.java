/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/triples/impl/BitmapTriples.java $
 * Revision: $Rev: 203 $
 * Last modified: $Date: 2013-05-24 10:48:53 +0100 (vie, 24 may 2013) $
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

package org.rdfhdt.hdt.triples.impl;

import org.rdfhdt.hdt.compact.bitmap.*;
import org.rdfhdt.hdt.compact.sequence.Sequence;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.options.*;

import java.io.*;

/**
 * @author mario.arias
 *
 */
public class BitmapQuadTriples extends BitmapTriples {
	public BitmapQuadTriples() throws IOException {
		super();
	}
	
	public BitmapQuadTriples(HDTOptions spec) throws IOException {
		super(spec);
		System.out.println("CREATE BITMAP QUAD TRIPLES");
	}

	public BitmapQuadTriples(HDTOptions spec, Sequence seqY, Sequence seqZ, Bitmap bitY, Bitmap bitZ, TripleComponentOrder order) throws IOException {
		super(spec, seqY, seqZ, bitY, bitZ, order);
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#getType()
	 */
	@Override
	public String getType() {
		return HDTVocabulary.TRIPLES_TYPE_BITMAP_QUAD;
	}
	// load search save getNumberOfElements, size, mapFromFiles
}

