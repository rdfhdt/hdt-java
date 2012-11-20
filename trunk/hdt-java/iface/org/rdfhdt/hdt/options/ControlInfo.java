/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/options/ControlInformation.java $
 * Revision: $Rev: 66 $
 * Last modified: $Date: 2012-09-17 23:35:51 +0100 (lun, 17 sep 2012) $
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author mario.arias
 *
 */
public interface ControlInfo extends HDTOptions {
	public enum Type {
		UNKNOWN,
		GLOBAL,
		HEADER,
		DICTIONARY,
		TRIPLES,
		INDEX
	}
	
	public Type getType();
	
	public void setType(Type type);
	
	public String getFormat();

	public void setFormat(String format);

	public void save(OutputStream output) throws IOException;
	
	public void load(InputStream input) throws IOException;	

}
