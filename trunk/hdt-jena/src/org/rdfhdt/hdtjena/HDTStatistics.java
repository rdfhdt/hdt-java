/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/compact/array/ArrayFactory.java $
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

package org.rdfhdt.hdtjena;

import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;

import com.hp.hpl.jena.graph.GraphStatisticsHandler;
import com.hp.hpl.jena.graph.Node;


/**
 * @author mck
 *
 */
public class HDTStatistics implements GraphStatisticsHandler {

	HDT hdt;
	
	/**
	 * 
	 */
	public HDTStatistics(HDT hdt) {
		this.hdt = hdt;
	}
	
	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.GraphStatisticsHandler#getStatistic(com.hp.hpl.jena.graph.Node, com.hp.hpl.jena.graph.Node, com.hp.hpl.jena.graph.Node)
	 */
	@Override
	public long getStatistic(Node subject, Node predicate, Node object) {
		System.out.println("STATISTICS");
		int s, p, o;
		s = subject==null ? 0 : hdt.getDictionary().stringToId(subject.toString(), TripleComponentRole.SUBJECT);
		p = predicate==null ? 0 : hdt.getDictionary().stringToId(predicate.toString(), TripleComponentRole.PREDICATE);
		o = object==null ? 0 : hdt.getDictionary().stringToId(object.toString(), TripleComponentRole.OBJECT);
		
		IteratorTripleID it = hdt.getTriples().search(new TripleID(s, p, o));
		return it.estimatedNumResults();
	}

}
