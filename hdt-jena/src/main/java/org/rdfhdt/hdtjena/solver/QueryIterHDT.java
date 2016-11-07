/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-jena/src/org/rdfhdt/hdtjena/solver/QueryIterHDT.java $
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

import java.util.Iterator;
import java.util.List;

import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.iterator.QueryIterPlainWrapper;
import org.rdfhdt.hdtjena.util.Abortable;


public class QueryIterHDT extends QueryIterPlainWrapper
{
    // Rename as QueryIterCloseOther?
    final private QueryIterator originalInput ;
    private final List<Abortable> killList ;
    
    // The original input needs closing as well.
    public QueryIterHDT(Iterator<Binding> iterBinding, List<Abortable> killList , QueryIterator originalInput, ExecutionContext execCxt)
    {
        super(iterBinding, execCxt) ;
        this.originalInput = originalInput ;
        this.killList = killList ;
    }
    
    @Override
    protected void closeIterator()
    { 
        if ( originalInput != null )
            originalInput.close();
        super.closeIterator() ;
    }

    @Override
    protected void requestCancel()
    { 
        if ( killList != null )
            for ( Abortable it : killList )
                it.abort() ;
    }
}
