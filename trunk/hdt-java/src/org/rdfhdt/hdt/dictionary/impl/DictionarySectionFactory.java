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

package org.rdfhdt.hdt.dictionary.impl;

import java.io.IOException;
import java.io.InputStream;

import org.rdfhdt.hdt.dictionary.DictionarySection;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTSpecification;

/**
 * @author mario.arias
 *
 */
public class DictionarySectionFactory {
	public static DictionarySection createInstance(InputStream input, ProgressListener listener) throws IOException {
		int dictType = input.read();
		switch(dictType) {
		case DictionarySectionHash.TYPE_INDEX:
			return new DictionarySectionHash(new HDTSpecification());
		case DictionarySectionPFC.TYPE_INDEX:
//			return new DictionarySectionPFC(new HDTSpecification());
			//return new DictionarySectionCache(new DictionarySectionPFC(new HDTSpecification()));
			return new DictionarySectionCache(new DictionarySectionPFCBig(new HDTSpecification()));
		}
		throw new IOException("DictionarySection implementation not available for id "+dictType);
	}
	
	public static DictionarySection loadFrom(InputStream input, ProgressListener listener) throws IOException {
		int dictType = input.read();
		
		DictionarySection section=null;
		
		switch(dictType) {
		case DictionarySectionHash.TYPE_INDEX:
			section = new DictionarySectionHash(new HDTSpecification());
			section.load(input, listener);
			return section;
		case DictionarySectionPFC.TYPE_INDEX:
			try{
				// First try load using the standard PFC 
				section = new DictionarySectionPFC(new HDTSpecification());
				section.load(input, listener);
			} catch (IllegalArgumentException e) {
				// The PFC Could not load the file because it is too big, use PFCBig
				section = new DictionarySectionPFCBig(new HDTSpecification());
				section.load(input, listener);
			}
			return new DictionarySectionCache(section);
		}
		throw new IOException("DictionarySection implementation not available for id "+dictType);
	}
}
