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

import com.hp.hpl.jena.graph.Capabilities;

/**
 * @author mck
 *
 */
public class HDTCapabilities implements Capabilities
{
	@Override
	public boolean sizeAccurate() { return true; }
	@Override
	public boolean addAllowed() { return false; }
	@Override
	public boolean addAllowed( boolean every ) { return false; } 
	@Override
	public boolean deleteAllowed() { return false; }
	@Override
	public boolean deleteAllowed( boolean every ) { return false; } 
	@Override
	public boolean canBeEmpty() { return true; }
	@Override
	public boolean iteratorRemoveAllowed() { return false; }
	@Override
	public boolean findContractSafe() { return true; }
	@Override
	public boolean handlesLiteralTyping() { return true; }
}
