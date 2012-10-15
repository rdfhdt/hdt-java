/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/tools/org/rdfhdt/hdt/tools/HdtSearch.java $
 * Revision: $Rev: 58 $
 * Last modified: $Date: 2012-08-26 00:31:15 +0100 (dom, 26 ago 2012) $
 * Last modified by: $Author: simpsonim13@gmail.com $
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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.rdfhdt.hdt.compact.integer.VByte;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.util.io.IOUtil;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Lists;


/**
 * @author mario.arias
 *
 */
public class HDTInfo {
	@Parameter(description = "Files")
	public List<String> parameters = Lists.newArrayList();

	@Parameter(names = "-input", description = "Input HDT file name")
	public String hdtInput = null;	
	
	public void execute() throws ParserException, IOException {
		InputStream input = new BufferedInputStream(new FileInputStream(hdtInput));
		ControlInformation ci = new ControlInformation();
		
		// Load header
		ci.load(input);
		int headerSize = (int)VByte.decode(input);
		
		byte [] headerData = IOUtil.readBuffer(input, headerSize, null);
		input.close();	
		
		System.out.write(headerData);
		
	}

	public static void main(String[] args) throws Throwable {
		HDTInfo hdtInfo = new HDTInfo();
		JCommander com = new JCommander(hdtInfo, args);
		
		try {
			if (hdtInfo.hdtInput==null)
				hdtInfo.hdtInput = hdtInfo.parameters.get(0);
		} catch (Exception e){
			com.usage();
			System.exit(1);
		}
		
		hdtInfo.execute();
	}

	
}
