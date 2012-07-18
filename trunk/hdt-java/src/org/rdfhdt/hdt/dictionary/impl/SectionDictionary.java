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

import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.header.Header;
import org.rdfhdt.hdt.listener.IntermediateListener;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.options.HDTSpecification;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * @author mario.arias
 *
 */
public class SectionDictionary extends BaseDictionary {
	
	/**
	 * 
	 */
	public SectionDictionary(HDTSpecification spec) {
		super(spec);
		// FIXME: Read type from spec.
		subjects = new DictionarySectionPFC(spec);
		predicates = new DictionarySectionPFC(spec);
		objects = new DictionarySectionPFC(spec);
		shared = new DictionarySectionPFC(spec);
	}
	
	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#load(hdt.dictionary.Dictionary)
	 */
	@Override
	public void load(Dictionary other, ProgressListener listener) {
		if(other instanceof BaseDictionary) {
			BaseDictionary baseDict = (BaseDictionary) other;
			
			IntermediateListener iListener = new IntermediateListener(listener);
			subjects.load(baseDict.subjects, iListener);
			predicates.load(baseDict.predicates, iListener);
			objects.load(baseDict.objects, iListener);
			shared.load(baseDict.shared, iListener);
		} else {
			throw new NotImplementedException();
		}
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#save(java.io.OutputStream, hdt.ControlInformation, hdt.ProgressListener)
	 */
	@Override
	public void save(OutputStream output, ControlInformation ci, ProgressListener listener) throws IOException {
		ci.set("codification", HDTVocabulary.DICTIONARY_TYPE_PFC);
		ci.set("format", "text/plain");
		ci.setInt("$mapping", this.mapping.ordinal());
		ci.setInt("$elements", this.getNumberOfElements());
		ci.save(output);
		
		IntermediateListener iListener = new IntermediateListener(listener);
		shared.save(output, iListener);
		subjects.save(output, iListener);
		predicates.save(output, iListener);
		objects.save(output, iListener);
		
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#load(java.io.InputStream)
	 */
	@Override
	public void load(InputStream input, ControlInformation ci, ProgressListener listener) throws IOException {
		int mappingVal = (int)ci.getInt("$mapping");
		if(mappingVal>=Mapping.values().length) {
			throw new IllegalArgumentException("Mapping out of bounds");
		}
		this.mapping = Mapping.values()[mappingVal];
	
		IntermediateListener iListener = new IntermediateListener(listener);
		
		shared = DictionarySectionFactory.createInstance(input, iListener);
		shared.load(input, iListener);
				
		subjects = DictionarySectionFactory.createInstance(input, iListener);
		subjects.load(input, iListener);
		
		predicates = DictionarySectionFactory.createInstance(input, iListener);
		predicates.load(input, iListener);
		
		objects = DictionarySectionFactory.createInstance(input, iListener);
		objects.load(input,iListener);
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#populateHeader(hdt.header.Header, java.lang.String)
	 */
	@Override
	public void populateHeader(Header header, String rootNode) {

	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#getType()
	 */
	@Override
	public String getType() {
		return HDTVocabulary.DICTIONARY_TYPE_PFC;
	}

}
