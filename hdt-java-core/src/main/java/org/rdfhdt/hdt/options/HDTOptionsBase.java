/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/options/HDTOptionsBase.java $
 * Revision: $Rev: 191 $
 * Last modified: $Date: 2013-03-03 11:41:43 +0000 (dom, 03 mar 2013) $
 * Last modified by: $Author: mario.arias $
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 3.0 of the License.
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
    final Properties properties;

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
	@Override
    public String get(String key) {
		return properties.getProperty(key);
	}
	
	@Override
    public void set(String key, String value) {
		properties.setProperty(key, value);
	}
	
	@Override
    public void setOptions(String options) {
		for(String item : options.split(";")) {
			int pos = item.indexOf('=');
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
	@Override
    public long getInt(String string) {
		String val = properties.getProperty(string.trim());
		if(val!=null) {
			return Long.parseLong(val);
		}
		return 0;
	}
	
	@Override
    public void setInt(String key, long value) {
		properties.setProperty(key, Long.toString(value));
	}
	

	
	@Override
    public void clear() {
		properties.clear();
	}
}
