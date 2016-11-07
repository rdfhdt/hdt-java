/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-jena/src/org/rdfhdt/hdtjena/solver/ReorderTransformationHDT.java $
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

package org.rdfhdt.hdtjena.solver;


import static org.apache.jena.sparql.engine.optimizer.reorder.PatternElements.TERM;
import static org.apache.jena.sparql.engine.optimizer.reorder.PatternElements.VAR;

import org.apache.jena.graph.GraphStatisticsHandler;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.engine.optimizer.Pattern;
import org.apache.jena.sparql.engine.optimizer.StatsMatcher;
import org.apache.jena.sparql.engine.optimizer.reorder.PatternTriple;
import org.apache.jena.sparql.engine.optimizer.reorder.ReorderTransformationSubstitution;
import org.apache.jena.sparql.graph.NodeConst;
import org.apache.jena.sparql.sse.Item;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdtjena.HDTGraph;

/**
 * Reorders the Triple Patterns of a BGP by using statistics directly fetched from
 * the dataset. At query optimization phase, when planning index-joins, some variables
 * are known to be bound at some stage, but the actual values are unknown.
 * In this case it uses predefined typical behaviour for RDF, using independent 
 * histograms for S/P/O, inspired by Jena's FixedReorder.
 * 
 * @author mario.arias
 *
 */
public class ReorderTransformationHDT extends ReorderTransformationSubstitution {
	
    /** Maximum value for a match involving two terms. */
    public static final int MultiTermMax = 100;

    /** The number of triples used for the base scale */
    public static final int MultiTermSampleSize = 10000 ; 
    
	final long TERM_S ;         // Used for S ? ? if no stats
	final long TERM_P ;         // Used for ? P ? if no stats
	final long TERM_O ;         // Used for ? ? O if no stats
	final long numTriples ;		// Actual number of triples of the dataset.

	private final GraphStatisticsHandler stats;
    public final StatsMatcher matcher = new StatsMatcher() ;
    

	public ReorderTransformationHDT(HDTGraph graph)
	{
		this.stats = graph.getStatisticsHandler();
		numTriples = graph.size();
	
		initializeMatcher();
				
		// FIXME: Compute exactly for using the HDT  
		Dictionary dict = graph.getHDT().getDictionary();
		TERM_S = dict.getNsubjects()/Math.max(numTriples, 1);
		TERM_P = dict.getNpredicates()/Math.max(numTriples, 1);
		TERM_O = dict.getNobjects()/Math.max(numTriples, 1);
	}

    private void initializeMatcher () {
    	Item type = Item.createNode(NodeConst.nodeRDFType);
        
        //matcher.addPattern(new Pattern(1,   TERM, TERM, TERM)) ;     // SPO - built-in - not needed as a rule
        
        // Numbers chosen as an approximation for a graph of 10K triples
        matcher.addPattern(new Pattern(5,	TERM, TERM, VAR)) ;     // SP?
        matcher.addPattern(new Pattern(1000,VAR, type, TERM)) ;    // ? type O -- worse than ?PO
        matcher.addPattern(new Pattern(90,	VAR,  TERM, TERM)) ;    // ?PO
        matcher.addPattern(new Pattern(5,	TERM, VAR, TERM)) ;    // S?O
        
        matcher.addPattern(new Pattern(40,	TERM, VAR,  VAR)) ;     // S??
        matcher.addPattern(new Pattern(200,	VAR,  VAR,  TERM)) ;    // ??O
        matcher.addPattern(new Pattern(2000,VAR,  TERM, VAR)) ;     // ?P?

        matcher.addPattern(new Pattern(MultiTermSampleSize, VAR,  VAR,  VAR)) ;     // ???
    }
	
	@Override
	protected double weight(PatternTriple pt)
	{	
		// If all are nodes, there are no substitutions. We can get the exact number.
		if(pt.subject.isNode() && pt.predicate.isNode() && pt.object.isNode()) {
			return stats.getStatistic(pt.subject.getNode(), pt.predicate.getNode(), pt.object.getNode());
		}

		// Try on fixed
		double x = matcher.match(pt);
		
		// If there are two fixed terms, use the fixed weighting, all of which are quite small.
		// This chooses a less optimal triple but the worse choice is still a very selective choice.
		// One case is IFPs: the multi term choice for PO is not 1. 
		if ( x < MultiTermMax )
		{
			return x;
		}
		
		// One or zero fixed terms.
		// Otherwise, assuming S / P / O independent, do an estimation.
		
		long S = -1 ;
		long P = -1 ;
		long O = -1 ;

		// Include guesses for SP, OP, typeClass
		if ( pt.subject.isNode() && !pt.subject.isVar()) {
			S = stats.getStatistic(pt.subject.getNode(), Node.ANY, Node.ANY) ;
		} else if ( TERM.equals(pt.subject) ) {
			S = TERM_S ;
		}

		// rdf:type.
		if ( pt.predicate.isNode() && !pt.predicate.isVar())
			P = stats.getStatistic(Node.ANY, pt.predicate.getNode(), Node.ANY) ;
		else if ( TERM.equals(pt.predicate) ) {
			P = TERM_P ;
		}

		if ( pt.object.isNode() && !pt.object.isVar())
			O = stats.getStatistic(Node.ANY, Node.ANY, pt.object.getNode()) ;
		else if ( TERM.equals(pt.object) ) {
			O = TERM_O ;
		}

		if ( S == 0 || P == 0 || O == 0 ) {
			// Can't match.
			return 0 ;
		}

		// Find min positive
		x = -1 ;
		if ( S > 0 ) x = S ;
		if ( P > 0 && P < x ) x = P ;
		if ( O > 0 && O < x ) x = O ;
		//System.out.printf("** [%d, %d, %d]\n", S, P ,O) ;

		return x;
	}
}
