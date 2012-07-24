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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTFactory;
import org.rdfhdt.hdt.iterator.IteratorTripleString;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.util.StopWatch;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Lists;


/**
 * @author mario.arias
 *
 */
public class HdtSearch implements ProgressListener {
	@Parameter(description = "Files")
	public List<String> parameters = Lists.newArrayList();

	@Parameter(names = "-input", description = "Input HDT file name")
	public String hdtInput = null;	
	
	protected static void iterate(HDT hdt, CharSequence subject, CharSequence predicate, CharSequence object) {
		StopWatch iterateTime = new StopWatch();
		try {
			int count = 0;
			
			// Iterate over triples as Strings
			IteratorTripleString it = hdt.search(subject,predicate,object);
			count = 0;
			while(it.hasNext()) {
				TripleString triple = it.next();
				System.out.println(triple);
				count++;
			}
			
			// Iterate over triples only as IDs
//			IteratorTripleID it = hdt.getTriples().search(hdt.getDictionary().tripleStringtoTripleID(new TripleString(subject, predicate, object)));
//			while(it.hasNext()) {
//				TripleID triple = it.next();
//				System.out.println(triple);
//				count++;
//			}
			System.out.println("Iterated "+ count + " triples in "+iterateTime.stopAndShow());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public void execute() throws ParserException, IOException {
		
		HDT hdt = HDTFactory.createHDT(new HDTSpecification());
		hdt.loadFromHDT(hdtInput, this);
		hdt.loadOrCreateIndex(this);
		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		TripleString cmd = new TripleString();
		
		// FIXME: Implement everything that the README says it does
		
		while(true) {
			System.out.print("> ");
			System.out.flush();
			String line=in.readLine();
			if(line==null || line.equals("exit") || line.equals("quit")) {
				System.exit(0);
			}
			if(line.equals("help")) {
				System.out.println("HELP:");
				System.out.println("Please write Triple Search Pattern, using '?' for wildcards. e.g ");
				System.out.println("   http://www.somewhere.com/mysubject ? ?");
				continue;
			}
			
			cmd.read(line);
			System.out.println("Query: |"+cmd.getSubject()+"| |"+cmd.getPredicate()+"| |" + cmd.getObject()+"|");
			
			iterate(hdt,cmd.getSubject(),cmd.getPredicate(),cmd.getObject());
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
		HdtSearch hdtSearch = new HdtSearch();
		JCommander com = new JCommander(hdtSearch, args);
		
		try {
			if (hdtSearch.hdtInput==null)
				hdtSearch.hdtInput = hdtSearch.parameters.get(0);
		} catch (Exception e){
			com.usage();
			System.exit(1);
		}
		
		hdtSearch.execute();
	}

	
}
