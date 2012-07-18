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
 *   Alejandro Andres:          fuzzy.alej@gmail.com
 */
package org.rdfhdt.hdt.tools;

import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTFactory;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTSpecification;

import java.io.IOException;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Lists;


/**
 * @author mario.arias
 *
 */
public class RDF2HDT implements ProgressListener {
	@Parameter(description = "Files")
	public List<String> parameters = Lists.newArrayList();

	@Parameter(names = "-options", description = "HDT Conversion options")
	public String options = null;
	
	@Parameter(names = "-config", description = "Conversion config file")
	public String configFile = null;
	
	@Parameter(names = "-input", description = "Input RDF file name")
	public String rdfInput = null;
	
	@Parameter(names = "-output", description = "Output HDT file name")
	public String hdtOutput = null;
	
	@Parameter(names = "-rdftype", description = "Type of RDF Input (ntriples, n3, rdfxml)")
	public String rdfType = null;
	
	@Parameter(names = "-base", description = "Base URI for the dataset")
	public String baseURI = null;
	
	@Parameter(names = "-index", description = "Generate also external indices to solve all queries")
	public boolean generateIndex = false;
	
	public void execute() throws ParserException, IOException {
		HDTSpecification spec;
		if(configFile!=null) {
			spec = new HDTSpecification(configFile);
		} else {
			spec = new HDTSpecification();
		}
		if(options!=null) {
			spec.setOptions(options);
		}
		if(baseURI==null) {
			baseURI = "file://"+rdfInput;
		}
		HDT hdt = HDTFactory.createHDTFromRDF(spec, rdfInput, baseURI, RDFNotation.parse(rdfType), this);
		
		hdt.saveToHDT(hdtOutput, this);
		
		if(generateIndex) {
			hdt.loadOrCreateIndex(this);
		}
		// Debug all inserted triples
		//HdtSearch.iterate(hdt, "","","");
	}
	
	/* (non-Javadoc)
	 * @see hdt.ProgressListener#notifyProgress(float, java.lang.String)
	 */
	@Override
	public void notifyProgress(float level, String message) {
		//System.out.println(message + "\t"+ Float.toString(level));
	}
	
	public static void main(String[] args) throws Throwable {
		RDF2HDT rdf2hdt = new RDF2HDT();
		JCommander com = new JCommander(rdf2hdt, args);
		if(rdf2hdt.parameters.size()<2) {
			com.usage();
			System.exit(1);
		}
		
		rdf2hdt.rdfInput = rdf2hdt.parameters.get(0);
		rdf2hdt.hdtOutput = rdf2hdt.parameters.get(1);
		
		rdf2hdt.execute();
	}

	
}
