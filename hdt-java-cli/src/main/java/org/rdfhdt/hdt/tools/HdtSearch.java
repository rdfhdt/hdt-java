/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/tools/org/rdfhdt/hdt/tools/HdtSearch.java $
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.hdt.HDTVersion;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.quad.QuadString;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.util.StopWatch;
import org.rdfhdt.hdt.util.UnicodeEscape;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.rdfhdt.hdt.util.listener.MultiThreadListenerConsole;

import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * @author mario.arias
 *
 */
public class HdtSearch {
	@Parameter(description = "<HDT File>")
	public List<String> parameters = new ArrayList<>();

	@Parameter(names = "-options", description = "HDT Conversion options (override those of config file)")
	public String options;

	@Parameter(names = "-config", description = "Conversion config file")
	public String configFile;

	@Parameter(names = "-color", description = "Print using color (if available)")
	public boolean color;

	@Parameter(names = "-quiet", description = "Do not show progress of the conversion")
	public boolean quiet;

	@Parameter(names = "-version", description = "Prints the HDT version number")
	public static boolean showVersion;
	
	public String hdtInput;
	
	@Parameter(names = "-memory", description = "Load the whole file into main memory. Ensures fastest querying.")
	public boolean loadInMemory;
	
	protected static void iterate(
		HDT hdt,
		boolean isHDTQ,
		CharSequence subject,
		CharSequence predicate,
		CharSequence object,
		CharSequence graph
	) throws NotFoundException {
		StopWatch iterateTime = new StopWatch();
		int count;

		subject = subject.length()==1 && subject.charAt(0)=='?' ? "" : subject;
		predicate = predicate.length()==1 && predicate.charAt(0)=='?' ? "" : predicate;
		object = object.length()==1 && object.charAt(0)=='?' ? "" : object;
		graph = graph.length()==1 && graph.charAt(0)=='?' ? "" : graph;
		// Iterate over triples as Strings
		IteratorTripleString it =
			isHDTQ
			? hdt.search(subject, predicate, object, graph)
			: hdt.search(subject, predicate, object);
		count = 0;
		while(it.hasNext()) {
			TripleString triple = it.next();
			System.out.println(triple);
			count++;
		}

//		Iterate over triples only as IDs
//		TripleID patternID = DictionaryUtil.tripleStringtoTripleID(hdt.getDictionary(), new TripleString(subject, predicate, object));
//		IteratorTripleID it2 = hdt.getTriples().search(patternID);
//		while(it2.hasNext()) {
//			TripleID triple = it2.next();
//			System.out.println(triple);
//			count++;
//		}
		System.out.println("Iterated "+ count + " triples in "+iterateTime.stopAndShow());
	}
	
	private void help() {
		System.out.println("HELP:");
		System.out.println("Please write Triple Search Pattern, using '?' for wildcards. e.g ");
		System.out.println("   http://www.somewhere.com/mysubject ? ?");
		System.out.println("Use 'exit' or 'quit' to terminate interactive shell.");
	}

	private static void parseTriplePatternErr(boolean isHDTQ) throws ParserException {
		throw new ParserException(
			isHDTQ
			? "Make sure that you included four terms."
			: "Make sure that you included three terms."
		);
	}
	
	/**
	 * Read from a line, where each component is separated by space.
	 * @param line line to parse
	 */
	private static void parseTriplePattern(TripleString dest, String line, boolean isHDTQ) throws ParserException {
		int split, posa, posb;
		dest.clear();
		
		// SET SUBJECT
		posa = 0;
		posb = split = line.indexOf(' ', posa);
		
		if(posb==-1) parseTriplePatternErr(isHDTQ);
		
		dest.setSubject(UnicodeEscape.unescapeString(line.substring(posa, posb)));
	
		// SET PREDICATE
		posa = split+1;
		posb = split = line.indexOf(' ', posa);
		
		if(posb==-1) parseTriplePatternErr(isHDTQ);
		
		dest.setPredicate(UnicodeEscape.unescapeString(line.substring(posa, posb)));
		
		if (isHDTQ) {
			// SET OBJECT
			posa = split+1;
			posb = split = line.indexOf(' ', posa);
			
			if(posb==-1) parseTriplePatternErr(isHDTQ);
			
			dest.setObject(UnicodeEscape.unescapeString(line.substring(posa, posb)));

			// SET GRAPH
			posa = split+1;
			posb = line.length();
			
			if(line.charAt(posb-1)=='.') posb--;
			if(line.charAt(posb-1)==' ') posb--;
					
			dest.setGraph(UnicodeEscape.unescapeString(line.substring(posa, posb)));
		} else {
			// SET OBJECT
			posa = split+1;
			posb = line.length();
			
			if(line.charAt(posb-1)=='.') posb--;	// Remove trailing <space> <dot> from NTRIPLES.
			if(line.charAt(posb-1)==' ') posb--;
					
			dest.setObject(UnicodeEscape.unescapeString(line.substring(posa, posb)));
		}
	}
	
	public void execute() throws IOException {
		HDTOptions spec;
		if(configFile != null) {
			spec = HDTOptions.readFromFile(configFile);
		} else {
			spec = HDTOptions.of();
		}
		if (options != null) {
			spec.setOptions(options);
		}

		ProgressListener listenerConsole =
				!quiet ? new MultiThreadListenerConsole(color)
						: ProgressListener.ignore();

		HDT hdt;
		if(loadInMemory) {
			hdt = HDTManager.loadIndexedHDT(hdtInput, listenerConsole, spec);
		} else {
			hdt= HDTManager.mapIndexedHDT(hdtInput, spec, listenerConsole);
		}
		Dictionary dict = hdt.getDictionary();
		boolean isHDTQ = dict.supportGraphs();

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in, UTF_8));
		try {
			TripleString triplePattern = new QuadString();

			while(true) {
				System.out.print(">> ");
				System.out.flush();
				String line=in.readLine();
				if(line==null || line.equals("exit") || line.equals("quit")) {
					break;
				}
				if(line.equals("help")) {
					help();
					continue;
				}

				try {
					parseTriplePattern(triplePattern, line, isHDTQ);
					if (isHDTQ) {
						System.out.println("Query: |"+triplePattern.getSubject()+"| |"+triplePattern.getPredicate()+"| |" + triplePattern.getObject()+"| |" + triplePattern.getGraph()+"|");
					} else {
						System.out.println("Query: |"+triplePattern.getSubject()+"| |"+triplePattern.getPredicate()+"| |" + triplePattern.getObject()+"|");
					}

					iterate(
						hdt,
						isHDTQ,
						triplePattern.getSubject(),
						triplePattern.getPredicate(),
						triplePattern.getObject(),
						triplePattern.getGraph()
					);
				} catch (ParserException e) {
					System.err.println("Could not parse triple pattern: "+e.getMessage());
					help();
				} catch (NotFoundException e) {
					System.err.println("No results found.");
				}

			}
		} finally {
			if(hdt!=null) hdt.close();
			in.close();
		}
	}

	public static void main(String[] args) throws Throwable {
		HdtSearch hdtSearch = new HdtSearch();
		JCommander com = new JCommander(hdtSearch);
		com.parse(args);
		com.setProgramName("hdtSearch");

		if (showVersion) {
			System.out.println(HDTVersion.get_version_string("."));
			System.exit(0);
		}
		
		if(hdtSearch.parameters.size()!=1) {
			com.usage();
			System.exit(1);
		}
		
		hdtSearch.hdtInput = hdtSearch.parameters.get(0);
		
		hdtSearch.execute();
	}

	
}
