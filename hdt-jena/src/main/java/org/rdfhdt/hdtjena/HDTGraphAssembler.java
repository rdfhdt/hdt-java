/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-jena/src/org/rdfhdt/hdtjena/HDTGraphAssembler.java $
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

import java.io.IOException;

import org.apache.jena.assembler.Assembler;
import org.apache.jena.assembler.Mode;
import org.apache.jena.assembler.assemblers.AssemblerBase;
import org.apache.jena.assembler.exceptions.AssemblerException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.util.graph.GraphUtils;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HDTGraphAssembler extends AssemblerBase implements Assembler {
	private static final Logger log = LoggerFactory.getLogger(HDTGraphAssembler.class);

	private static boolean initialized;

	public static void init() {
		if(initialized) {
			return;
		}

		initialized = true;

		Assembler.general.implementWith(HDTJenaConstants.tGraphHDT, new HDTGraphAssembler());
	}

	@Override
	public Model open(Assembler a, Resource root, Mode mode)
	{
		String file = GraphUtils.getStringValue(root, HDTJenaConstants.pFileName) ;
		boolean loadInMemory = Boolean.parseBoolean(GraphUtils.getStringValue(root, HDTJenaConstants.pKeepInMemory));
		try {
			// FIXME: Read more properties. Cache config?
			HDT hdt;
			if(loadInMemory) {
				hdt = HDTManager.loadIndexedHDT(file, null);				
			} else {
				hdt = HDTManager.mapIndexedHDT(file, null);
			}
			HDTGraph graph = new HDTGraph(hdt);
			return ModelFactory.createModelForGraph(graph);
		} catch (IOException e) {
			log.error("Error reading HDT file: {}", file, e);
			throw new AssemblerException(root, "Error reading HDT file: "+file+" / "+e.toString());
		}
	}

	static {
		init();
	}
}
