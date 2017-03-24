/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-jena/src/org/rdfhdt/hdtjena/solver/OpExecutorHDT.java $
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

import java.util.function.Predicate;

import org.apache.jena.atlas.lib.tuple.Tuple;
import org.apache.jena.atlas.logging.Log;
import org.rdfhdt.hdtjena.HDTGraph;
import org.rdfhdt.hdtjena.HDTJenaConstants;
import org.rdfhdt.hdtjena.bindings.HDTId;

import org.apache.jena.graph.Graph;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpDistinct;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpReduced;
import org.apache.jena.sparql.algebra.optimize.TransformFilterPlacement;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Substitute;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.iterator.QueryIterPeek;
import org.apache.jena.sparql.engine.main.OpExecutor;
import org.apache.jena.sparql.engine.main.OpExecutorFactory;
import org.apache.jena.sparql.engine.main.QC;
import org.apache.jena.sparql.engine.optimizer.reorder.ReorderProc;
import org.apache.jena.sparql.engine.optimizer.reorder.ReorderTransformation;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.mgt.Explain;

public class OpExecutorHDT extends OpExecutor {
	
    public final static OpExecutorFactory opExecFactoryHDT = new OpExecutorFactory()
    {
        @Override
        public OpExecutor create(ExecutionContext execCxt)
        { 
        	return new OpExecutorHDT(execCxt) ; 
        }
    };
	
	private final boolean isForHDT;

	protected OpExecutorHDT(ExecutionContext execCtx) {
		super(execCtx);
		
		isForHDT = execCtx.getActiveGraph() instanceof HDTGraph ;
	}
	
    @Override
    protected QueryIterator execute(OpDistinct opDistinct, QueryIterator input)
    {
    	// FIXME: Add to context duplicates property.
        return super.execute(opDistinct, input) ;
    }
    
    @Override
    protected QueryIterator execute(OpReduced opReduced, QueryIterator input)
    {
    	// FIXME: Add to context duplicates property.
        return super.execute(opReduced, input) ;
    }
    
    @Override
    protected QueryIterator execute(OpFilter opFilter, QueryIterator input)
    {
        if ( ! isForHDT )
            return super.execute(opFilter, input) ;
        
        // If the filter does not apply to the input??
        // Where does ARQ catch this?
        
        // (filter (bgp ...))
        if ( OpBGP.isBGP(opFilter.getSubOp()) )
        {
            // Still may be a HDT graph in a non-HDT dataset (e.g. a named model)
            HDTGraph graph = (HDTGraph)execCxt.getActiveGraph() ;
            OpBGP opBGP = (OpBGP)opFilter.getSubOp() ;
            return executeBGP(graph, opBGP, input, opFilter.getExprs(), execCxt) ;
        }
    
        // (filter (anything else))
        return super.execute(opFilter, input) ;
    }

    // ---- Triple patterns
    
    @Override
    protected QueryIterator execute(OpBGP opBGP, QueryIterator input)
    {
        if ( ! isForHDT )
            return super.execute(opBGP, input) ;
        
        HDTGraph graph = (HDTGraph)execCxt.getActiveGraph() ;
 
        return executeBGP(graph, opBGP, input, null, execCxt);
    }
    
    /** Execute a BGP (and filters) on a HDT graph, which may be in default storage or it may be a named graph */ 
    private static QueryIterator executeBGP(HDTGraph graph, OpBGP opBGP, QueryIterator input, ExprList exprs, 
                                            ExecutionContext execCxt)
    {
        // Execute a BGP on the real default graph
        return optimizeExecuteTriples(graph, input, opBGP.getPattern(), exprs, execCxt) ;
    }

