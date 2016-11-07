/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/options/HDTSpecification.java $
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

package org.rdfhdt.hdt.options;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Holds the properties of HDT from a configuration file
 * 
 */
public class HDTSpecification extends HDTOptionsBase {

	/**
	 * Default constructor, reads the file config.properties
	 * 
	 * @throws IOException
	 */
	public HDTSpecification() {
		super();
	}

	/**
	 * Constructor that reads a specific filename
	 * 
	 * @param filename
	 * @throws IOException 
	 */
	public HDTSpecification(String filename) throws IOException {
		super();
		load(filename);
	}

	public void load(String filename) throws IOException {
		FileInputStream	fin = new FileInputStream(filename);
		try {
			properties.load(fin);
		} finally {
			fin.close();
		}

	}

}
