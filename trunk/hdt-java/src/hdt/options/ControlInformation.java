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

package hdt.options;

import hdt.util.io.IOUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

/**
 * @author mario.arias
 *
 */
public class ControlInformation extends HDTOptionsBase {
	private short version=0;
	private short components=0;
	
	private static byte TRIPLES_BIT = 1;
	private static byte DICTIONARY_BIT = 2;
	private static byte HEADER_BIT = 4;
	private static byte INDEX_BIT = 8;
	
	public ControlInformation() {
		super();
	}
	
	public short getVersion() {
		return version;
	}
	
	public void setVersion(short version) {
		this.version = version;
	}
	
	public boolean getHeader() {
		return (this.components & HEADER_BIT) != 0;
	}
	
	public void setHeader(boolean head) {
		if(head) {
			this.components |= HEADER_BIT;
		} else {
			this.components &= ~HEADER_BIT;
		}		
	}

	public boolean getDictionary() {
		return (this.components & DICTIONARY_BIT) != 0;
	}

	public void setDictionary(boolean dict) {
		if(dict) {
			this.components |= DICTIONARY_BIT;
		} else {
			this.components &= ~DICTIONARY_BIT;
		}	
	}
	
	public boolean getTriples() {
		return (this.components & TRIPLES_BIT) != 0;
	}
	
	public void setTriples(boolean head) {
		if(head) {
			this.components |= TRIPLES_BIT;
		} else {
			this.components &= ~TRIPLES_BIT;
		}		
	}

	public boolean getIndex() {
		return (this.components & INDEX_BIT) != 0;
	}

	public void setIndex(boolean idx) {
		if(idx) {
			this.components |= INDEX_BIT;
		} else {
			this.components &= ~INDEX_BIT;
		}	
	}
	
	public void save(OutputStream output) throws IOException {
		DataOutputStream dout = new DataOutputStream(output);
		IOUtil.writeLine(dout, "$HDT");
		
		dout.writeShort(version);
		dout.writeShort(components);
		
		for (Enumeration<Object> e = properties.keys(); e.hasMoreElements();) {
			String key = (String) e.nextElement();
			
			IOUtil.writeLine(dout, key+':'+properties.getProperty(key)+";\n");
		}
		IOUtil.writeLine(dout, "$END\n");
	}
	
	public void load(InputStream input) throws IOException {
        DataInputStream din = new DataInputStream(input);
        
        String magic = IOUtil.readChars(din, 4);
        if(!magic.equals("$HDT")) {
        	 throw new IOException("Non-HDT Section");
        }
 
        version = din.readShort();
        components = din.readShort();
        
        String line;
        StringBuilder out = new StringBuilder();
        while((line = IOUtil.readLine(din, '\n'))!=null) {
//        	System.out.println("LINE: "+line);
        	out.append(line);
        	if(line.equals("$END")) {
        		break;
        	}
        }
        
        // CUT   
        for(String item : out.toString().split(";")) {
        	if(!item.equals("$END")) {
        		int pos = item.indexOf(':');
        		if(pos!=-1) {
        			String property = item.substring(0, pos);
        			String value = item.substring(pos+1);
        			properties.put(property, value);
        		}
        	}
        }
	}
}
