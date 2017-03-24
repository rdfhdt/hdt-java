/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-jena/src/org/rdfhdt/hdtjena/HDTGraph.java $
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

import java.io.IOException;

import org.apache.jena.graph.Capabilities;
import org.apache.jena.graph.GraphStatisticsHandler;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.GraphBase;
import org.apache.jena.query.ARQ;
import org.apache.jena.sparql.engine.main.QC;
import org.apache.jena.sparql.engine.optimizer.reorder.ReorderTransformation;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdtjena.solver.HDTJenaIterator;
import org.rdfhdt.hdtjena.solver.HDTQueryEngine;
import org.rdfhdt.hdtjena.solver.OpExecutorHDT;
import org.rdfhdt.hdtjena.solver.ReorderTransformationHDT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author mario.arias
 *
 */
public class HDTGraph extends GraphBase {
	private static final Logger log = LoggerFactory.getLogger(HDTGraph.class);

	private static final HDTCapabilities capabilities= new HDTCapabilities();

	private HDT hdt;
	private NodeDictionary nodeDictionary;
	private ReorderTransformation reorderTransform;
	private HDTStatistics hdtStatistics;
	private long numSearches;
	private boolean closeAfter;
	
	static {
		// Register OpExecutor
		QC.setFactory(ARQ.getContext(), OpExecutorHDT.opExecFactoryHDT);
		HDTQueryEngine.register();
	}
	
	public HDTGraph(HDT hdt) {
		this(hdt, false);
	}
	
	public HDTGraph(HDT hdt, boolean close) {
		this.hdt = hdt;
		this.nodeDictionary = new NodeDictionary(hdt.getDictionary());
		this.hdtStatistics = new HDTStatistics(this);	// Must go after NodeDictionary created.
		this.reorderTransform=new ReorderTransformationHDT(this);  // Must go after Dict and Stats
		this.closeAfter = close;
	}
	
	public HDT getHDT() {
		return hdt;
	}
	
	public NodeDictionary getNodeDictionary() {
		return nodeDictionary;
	}
	
	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.impl.GraphBase#graphBaseFind(com.hp.hpl.jena.graph.TripleMatch)
	 */
	@Override
	protected ExtendedIterator<Triple> graphBaseFind(Triple jenaTriple) {

		TripleID triplePatID = nodeDictionary.getTriplePatID(jenaTriple);
//		System.out.println("Triple Pattern: "+jenaTriple+" as IDs: "+triplePatID);
		
		IteratorTripleID hdtIterator = hdt.getTriples().search( triplePatID );
		numSearches++;
		return new HDTJenaIterator(nodeDictionary, hdtIterator);
	}
	
	public long getNumSearches() {
		return numSearches;
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.impl.GraphBase#getStatisticsHandler()
	 */
	@Override
	public GraphStatisticsHandler getStatisticsHandler() {
		return hdtStatistics;
	}
	
	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.impl.GraphBase#getCapabilities()
	 */
	@Override
	public Capabilities getCapabilities() {
		return HDTGraph.capabilities;
	}

	public ReorderTransformation getReorderTransform() {
		return reorderTransform;
	}
	
	@Override
	protected int graphBaseSize() {
		return (int)hdt.getTriples().getNumberOfElements();
	}
	
	@Override
	public void close() {
		super.close();
		
		if(closeAfter) {
			try {
				hdt.close();
			} catch (IOException e) {
				log.error("Unexpected exception.", e);
			}
		}
	}
}
