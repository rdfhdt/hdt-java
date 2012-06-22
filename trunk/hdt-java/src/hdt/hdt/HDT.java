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

package hdt.hdt;

import hdt.dictionary.Dictionary;
import hdt.enums.RDFNotation;
import hdt.exceptions.NotFoundException;
import hdt.exceptions.ParserException;
import hdt.header.Header;
import hdt.iterator.IteratorTripleString;
import hdt.listener.ProgressListener;
import hdt.rdf.RDFAccess;
import hdt.rdf.RDFSerializer;
import hdt.triples.Triples;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Interface that specifies the methods for a HDT implementation
 * 
 */
public interface HDT extends RDFAccess {

	/**
	 * Gets the header of the HDT
	 * 
	 * @return Header
	 */
	public Header getHeader();

	/**
	 * Gets the dictionary of the HDT
	 * 
	 * @return Dictionary
	 */
	public Dictionary getDictionary();

	/**
	 * Gets the triples of the HDT
	 * 
	 * @return Triples
	 */
	public Triples getTriples();
		
	/**
	 * Generates a new compact HDT file from a ModifiableHDT
	 * @param modHdt
	 * @param listener
	 */
	public void loadFromModifiableHDT(ModifiableHDT modHdt, ProgressListener listener);
	
	/**
	 * Loads a RDF from an InputStream to a HDT format
	 * 
	 * @param input
	 *            InputStream to read from
	 */
	public void loadFromRDF(String fileName, String baseUri, RDFNotation notation, ProgressListener listener) throws ParserException, IOException;

	/**
	 * Loads a HDT file
	 * 
	 * @param input
	 *            InputStream to read from
	 */
	public void loadFromHDT(InputStream input, ProgressListener listener) throws IOException;

	/**
	 * Loads a HDT file
	 * 
	 * @param input
	 *            InputStream to read from
	 */
	public void loadFromHDT(String fileName, ProgressListener listener) throws IOException;
	
	/**
	 * Saves to OutputStream in RDF format
	 * 
	 * @param output
	 *            The OutputStream to save to
	 * @param notation
	 *            RDFNotation of the output
	 */
	public void saveToRDF(RDFSerializer serializer, ProgressListener listener) throws IOException;

	/**
	 * Saves to OutputStream in HDT format
	 * 
	 * @param output
	 *            The OutputStream to save to
	 */
	public void saveToHDT(OutputStream output, ProgressListener listener) throws IOException;

	/**
	 * Generates any additional index needed to solve all triple patterns more efficiently
	 * 
	 * @param listener A listener to be notified of the progress.
	 */
	public void generateIndex(ProgressListener listener);

	/**
	 * Saves to OutputStream in HDT format
	 * 
	 * @param output
	 *            The OutputStream to save to
	 */
	public void saveToHDT(String fileName, ProgressListener listener) throws IOException;
	
	/**
	 * Resturns the size of the Data Structure in bytes.
	 * @return
	 */
	public long size();
	
	/**
	 * Search a pattern in a HDT
	 * 
	 * The null string acts as a wildcard. (i.e. search(null, null, null)
	 * returns all elements)
	 * 
	 * @param subject
	 *            The subject to search
	 * @param predicate
	 *            The predicate to search
	 * @param object
	 *            The object to search
	 * 
	 * @return Iterator of TripleStrings
	 * @throws NotFoundException 
	 */
	public IteratorTripleString search(CharSequence subject, CharSequence predicate, CharSequence object) throws NotFoundException;

}
