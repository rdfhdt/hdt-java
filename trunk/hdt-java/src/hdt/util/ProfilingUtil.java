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

package hdt.util;


/**
 * @author mario.arias
 *
 */
public class ProfilingUtil {

	public static String tidyFileSize(long size ){
		long calcSize;
		String str;
		if (size >= 1024 * 1024 * 1024)
		{
			calcSize = (long) (((double)size) / (1024 * 1024 * 1024));
			str = ""+calcSize +"GB";
		}
		else if (size>= 1024 * 1024) {
			calcSize = (long) (((double)size) / (1024 * 1024 ));
			str = ""+ calcSize +"MB";
		}
		else if (size>= 1024) {
			calcSize = (long) (((double)size) / (1024));
			str = ""+ calcSize +"KB";
		}
		else {
			calcSize = size;
			str = ""+ calcSize +"GB";
		}
		return str;
	}

	public static void showMemory(String label) {
		System.out.println(label+": "+getMemory());

	}

	public static String getMemory() {
		return tidyFileSize(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())+" / "+
		tidyFileSize(Runtime.getRuntime().totalMemory())+" / "+
		tidyFileSize(Runtime.getRuntime().maxMemory());
	}
}
