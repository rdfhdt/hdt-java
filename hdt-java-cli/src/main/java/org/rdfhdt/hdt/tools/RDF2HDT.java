/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/tools/org/rdfhdt/hdt/tools/RDF2HDT.java $
 * Revision: $Rev: 191 $
 * Last modified: $Date: 2013-03-03 11:41:43 +0000 (dom, 03 mar 2013) $
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
 *   Alejandro Andres:          fuzzy.alej@gmail.com
 */
package org.rdfhdt.hdt.tools;

import java.io.IOException;
import java.util.List;

import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.hdt.HDTVersion;
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

	public String rdfInput;
	public String hdtOutput;
	
	@Parameter(description = "<input RDF> <output HDT>")
	public List<String> parameters = Lists.newArrayList();

	@Parameter(names = "-options", description = "HDT Conversion options (override those of config file)")
	public String options;
	
	@Parameter(names = "-config", description = "Conversion config file")
	public String configFile;
	
	@Parameter(names = "-rdftype", description = "Type of RDF Input (ntriples, nquad, n3, turtle, rdfxml)")
	public String rdfType;
	
	@Parameter(names = "-version", description = "Prints the HDT version number")
	public static boolean showVersion;
	
	@Parameter(names = "-base", description = "Base URI for the dataset")
	public String baseURI;
	
	@Parameter(names = "-index", description = "Generate also external indices to solve all queries")
	public boolean generateIndex;
	
	@Parameter(names = "-quiet", description = "Do not show progress of the conversion")
	public boolean quiet;
	
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
		
		RDFNotation notation=null;
		if(rdfType!=null) {
			try {
				notation = RDFNotation.parse(rdfType);
			} catch (IllegalArgumentException e) {
				System.out.println("Notation "+rdfType+" not recognised.");
			}
		}
		
		if(notation==null) {
			try {
				notation =  RDFNotation.guess(rdfInput);
			} catch (IllegalArgumentException e) {
				System.out.println("Could not guess notation for "+rdfInput+" Trying NTriples");
				notation = RDFNotation.NTRIPLES;
			}
		}

		StopWatch sw = new StopWatch();
		HDT hdt = HDTManager.generateHDT(rdfInput, baseURI,notation , spec, this);
		System.out.println("File converted in: "+sw.stopAndShow());

		try {
			// Show Basic stats
			if(!quiet){
				System.out.println("Total Triples: "+hdt.getTriples().getNumberOfElements());
				System.out.println("Different subjects: "+hdt.getDictionary().getNsubjects());
				System.out.println("Different predicates: "+hdt.getDictionary().getNpredicates());
				System.out.println("Different objects: "+hdt.getDictionary().getNobjects());
				System.out.println("Common Subject/Object:"+hdt.getDictionary().getNshared());
			}

			// Dump to HDT file
			sw = new StopWatch();
			hdt.saveToHDT(hdtOutput, this);
			System.out.println("HDT saved to file in: "+sw.stopAndShow());

			// Generate index and dump it to .hdt.index file
			sw.reset();
			if(generateIndex) {
				hdt = HDTManager.indexedHDT(hdt,this);
				System.out.println("Index generated and saved in: "+sw.stopAndShow());
			}
		} finally {
			if(hdt!=null) hdt.close();
		}
		
		// Debug all inserted triples
		//HdtSearch.iterate(hdt, "","","");
	}
	
	/* (non-Javadoc)
	 * @see hdt.ProgressListener#notifyProgress(float, java.lang.String)
	 */
	@Override
	public void notifyProgress(float level, String message) {
		if(!quiet) {
			System.out.print("\r"+message + "\t"+ Float.toString(level)+"                            \r");
		}
	}
	
	public static void main(String[] args) throws Throwable {
		RDF2HDT rdf2hdt = new RDF2HDT();
		JCommander com = new JCommander(rdf2hdt, args);
		com.setProgramName("rdf2hdt");
	
		if(rdf2hdt.parameters.size()==1) {
			System.err.println("No input file specified, reading from standard input.");
			rdf2hdt.rdfInput = "-";
			rdf2hdt.hdtOutput = rdf2hdt.parameters.get(0);
		} else if(rdf2hdt.parameters.size()==2) {
			rdf2hdt.rdfInput = rdf2hdt.parameters.get(0);
			rdf2hdt.hdtOutput = rdf2hdt.parameters.get(1);
			
		} else if (showVersion){
				System.out.println(HDTVersion.get_version_string("."));
				System.exit(0);
			}
		else{
			com.usage();
			System.exit(1);
		}
		
		System.out.println("Converting "+rdf2hdt.rdfInput+" to "+rdf2hdt.hdtOutput+" as "+rdf2hdt.rdfType);
		
		rdf2hdt.execute();
	}
}
