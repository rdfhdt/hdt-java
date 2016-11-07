/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-jena/src/org/rdfhdt/hdtjena/bindings/BindingHDTId.java $
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

package org.rdfhdt.hdtjena.bindings;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.jena.atlas.lib.Map2;

import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;


/** Class for a Binding-like structure except it works on HDT IDs, not on Nodes */  
public class BindingHDTId extends Map2<Var, HDTId>
{
    // This is the parent binding - which may be several steps up the chain. 
    // This just carried around for later use when we go BindingHDTId back to Binding.
    private final Binding parentBinding;

    // Possible optimization: there are at most 3 possible values so HashMap is overkill.
    // Use a chain of small objects.
    
    private BindingHDTId(Map<Var, HDTId> map1, Map2<Var, HDTId> map2, Binding parentBinding)
    {
        super(map1, map2);
        this.parentBinding = parentBinding;
    }

    // Make from an existing BindingHDTId 
    public BindingHDTId(BindingHDTId other)
    {
        this(new HashMap<>(), other, other.getParentBinding());
    }
    
    // Make from an existing Binding 
    public BindingHDTId(Binding binding)
    {
        this(new HashMap<>(), null, binding);
    }

    public BindingHDTId()
    {
        this(new HashMap<>(), null, null);
    }
    
    public Binding getParentBinding()    { return parentBinding; } 
    
    //@Override public HDTId get(Var v)    { return super.get(v); } 
    
    @Override public void put(Var v, HDTId n)
    {
        if ( v == null || n == null )
            throw new IllegalArgumentException("("+v+","+n+")");
        super.put(v, n);
    }
    
    public void putAll(BindingHDTId other)
    {
        Iterator<Var> vIter = other.iterator();
        
        for (; vIter.hasNext(); )
        {
            Var v = vIter.next();
            if ( v == null )
                throw new IllegalArgumentException("Null key");
            HDTId n = other.get(v);
            if ( n == null )
                throw new IllegalArgumentException("("+v+","+n+")");
            super.put(v, n);
        }
    }
    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        boolean first = true;
        for ( Var v : this )
        {
            if ( ! first )
                sb.append(" ");
            first = false;
            HDTId x = get(v);
            sb.append(v);
            sb.append(" = ");
            sb.append(x);
        }
            
        return sb.toString();
    }
}
