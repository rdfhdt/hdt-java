/**
 * File: $HeadURL$
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
 */

package org.rdfhdt.hdtjena;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.util.Symbol;

public class HDTJenaConstants {
	private static final String HDTJENA_NS     = "http://www.rdfhdt.org/fuseki#" ;
	public static final Resource tGraphHDT     = ResourceFactory.createResource(HDTJENA_NS+"HDTGraph") ;
	public static final Property pFileName     = ResourceFactory.createProperty(HDTJENA_NS+"fileName");
	public static final Property pKeepInMemory = ResourceFactory.createProperty(HDTJENA_NS+"keepInMemory");
	public static final Property pCacheSize = ResourceFactory.createProperty(HDTJENA_NS+"cacheSize");
	public static final Property pCacheSizeNodeToID = ResourceFactory.createProperty(HDTJENA_NS+"cacheSizeNodeToID");
	public static final Property pCacheSizeIDToNode = ResourceFactory.createProperty(HDTJENA_NS+"cacheSizeIDToNode");
	
	public static final Symbol REMOVE_DUPLICATES = Symbol.create(HDTJENA_NS+"removeDuplicates");
	public static final Symbol FILTER_SYMBOL = Symbol.create(HDTJENA_NS+"filter");
}
