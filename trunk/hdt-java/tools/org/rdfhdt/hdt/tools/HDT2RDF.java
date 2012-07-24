/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/tools/hdt/tools/HdtSearch.java $
 * Revision: $Rev: 5 $
 * Last modified: $Date: 2012-06-22 12:54:53 +0100 (vie, 22 jun 2012) $
 * Last modified by: $Author: mario.arias@gmail.com $
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

import java.io.PrintStream;
import java.util.List;

import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTFactory;
import org.rdfhdt.hdt.iterator.IteratorTripleString;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.TripleString;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Lists;


/**
 * @author mario.arias
 *
 */
public class HDT2RDF implements ProgressListener {
	@Parameter(description = "Files")
	public List<String> parameters = Lists.newArrayList();

	@Parameter(names = "-input", description = "Input HDT file name")
	public String hdtInput = null;
	
	@Parameter(names = "-output", description = "Output RDF file name")
	public String rdfOutput = null;

	public void execute() throws Exception {
		
		PrintStream out = null;
		if (rdfOutput.equals("stdout")){
			out = System.out;
		} else {
			out = new PrintStream(rdfOutput);
		}
		
		HDT hdt = HDTFactory.createHDT(new HDTSpecification());
		hdt.loadFromHDT(hdtInput, this);

		IteratorTripleString it = hdt.search("","","");
		while(it.hasNext()) {
			TripleString triple = it.next();
			out.print(triple.asNtriple());
		}		
	}

	/* (non-Javadoc)
	 * @see hdt.ProgressListener#notifyProgress(float, java.lang.String)
	 */
	@Override
	public void notifyProgress(float level, String message) {
		//System.out.println(message + "\t"+ Float.toString(level));
	}

	public static void main(String[] args) throws Throwable {
		HDT2RDF hdt2rdf = new HDT2RDF();
		JCommander com = new JCommander(hdt2rdf, args);

		if(hdt2rdf.hdtInput==null) {
			try {
				hdt2rdf.hdtInput = hdt2rdf.parameters.get(0);
			} catch (Exception e){
				com.usage();
				System.exit(1);
			}
		}
		if (hdt2rdf.rdfOutput==null){
			hdt2rdf.rdfOutput = "stdout";
		}
		System.out.println("Converting "+hdt2rdf.hdtInput+" to RDF on "+hdt2rdf.rdfOutput);
		
		hdt2rdf.execute();
	}


}
