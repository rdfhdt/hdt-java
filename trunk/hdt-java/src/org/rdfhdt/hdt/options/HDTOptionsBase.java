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

package org.rdfhdt.hdt.options;

import java.util.Properties;

/**
 * @author mario.arias
 *
 */
public class HDTOptionsBase implements HDTOptions {
	/** The properties are stored here */
	Properties properties;

	/**
	 * 
	 */
	public HDTOptionsBase() {
		properties = new Properties();
	}
	
	/**
	 * Gets a property value from a property key
	 * 
	 * @param key
	 * @return
	 */
	public String get(String key) {
		return properties.getProperty(key);
	}
	
	public void set(String key, String value) {
		properties.setProperty(key, value);
	}
	
	public void setOptions(String options) {
		for(String item : options.split(";")) {
			int pos = item.indexOf(':');
			if(pos!=-1) {
				String property = item.substring(0, pos);
				String value = item.substring(pos+1);
				properties.setProperty(property, value);
			}
		}
	}

	/**
	 * @param string
	 * @return
	 */
	public long getInt(String string) {
		String val = properties.getProperty(string.trim());
		if(val!=null) {
			return Long.parseLong(val);
		}
		return 0;
	}
	
	public void setInt(String key, long value) {
		properties.setProperty(key, Long.toString(value));
	}
	
	/**
	 * A method for getting a property denoting a size in bytes (like size of cache for example)
	 * 
	 * If the property is not set the method returns -1, else it checks if it ends with a
	 * k, K, m, M, g or G and multiplies the number before with the apropriate power of 2 before
	 * returning it.
	 */
	public long getBytesProperty(String string) {
		String property = properties.getProperty(string);
		if (property==null || property.equals(""))
			return -1;
		
		property = property.trim();
		char lastChar = property.charAt(property.length()-1);
		
		switch(lastChar){
		case ('k'):
		case ('K'):
			return Long.parseLong(property.substring(0, property.length()-1))*1024;
		case ('m'):
		case ('M'):
			return Long.parseLong(property.substring(0, property.length()-1))*1024*1024;
		case ('g'):
		case ('G'):
			return Long.parseLong(property.substring(0, property.length()-1))*1024*1024*1024;
		default:
			return Long.parseLong(property);
		}
	}
	
	public void clear() {
		properties.clear();
	}
}