	private static QueryIterator optimizeExecuteTriples(HDTGraph graph,
			QueryIterator input, BasicPattern pattern, ExprList exprs,
			ExecutionContext execCxt) {
		 if ( ! input.hasNext() )
	            return input ;
	    
	        // -- Input
	        // Must pass this iterator into the next stage.
	        if ( pattern.size() >= 2 )
	        {
	            // Must be 2 or triples to reorder. 
	            ReorderTransformation transform = graph.getReorderTransform() ;
	            if ( transform != null )
	            {
	                QueryIterPeek peek = QueryIterPeek.create(input, execCxt) ;
	                input = peek ; // Must pass on
	                pattern = reorder(pattern, peek, transform) ;
	            }
	        }
	        // -- Filter placement
	            
	        Op op = null ;
	        if ( exprs != null )
	            op = TransformFilterPlacement.transform(exprs, pattern) ;
	        else
	            op = new OpBGP(pattern) ;
	        
	        return plainExecute(op, input, execCxt) ;
	}
	
    /** Execute without modification of the op - does <b>not</b> apply special graph name translations */ 
    private static QueryIterator plainExecute(Op op, QueryIterator input, ExecutionContext execCxt)
    {
        // -- Execute
        // Switch to a non-reordering executor
        // The Op may be a sequence due to TransformFilterPlacement
        // so we need to do a full execution step, not go straight to the SolverLib.
        
        ExecutionContext ec2 = new ExecutionContext(execCxt) ;
        ec2.setExecutor(plainFactory) ;

        // Solve without going through this executor again.
        // There would be issues of nested patterns but this is only a
        // (filter (bgp...)) or (filter (quadpattern ...)) or sequences of these.
        // so there are no nested patterns to reorder.
        return QC.execute(op, input, ec2) ;
    }
	
    private static BasicPattern reorder(BasicPattern pattern, QueryIterPeek peek, ReorderTransformation transform)
    {
        if ( transform != null )
        {
            // This works by getting one result from the peek iterator,
            // and creating the more grounded BGP. The tranform is used to
            // determine the best order and the transformation is returned. This
            // transform is applied to the unsubstituted pattern (which will be
            // substituted as part of evaluation.
            
            if ( ! peek.hasNext() )
                throw new ARQInternalErrorException("Peek iterator is already empty") ;
 
            BasicPattern pattern2 = Substitute.substitute(pattern, peek.peek() ) ;
            // Calculate the reordering based on the substituted pattern.
            ReorderProc proc = transform.reorderIndexes(pattern2) ;
            // Then reorder original patten
            pattern = proc.reorder(pattern) ;
        }
        return pattern ;
    }
    
    private static final OpExecutorFactory plainFactory = new OpExecutorPlainFactoryHDT() ;
    private static class OpExecutorPlainFactoryHDT implements OpExecutorFactory
    {
        @Override
        public OpExecutor create(ExecutionContext execCxt)
        {
            return new OpExecutorPlainHDT(execCxt) ;
        }
    }

    /** An op executor that simply executes a BGP or QuadPattern without any reordering */ 
    private static class OpExecutorPlainHDT extends OpExecutor
    {
        final Predicate<Tuple<HDTId>> filter;
        
        @SuppressWarnings("unchecked")
		public OpExecutorPlainHDT(ExecutionContext execCxt)
        {
            super(execCxt) ;
            filter = (Predicate<Tuple<HDTId>>)execCxt.getContext().get(HDTJenaConstants.FILTER_SYMBOL);
        }
        
        @Override
        public QueryIterator execute(OpBGP opBGP, QueryIterator input)
        {
            Graph g = execCxt.getActiveGraph() ;
            
            if ( g instanceof HDTGraph )
            {
                BasicPattern bgp = opBGP.getPattern() ;
                Explain.explain("Execute", bgp, execCxt.getContext()) ;
                // Triple-backed (but may be named as explicit default graph).
                return HDTSolverLib.execute((HDTGraph)g, bgp, input, filter, execCxt) ;
            }
            Log.warn(this, "Non-HDTGraph passed to OpExecutorPlainHDT") ;
            return super.execute(opBGP, input) ;
        }
    }
    
}
