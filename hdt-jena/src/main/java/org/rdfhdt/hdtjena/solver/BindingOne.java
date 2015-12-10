package org.rdfhdt.hdtjena.solver;

import java.util.Iterator;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;


/**
 * Copied from Jena distribution because the constructor was protected :(
 * 
 * @author mario.arias
 *
 */

public class BindingOne implements Binding
{
    private final Var var ;
    private final Node value ;
    
    public BindingOne(Var var, Node node)
    { 
        this.var = var; 
        this.value = node;
    }
    
    @Override
	public int size() { return 1 ; }
    
    @Override
	public boolean isEmpty() { return false ; }
    
    /** Iterate over all the names of variables.
     */
    @Override
    public Iterator<Var> vars() 
    {
        return Iter.singleton(var) ;
    }
    
    @Override
    public boolean contains(Var n)
    {
        return var.equals(n) ;
    }
    
    @Override
    public Node get(Var v)
    {
        if ( v.equals(var) )
            return value ;
        return null ;
    }
}