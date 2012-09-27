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

import java.io.IOException;
import java.util.List;

import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTFactory;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.util.StopWatch;

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
	
	@Parameter(names = "-rdftype", description = "Type of RDF Input (ntriples, nquad, n3, turtle, rdfxml)")
	public String rdfType = "ntriples";
	
	@Parameter(names = "-base", description = "Base URI for the dataset")
	public String baseURI = null;
	
	@Parameter(names = "-index", description = "Generate also external indices to solve all queries")
	public boolean generateIndex = false;
	
	@Parameter(names = "-progress", description = "Show progress of the conversion")
	public boolean progress = false;
	
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
		
		//TODO debug delete from here...
		System.out.println("triples: "+hdt.getTriples().getNumberOfElements());
		System.out.println("subjects: "+hdt.getDictionary().getNsubjects());
		System.out.println("predicates: "+hdt.getDictionary().getNpredicates());
		System.out.println("objects: "+hdt.getDictionary().getNobjects());
		System.out.println("shared: "+hdt.getDictionary().getNshared());
		// ... to here
		
		// Dump to HDT file
		StopWatch sw = new StopWatch();
		hdt.saveToHDT(hdtOutput, this);
		System.out.println("HDT saved to file in: "+sw.stopAndShow());
		
		// Generate index and dump it to jindex file
		sw.reset();
		if(generateIndex) {
			hdt.loadOrCreateIndex(this);
			System.out.println("Index generated and saved in: "+sw.stopAndShow());
		}
		
		// Debug all inserted triples
		//HdtSearch.iterate(hdt, "","","");
	}
	
	/* (non-Javadoc)
	 * @see hdt.ProgressListener#notifyProgress(float, java.lang.String)
	 */
	@Override
	public void notifyProgress(float level, String message) {
		if(progress) {
			System.out.println(message + "\t"+ Float.toString(level));
		}
	}
	
	public static void main(String[] args) throws Throwable {
		RDF2HDT rdf2hdt = new RDF2HDT();
		JCommander com = new JCommander(rdf2hdt, args);
	
		if (rdf2hdt.rdfInput==null){
			try {
				rdf2hdt.rdfInput = rdf2hdt.parameters.get(0); //first 'free' param
			} catch (IndexOutOfBoundsException e){
				com.usage();
				System.exit(1);
			}
		}
		if (rdf2hdt.hdtOutput==null){
			try {
				rdf2hdt.hdtOutput = rdf2hdt.parameters.get(rdf2hdt.parameters.size()-1); //last 'free' param
				if (rdf2hdt.rdfInput.equals(rdf2hdt.hdtOutput))
					throw new IndexOutOfBoundsException(); //have to be different
			} catch (IndexOutOfBoundsException e){
				com.usage();
				System.exit(1);
			}
		}
		System.out.println("Converting "+rdf2hdt.rdfInput+" to "+rdf2hdt.hdtOutput+" as "+rdf2hdt.rdfType);
		
		rdf2hdt.execute();
	}
}