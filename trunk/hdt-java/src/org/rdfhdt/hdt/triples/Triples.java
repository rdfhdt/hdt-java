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

package org.rdfhdt.hdt.triples;

import org.rdfhdt.hdt.header.Header;
import org.rdfhdt.hdt.iterator.IteratorTripleID;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInformation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface for Triples implementation.
 * 
 * This is a static (as in read-only) interface. For dynamic(write) behaviour
 * have a look at {@link ModifiableTriples}
 * 
 */
public interface Triples {
	/**
	 * Iterates over all triples. Equivalent to this.search(new TripleID());
	 * @return
	 */
	public abstract IteratorTripleID searchAll();

	
	/**
	 * Iterates over all triples that match the pattern.
	 * 
	 * @param pattern
	 *            The pattern to match against
	 * @return IteratorTripleID 
	 * 
	 */
	public abstract IteratorTripleID search(TripleID pattern);
	
	/**
	 * Calculates the cost to retrieve a specific pattern
	 * 
	 * @param pattern
	 *            The pattern to match against
	 * @return float
	 */
	public abstract float cost(TripleID pattern);

	/**
	 * Returns the total number of triples
	 * 
	 * @return int
	 */
	public abstract long getNumberOfElements();

	/**
	 * Returns the size in bytes of the internal represenatation
	 * 
	 * @return int
	 */
	public abstract long size();

	/**
	 * Serializes the triples to an OutputStream
	 * 
	 * @param output
	 *            The OutputStream to save the triples to
	 */
	public abstract void save(OutputStream output, ControlInformation ci, ProgressListener listener) throws IOException;

	/**
	 * Loads the structure from an InputStream
	 * 
	 * @param input
	 *            The InputStream to load the file from
	 * @throws IOException
	 */
	public abstract void load(InputStream input, ControlInformation ci, ProgressListener listener) throws IOException;

	/**
	 * Generates the associated Index
	 * @param listener
	 */
	public abstract void generateIndex(ProgressListener listener);
	
	/**
	 * Loads the associated Index from an InputStream
	 * 
	 * @param input
	 *            The InputStream to load the index from
	 * @throws IOException
	 */
	public abstract void loadIndex(InputStream input, ControlInformation ci, ProgressListener listener) throws IOException;

	/**
	 * Saves the associated Index to an OutputStream
	 * 
	 * @param output
	 *            The OutputStream to save the index
	 * @throws IOException
	 */
	public abstract void saveIndex(OutputStream output, ControlInformation ci, ProgressListener listener) throws IOException;

	/**
	 * Loads triples from another Triples Structure
	 * 
	 * @param input
	 *            The ModifiableTriples input to load from
	 */
	public abstract void load(ModifiableTriples input, ProgressListener listener);
	
	/**
	 * Populates HDT Header with all information relevant to this Triples under a RDF root node.
	 * @param head
	 * @param rootNode
	 */
	public void populateHeader(Header head, String rootNode);
	
	/**
	 * Returns a unique identifier of this Triples Implementation
	 * @return
	 */
	public String getType();
}