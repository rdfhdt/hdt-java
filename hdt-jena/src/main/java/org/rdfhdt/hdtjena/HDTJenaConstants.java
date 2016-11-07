/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-jena/src/org/rdfhdt/hdtjena/HDTJenaConstants.java $
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

package org.rdfhdt.hdtjena;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.util.Symbol;

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
	
	private HDTJenaConstants() {}
}
