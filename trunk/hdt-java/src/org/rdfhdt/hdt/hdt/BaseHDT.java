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

package org.rdfhdt.hdt.hdt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.dictionary.DictionaryFactory;
import org.rdfhdt.hdt.dictionary.ModifiableDictionary;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.header.Header;
import org.rdfhdt.hdt.header.HeaderFactory;
import org.rdfhdt.hdt.iterator.DictionaryTranslateIterator;
import org.rdfhdt.hdt.iterator.IteratorTripleString;
import org.rdfhdt.hdt.listener.IntermediateListener;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.ModifiableTriples;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.Triples;
import org.rdfhdt.hdt.triples.TriplesFactory;
import org.rdfhdt.hdt.util.StopWatch;

/**
 * Basic implementation of HDT interface
 * 
 */
public class BaseHDT implements HDT {
	HDTSpecification spec;

	Header header;
	Dictionary dictionary;
	Triples triples;
	
	String hdtFileName;
	
	private void createComponents() {
		header = HeaderFactory.createHeader(spec);
        dictionary = DictionaryFactory.createDictionary(spec);
        triples = TriplesFactory.createTriples(spec);
	}
	
	/**
	 * @param specification
	 */
	protected BaseHDT(HDTSpecification specification) {
		this.spec = specification;

		createComponents();
	}
	
	@Override
	public void loadFromHDT(InputStream input, ProgressListener listener) throws IOException {
		ControlInformation ci = new ControlInformation();
		IntermediateListener iListener = new IntermediateListener(listener);
		
		// Load header
		ci.clear();
		ci.load(input);
		iListener.setRange(0, 5);
		header = HeaderFactory.createHeader(ci);
		header.load(input, ci, iListener);
		
		// Load dictionary
		ci.clear();
		ci.load(input);
		iListener.setRange(5, 60);
		dictionary = DictionaryFactory.createDictionary(ci);
		dictionary.load(input, ci, iListener);
		
		// Load Triples
		ci.clear();
		ci.load(input);
		iListener.setRange(60, 100);
		triples = TriplesFactory.createTriples(ci);
		triples.load(input, ci, iListener);
	}

	@Override
	public void loadFromHDT(String hdtFileName, ProgressListener listener)	throws IOException {
//		InputStream in = new GZIPInputStream(new FileInputStream(fileName));
		InputStream in = new BufferedInputStream(new FileInputStream(hdtFileName));
		loadFromHDT(in, listener);
		in.close();
		
		this.hdtFileName = hdtFileName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.HDT#saveToHDT(java.io.OutputStream)
	 */
	@Override
	public void saveToHDT(OutputStream output, ProgressListener listener) throws IOException {
		ControlInformation ci = new ControlInformation();
		IntermediateListener iListener = new IntermediateListener(listener);
		
		ci.clear();
		ci.setHeader(true);
		header.save(output, ci, iListener);
		
		ci.clear();
		ci.setDictionary(true);
		dictionary.save(output, ci, iListener);
		
		ci.clear();
		ci.setTriples(true);
		triples.save(output, ci, iListener);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.HDT#saveToHDT(java.io.OutputStream)
	 */
	@Override
	public void saveToHDT(String fileName, ProgressListener listener) throws IOException {
		OutputStream out = new BufferedOutputStream(new FileOutputStream(fileName));
		//OutputStream out = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)));
		saveToHDT(out, listener);
		out.close();
		
		this.hdtFileName = fileName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.HDT#search(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public IteratorTripleString search(CharSequence subject, CharSequence predicate, CharSequence object) throws NotFoundException {

		// Conversion from TripleString to TripleID
		TripleID triple = new TripleID(
				dictionary.stringToId(subject, TripleComponentRole.SUBJECT),
				dictionary.stringToId(predicate, TripleComponentRole.PREDICATE),
				dictionary.stringToId(object, TripleComponentRole.OBJECT)
			);
		
		if(triple.getSubject()==-1 || triple.getPredicate()==-1 || triple.getObject()==-1) {
			throw new NotFoundException("String not found in dictionary");
		}

		return new DictionaryTranslateIterator(triples.search(triple), dictionary);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.HDT#getHeader()
	 */
	@Override
	public Header getHeader() {
		return header;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.HDT#getDictionary()
	 */
	@Override
	public Dictionary getDictionary() {
		return dictionary;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.HDT#getTriples()
	 */
	@Override
	public Triples getTriples() {
		return triples;
	}
	
	/* (non-Javadoc)
	 * @see hdt.hdt.HDT#getSize()
	 */
	@Override
	public long size() {
		return dictionary.size()+triples.size();
	}

	/* (non-Javadoc)
	 * @see hdt.hdt.HDT#loadFromModifiableHDT(hdt.hdt.ModifiableHDT, hdt.listener.ProgressListener)
	 */
	@Override
	public void loadFromModifiableHDT(ModifiableHDT modHdt, ProgressListener listener) {
		// WARNING: The modifiable HDT must be reorganized before calling this method.
		
        // Get parts
        ModifiableTriples modifiableTriples = (ModifiableTriples) modHdt.getTriples();
        ModifiableDictionary modifiableDictionary = (ModifiableDictionary) modHdt.getDictionary();
        
        // Convert triples to final format
        if(triples.getClass().equals(modifiableTriples.getClass())) {
                triples = modifiableTriples;
        } else {
                StopWatch tripleConvTime = new StopWatch();
                triples.load(modifiableTriples, listener);
                System.out.println("Triples conversion time: "+tripleConvTime.stopAndShow());
        }
        
        // Convert dictionary to final format
        if(dictionary.getClass().equals(modifiableDictionary.getClass())) {
                dictionary = modifiableDictionary;
        } else {
                StopWatch dictConvTime = new StopWatch();
                dictionary.load(modifiableDictionary, listener);
                System.out.println("Dictionary conversion time: "+dictConvTime.stopAndShow());
        }       
	}
	
	/* (non-Javadoc)
	 * @see hdt.hdt.HDT#generateIndex(hdt.listener.ProgressListener)
	 */
	@Override
	public void loadOrCreateIndex(ProgressListener listener) {
		String indexName = hdtFileName+".jindex";
		ControlInformation ci = new ControlInformation();
		
		try {
			FileInputStream in = new FileInputStream(indexName);	
			ci.load(in);
			triples.loadIndex(in, ci, listener);
			in.close();
		} catch (IOException e) {
			// GENERATE
			triples.generateIndex(listener);
			
			// SAVE
			try {
				FileOutputStream out = new FileOutputStream(indexName);
				ci.clear();
				triples.saveIndex(out, ci, listener);
				out.close();
			} catch (IOException e2) {
				
			}
		} 
	}

}
