/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-jena/src/org/rdfhdt/hdtjena/solver/HDTSolverLib.java $
 * Revision: $Rev: 197 $
 * Last modified: $Date: 2013-04-12 19:39:33 +0100 (vie, 12 abr 2013) $
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.atlas.lib.tuple.Tuple;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdtjena.HDTGraph;
import org.rdfhdt.hdtjena.NodeDictionary;
import org.rdfhdt.hdtjena.bindings.BindingHDTId;
import org.rdfhdt.hdtjena.bindings.BindingHDTNode;
import org.rdfhdt.hdtjena.bindings.HDTId;
import org.rdfhdt.hdtjena.util.Abortable;
import org.rdfhdt.hdtjena.util.IterAbortable;
import org.rdfhdt.hdtjena.util.VarAppearance;

/** Utilities used within the HDT BGP solver : local HDT store */
public class HDTSolverLib
{
	public static long numBGPs;
	
    protected static QueryIterator execute(HDTGraph graph, BasicPattern pattern, QueryIterator input,
    										Predicate<Tuple<HDTId>> filter, ExecutionContext execCxt)
    {
    	numBGPs++;
    	
        Iterator<BindingHDTId> chain = Iter.map(input, HDTSolverLib.convFromBinding(graph.getNodeDictionary(), execCxt)) ;
        
        List<Abortable> killList = new ArrayList<>() ;
        
        // Compute appearances of variables
        Map<Var, VarAppearance> mapVar = new HashMap<>();
        for ( Triple triplePattern : pattern )
        {
        	addVarAppearance(mapVar, triplePattern.getSubject(), TripleComponentRole.SUBJECT);
//        	addVarAppearance(mapVar, triplePattern.getPredicate(), TripleComponentRole.PREDICATE);
        	addVarAppearance(mapVar, triplePattern.getObject(), TripleComponentRole.OBJECT);
        }

        for ( Triple triplePattern : pattern )
        {
            chain = solve(graph, triplePattern, chain, filter, mapVar, execCxt) ;
            chain = IterAbortable.makeAbortable(chain, killList) ; 
        }
        
        // Need to make sure the bindings here point to parent.
        Iterator<Binding> iterBinding = Iter.map(chain, HDTSolverLib::convToBinding) ;
        
        // "input" will be closed by QueryIterHDT but is otherwise unused.
        // "killList" will be aborted on timeout.
        return new QueryIterHDT(iterBinding, killList, input, execCxt) ;
    }
    

     private static void addVarAppearance(Map<Var, VarAppearance> mapVar, Node node, TripleComponentRole role) {
    	 if(node.isVariable()) {
    		 Var v = NodeDictionary.asVar(node);
    		 VarAppearance varAp = mapVar.get(v);
    		 if(varAp==null) {
    			 varAp = new VarAppearance();
    			 mapVar.put(v, varAp);
    		 }
    		 varAp.set(role);
    	 }
     }


    
    private static Iterator<BindingHDTId> solve(HDTGraph graph, Triple tuple, Iterator<BindingHDTId> chain, 
    											Predicate<Tuple<HDTId>> filter, Map<Var, VarAppearance> mapVar,
                                                 ExecutionContext execCxt)
    {
        return new StageMatchTripleID(graph, chain, tuple, execCxt, mapVar) ;
    }
    

    
	private static HDTId getID(Node n, NodeDictionary dict, PrefixMapping prefixMapping) {
		int id = dict.getIntID(n, prefixMapping, TripleComponentRole.SUBJECT);
		if(id>0) {
			return new HDTId(id, TripleComponentRole.SUBJECT, dict);
		}
		
		id = dict.getIntID(n, prefixMapping, TripleComponentRole.PREDICATE);
		if(id>0) {
			return new HDTId(id, TripleComponentRole.PREDICATE, dict);
		}
		
		id = dict.getIntID(n, prefixMapping, TripleComponentRole.OBJECT);
		if(id>0) {
			return new HDTId(id, TripleComponentRole.OBJECT, dict);
		}
		return new HDTId(id, null, dict); // NOT FOUND
	}
	
	// Conversions
	
    /** Transform from HDTId bindings to generic Jena bindings. */
    public static Binding convToBinding(BindingHDTId binding) {
        return new BindingHDTNode(binding);
    }
    
    // Transform : BindingHDTId ==> Binding
    private static Function<BindingHDTId, Binding> convToBinding(final NodeDictionary dictionary)
    {
        return new Function<BindingHDTId, Binding>()
        {
            @Override
            public Binding apply(BindingHDTId bindingIds)
            {
                return new BindingHDTNode(bindingIds);
            }
        } ;
    }
    
    public static Iterator<BindingHDTId> convertToIds(Iterator<Binding> iterBindings, NodeDictionary dictionary, ExecutionContext ctx)
    { 
    	return Iter.map(iterBindings, convFromBinding(dictionary, ctx)) ; 
    }
    
    public static Iterator<Binding> convertToNodes(Iterator<BindingHDTId> iterBindingIds, NodeDictionary dictionary)
    {
    	return Iter.map(iterBindingIds, convToBinding(dictionary)) ; 
    }
    
    // Transform : Binding ==> BindingHDTId
    public static Function<Binding, BindingHDTId> convFromBinding(final NodeDictionary dictionary, final ExecutionContext ctx)
    {
        return new Function<Binding, BindingHDTId>()
        {
        	PrefixMapping mapping = NodeDictionary.getMapping(ctx);
        	
            @Override
            public BindingHDTId apply(Binding binding)
            {
                if ( binding instanceof BindingHDTNode )
                    return ((BindingHDTNode)binding).getBindingId() ;
                
                BindingHDTId b = new BindingHDTId(binding) ;
                // and copy over, getting HDTIds.
                Iterator<Var> vars = binding.vars() ;
    
                for ( ; vars.hasNext() ; )
                {
                    Var v = vars.next() ;
                    Node n = binding.get(v) ;  
                    if ( n == null )
                        // Variable mentioned in the binding but not actually defined.
                        // Can occur with BindingProject
                        continue ;
                    
                    HDTId id = getID(n, dictionary, mapping);
                    // Even put "does not exist" if it is null.
                    b.put(v, id) ;
                }
                return b ;
            }
        };
    }
}
