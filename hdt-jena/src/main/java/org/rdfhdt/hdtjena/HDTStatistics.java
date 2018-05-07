/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-jena/src/org/rdfhdt/hdtjena/HDTStatistics.java $
 * Revision: $Rev: 190 $
 * Last modified: $Date: 2013-03-03 11:30:03 +0000 (dom, 03 mar 2013) $
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
 */

package org.rdfhdt.hdtjena;

import org.apache.jena.graph.GraphStatisticsHandler;
import org.apache.jena.graph.Node;
import org.rdfhdt.hdt.compact.sequence.Sequence;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.impl.BitmapTriples;


/**
 * @author mario.arias
 *
 */
public class HDTStatistics implements GraphStatisticsHandler {

	private final NodeDictionary nodeDictionary;
	private final HDT hdt;
	
	/**
	 * 
	 */
	public HDTStatistics(HDTGraph graph) {
		this.nodeDictionary = graph.getNodeDictionary();
		this.hdt = graph.getHDT();
	}
	
	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.GraphStatisticsHandler#getStatistic(com.hp.hpl.jena.graph.Node, com.hp.hpl.jena.graph.Node, com.hp.hpl.jena.graph.Node)
	 */
	@Override
	public long getStatistic(Node subject, Node predicate, Node object) {
		try {
			final BitmapTriples triples = (BitmapTriples) hdt.getTriples();
			
			if(subject.isVariable() && predicate.isVariable() && object.isVariable()) {
				return triples.getNumberOfElements();
			}


			int s, p, o;
			s = nodeDictionary.getIntID(subject, TripleComponentRole.SUBJECT);
			p = nodeDictionary.getIntID(predicate, TripleComponentRole.PREDICATE);
			o = nodeDictionary.getIntID(object, TripleComponentRole.OBJECT);

			if(s<0||p<0||o<0) {
				// Not in dataset
				return 0;
			}

			// FIMXE: No index on ?P?, avoid creating the iterator. Dirty hack.
			if(p>0 && s==0 && o==0 && (triples instanceof BitmapTriples)) {
				Sequence predCount = ((BitmapTriples)triples).getPredicateCount();
				if(predCount!=null) {
					return predCount.get(p-1);
				} else {
					// We don't know, rough estimation.
					long pred = hdt.getDictionary().getNpredicates();
					if(pred>0) {
						return triples.getNumberOfElements()/pred;
					} else {
						return triples.getNumberOfElements();
					}
				}	
			}
			
			if(s==0 && o!=0 && triples.getIndexZ()==null) {
				return triples.getNumberOfElements();
			}

			IteratorTripleID it = triples.search(new TripleID(s, p, o));
			return it.estimatedNumResults();
		} catch (Exception e) {
			// Something went wrong. Worst case estimation instead of crashing.
			return Long.MAX_VALUE;
		}
	}
}
