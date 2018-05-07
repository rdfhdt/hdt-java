/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.rdfhdt.hdtjena.bindings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBase;

/** Binding that delays turning an HDTId into a Jena Node using the dictionary until explicitly needed by get() */

public class BindingHDTNode extends BindingBase
{

    private final BindingHDTId idBinding ;

    public BindingHDTNode(BindingHDTId idBinding)
    {
        // BindingHDTId contains the bindings actually used  copied down when created. 
        super(idBinding.getParentBinding()) ;
        this.idBinding = idBinding ;
    }

    @Override
    protected int size1() { return idBinding.size(); }
    
    private List<Var> vars;
    
    /** Iterate over all the names of variables. */
    @Override
    protected Iterator<Var> vars1() 
    {
        if ( vars == null )
            vars = calcVars() ;
        return vars.iterator() ;
    }

    private List<Var> calcVars()
    {
        List<Var> vars = new ArrayList<>(4) ;
        // Only if not in parent.
        // A (var/value) binding may have been copied down to record it's HDTId.  
        
        Binding b = idBinding.getParentBinding() ;
        
        for ( Var v : idBinding )
        {
            if ( b == null || ! b.contains(v) )
                vars.add(v) ;
        }
        return vars ;
    }
    
    @Override
    protected boolean isEmpty1()
    {
        return size1() == 0 ;
    }

    @Override
    public boolean contains1(Var var)
    {
        return idBinding.containsKey(var) ;
    }
    
    public BindingHDTId getBindingId() { return idBinding ; }
    
    public HDTId getHDTId(Var var)
    {
        HDTId id = idBinding.get(var) ;
        if ( id != null )
            return id ;
        
        if ( parent == null )
            return null ; 

        // Maybe in the parent.
        if ( parent instanceof BindingHDTNode )
            return ((BindingHDTNode)parent).getHDTId(var) ;
        return null ;
    }
    
    @Override
    public Node get1(Var var)
    {
        try {           
            HDTId id = idBinding.get(var) ;
            if ( id == null )
                return null;

            return id.getNode();
        } catch (Exception ex)
        {
        	ex.printStackTrace();
            return null ;
        }
    }
    
    @Override
    protected void format(StringBuffer sbuff, Var var)
    {
        HDTId id = idBinding.get(var) ;
        String extra = "" ;
        if ( id != null )
            extra = "/"+id ;
        Node node = get(var) ;

        sbuff.append("( ?")
                .append(var.getVarName())
                .append(extra)
                .append(" = ")
                .append(node)
                .append(" )");
    }

}
