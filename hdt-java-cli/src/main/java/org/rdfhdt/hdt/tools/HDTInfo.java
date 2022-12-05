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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.rdfhdt.hdt.enums.CompressionType;
import org.rdfhdt.hdt.hdt.HDTVersion;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.util.io.IOUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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

	public void execute() throws IOException {
		byte[] headerData;
		try (InputStream input = IOUtil.asUncompressed(new FileInputStream(hdtInput), CompressionType.guess(hdtInput))) {
			ControlInformation ci = new ControlInformation();

			// Load Global ControlInformation
			ci.load(input);

			// Load header
			ci.load(input);
			int headerSize = (int) ci.getInt("length");

			headerData = IOUtil.readBuffer(input, headerSize, null);
		}

		System.out.write(headerData, 0, headerData.length);
	}

	public static void main(String[] args) throws Throwable {
		HDTInfo hdtInfo = new HDTInfo();
		JCommander com = new JCommander(hdtInfo);
		com.parse(args);
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
