/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/triples/impl/TriplesList.java $
 * Revision: $Rev: 45 $
 * Last modified: $Date: 2012-07-31 12:00:35 +0100 (Tue, 31 Jul 2012) $
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

package org.rdfhdt.hdt.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.rdfhdt.hdt.options.HDTSpecification;

/**
 * A class for getting basic information about a file
 * 
 * @author Eugen
 *
 */
public class RDFInfo {

	public static final String size_prop = "rdf.sizeInBytes";
	public static final String lines_prop = "rdf.lines";
	public static final String compression_prop = "rdf.expectedCompression";

	/**
	 * Convinient method that counts lines in the given file and writes the info about the
	 * length of the file in bytes and the number of lines in the given specs, overwriting if
	 * they were already set.
	 * 
	 * @throws {@link RuntimeException} if something is wrong with the file... 
	 */
	public static void fillHDTSpecifications(String filename, HDTSpecification specs) {

		File file = new File(filename);
		long sizeInBytes = -1;
		long numLines = -1;
		try {
			sizeInBytes = file.length();
			numLines = countLines(file);
		} catch (FileNotFoundException e){
			throw new RuntimeException("File "+filename+" doesn't exist or is not a file or can't be opened.",e);
		} catch (IOException e){
			throw new RuntimeException("Unexpected error while reading file "+filename,e);
		}
		fillHDTSpecifications(sizeInBytes, numLines, specs);

	}

	/**
	 * Fills the given HDTSpecification object with the given information about the (RDF)
	 * file.
	 * 
	 * It sets these options: rdf.sizeInBytes, rdf.lines
	 * 
	 * WILL overwrite if options already set in specs.
	 *  
	 * @param specs HDTSpecification object to be filled
	 */
	public static void fillHDTSpecifications(long sizeInBytes, long numLines, HDTSpecification specs){

		if (sizeInBytes<0)
			System.err.println("WARNING! Setting rdf.sizeInBytes to a negative value!");
		specs.setInt(size_prop, sizeInBytes);
		if (numLines<0)
			System.err.println("WARNING! Setting rdf.lines to a negative value!");
		specs.setInt(lines_prop, numLines);
	}

	/**
	 * Sets the "rdf.sizeInBytes" property, overwrites if existing
	 */
	public static void setSizeInBytes(long sizeInBytes, HDTSpecification specs){
		specs.setInt(size_prop, sizeInBytes);
	}

	/**
	 * Sets the "rdf.lines" property, overwrites if existing
	 */
	public static void setLines(long numLines, HDTSpecification specs){
		specs.setInt(lines_prop, numLines);
	}
	
	/**
	 * Sets the "rdf.expectedCompression" property, overwrites if existing
	 */
	public static void setCompression(float compression, HDTSpecification specs){
		specs.set(compression_prop, String.format("%.3f", compression));
	}

	/**
	 * Gets the "rdf.sizeInBytes" property and returns it if set, otherwise returns null.
	 */
	public static Long getSizeInBytes(HDTSpecification specs){
		long x = specs.getInt(size_prop);
		if (x>0)
			return x;
		else
			return null;
	}

	/**
	 * Gets the "rdf.lines" property and returns it if set, otherwise returns null.
	 */
	public static Long getLines(HDTSpecification specs){
		long x = specs.getInt(lines_prop);
		if (x>0)
			return x;
		else
			return null;
	}
	
	/**
	 * Gets the "rdf.expectedCompression" peoperty is it is set. If not sets it to 0.15 and returns that value.
	 */
	public static float getCompression(HDTSpecification specs){
		float compression = 0;
		try {
			compression = Float.parseFloat(specs.get(compression_prop));
		} catch (NumberFormatException e){
			System.err.println(compression_prop+" improperly set, using default 0.15");
			compression = 0.15f; //FIXME fixed default parameter here, not best of practices
			setCompression(compression, specs);
		} catch (NullPointerException e){
			System.err.println(compression_prop+" missing, using default 0.15");
			compression = 0.15f; //FIXME fixed default parameter here, not best of practices
			setCompression(compression, specs);
		}
		return compression;
	}

	/**
	 * Finds the length of file in bytes and counts the number of lines and fills
	 * the given specs with them using "fillHDTSpecification(long, int, HDTSpecifications)" method.
	 * 
	 */
	public static long countLines(File file) throws FileNotFoundException, IOException {

		long numLines = 0;
		BufferedReader reader = new BufferedReader(new FileReader(file));
		while(reader.readLine()!=null) {
			numLines++;
		}
		reader.close();

		return numLines;
	}

}
