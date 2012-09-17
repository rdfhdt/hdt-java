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

import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.hdt.HDT;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.query.BindingQueryPlan;
import com.hp.hpl.jena.graph.query.ExpressionSet;
import com.hp.hpl.jena.graph.query.GraphQuery;
import com.hp.hpl.jena.graph.query.Mapping;
import com.hp.hpl.jena.graph.query.PatternStage;
import com.hp.hpl.jena.graph.query.QueryHandler;
import com.hp.hpl.jena.graph.query.SimpleTreeQueryPlan;
import com.hp.hpl.jena.graph.query.Stage;
import com.hp.hpl.jena.graph.query.TreeQueryPlan;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * @author mck
 *
 */
public class HDTQueryHandler implements QueryHandler {

	HDTGraph graph;
	HDT hdt;

	/**
	 * 
	 */
	public HDTQueryHandler(HDTGraph graph, HDT hdt) {
		this.graph = graph;
		this.hdt = hdt;
	}

	@Override
	public Stage patternStage( Mapping map, ExpressionSet constraints, Triple [] t )
	{ return new PatternStage( graph, map, constraints, t ); }

	@Override
	public TreeQueryPlan prepareTree( Graph pattern )
	{ return new SimpleTreeQueryPlan( graph, pattern ); }

	@Override
	public ExtendedIterator<Node> objectsFor( Node s, Node p )
	{ 
		throw new NotImplementedException();
	}

	@Override
	public ExtendedIterator<Node> subjectsFor( Node p, Node o )
	{ 	
		throw new NotImplementedException();
	}

	@Override
	public ExtendedIterator<Node> predicatesFor( Node s, Node o )
	{ 
		throw new NotImplementedException();
	}

	 @Override
	 public boolean containsNode( Node n )
	{
		return 
		graph.contains( n, Node.ANY, Node.ANY )
		|| graph.contains( Node.ANY, n, Node.ANY )
		|| graph.contains( Node.ANY, Node.ANY, n )
		;
	}

	@Override
	public BindingQueryPlan prepareBindings(GraphQuery arg0, Node[] arg1) {
		// TODO Auto-generated method stub
		return null;
	}

}