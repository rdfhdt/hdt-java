/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/tools/org/rdfhdt/hdt/tools/HDTInfo.java $
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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDTVersion;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.util.io.IOUtil;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * @author mario.arias
 *
 */
public class HDTInfo {
	@Parameter(description = "<HDT File>")
	public List<String> parameters = new ArrayList<>();

	@Parameter(names = "-version", description = "Prints the HDT version number")
	public static boolean showVersion;

	public String hdtInput;

	public void execute() throws ParserException, IOException {
		InputStream input;
		if (hdtInput.endsWith(".gz")) {
			input = new GZIPInputStream(new FileInputStream(hdtInput));
		} else {
			input = new BufferedInputStream(new FileInputStream(hdtInput));
		}
		ControlInformation ci = new ControlInformation();

		// Load Global ControlInformation
		ci.load(input);

		// Load header
		ci.load(input);
		int headerSize = (int) ci.getInt("length");

		byte[] headerData = IOUtil.readBuffer(input, headerSize, null);
		input.close();

		System.out.write(headerData);

		input.close();
	}

	public static void main(String[] args) throws Throwable {
		HDTInfo hdtInfo = new HDTInfo();
		JCommander com = new JCommander(hdtInfo, args);
		com.setProgramName("hdtInfo");
		if (showVersion) {
			System.out.println(HDTVersion.get_version_string("."));
			System.exit(0);
		}

		try {
			if (hdtInfo.hdtInput == null)
				hdtInfo.hdtInput = hdtInfo.parameters.get(0);
		} catch (Exception e) {
			com.usage();
			System.exit(1);
		}


		hdtInfo.execute();
	}

}
