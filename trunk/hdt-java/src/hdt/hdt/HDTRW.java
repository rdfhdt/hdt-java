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
import hdt.dictionary.DictionaryFactory;
import hdt.dictionary.ModifiableDictionary;
import hdt.enums.RDFNotation;
import hdt.enums.TripleComponentOrder;
import hdt.enums.TripleComponentRole;
import hdt.exceptions.NotImplementedException;
import hdt.exceptions.ParserException;
import hdt.header.Header;
import hdt.iterator.DictionaryTranslateIterator;
import hdt.iterator.IteratorTripleString;
import hdt.listener.ProgressListener;
import hdt.options.HDTSpecification;
import hdt.rdf.RDFParserCallback;
import hdt.rdf.RDFParserFactory;
import hdt.rdf.RDFSerializer;
import hdt.triples.ModifiableTriples;
import hdt.triples.TripleID;
import hdt.triples.TripleString;
import hdt.triples.Triples;
import hdt.triples.TriplesFactory;
import hdt.util.StopWatch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author mario.arias
 *
 */
public class HDTRW implements ModifiableHDT {
	private Header header;
	private ModifiableDictionary dictionary;
	private ModifiableTriples triples;
	
	protected HDTRW(HDTSpecification spec) {
		dictionary = DictionaryFactory.createModifiableDictionary();
		triples = TriplesFactory.createModifiableTriples();
	}
	
	/* (non-Javadoc)
	 * @see hdt.hdt.HDT#getHeader()
	 */
	@Override
	public Header getHeader() {
		return header;
	}

	/* (non-Javadoc)
	 * @see hdt.hdt.HDT#getDictionary()
	 */
	@Override
	public Dictionary getDictionary() {
		return dictionary;
	}

	/* (non-Javadoc)
	 * @see hdt.hdt.HDT#getTriples()
	 */
	@Override
	public Triples getTriples() {
		return triples;
	}

	/* (non-Javadoc)
	 * @see hdt.hdt.HDT#loadFromRDF(java.lang.String, java.lang.String, hdt.enums.RDFNotation, hdt.listener.ProgressListener)
	 */
	@Override
	public void loadFromRDF(String filename, String baseUri, RDFNotation notation, ProgressListener listener)
			throws ParserException, IOException {
		RDFParserCallback parser = RDFParserFactory.getParserCallback(notation);

		// Import all triples
		StopWatch importSt = new StopWatch();
		dictionary.startProcessing();
		parser.doParse(filename, baseUri, notation, new TripleAppender(dictionary, triples));
		dictionary.endProcessing();
		System.out.println("Imported in: "+importSt.stopAndShow());
	}

	/* (non-Javadoc)
	 * @see hdt.hdt.HDT#loadFromHDT(java.io.InputStream, hdt.listener.ProgressListener)
	 */
	@Override
	public void loadFromHDT(InputStream input, ProgressListener listener) throws IOException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see hdt.hdt.HDT#loadFromHDT(java.lang.String, hdt.listener.ProgressListener)
	 */
	@Override
	public void loadFromHDT(String fileName, ProgressListener listener)	throws IOException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see hdt.hdt.HDT#saveToRDF(hdt.rdf.RDFSerializer, hdt.listener.ProgressListener)
	 */
	@Override
	public void saveToRDF(RDFSerializer serializer, ProgressListener listener) throws IOException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see hdt.hdt.HDT#saveToHDT(java.io.OutputStream, hdt.listener.ProgressListener)
	 */
	@Override
	public void saveToHDT(OutputStream output, ProgressListener listener) throws IOException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see hdt.hdt.HDT#saveToHDT(java.lang.String, hdt.listener.ProgressListener)
	 */
	@Override
	public void saveToHDT(String fileName, ProgressListener listener) throws IOException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see hdt.hdt.HDT#size()
	 */
	@Override
	public long size() {
		return dictionary.size()+triples.size();
	}


	/* (non-Javadoc)
	 * @see hdt.hdt.HDT#search(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public IteratorTripleString search(CharSequence subject, CharSequence predicate, CharSequence object) {

		// Conversion from TripleString to TripleID
		TripleID triple = new TripleID(
				dictionary.stringToId(subject, TripleComponentRole.SUBJECT),
				dictionary.stringToId(predicate, TripleComponentRole.PREDICATE),
				dictionary.stringToId(object, TripleComponentRole.OBJECT)
			);

		return new DictionaryTranslateIterator(triples.search(triple), dictionary);
	}

	/* (non-Javadoc)
	 * @see hdt.hdt.ModifiableHDT#insert(java.lang.CharSequence, java.lang.CharSequence, java.lang.CharSequence)
	 */
	@Override
	public void insert(CharSequence subject, CharSequence predicate, CharSequence object) {
		this.triples.insert(
				dictionary.insert(subject, TripleComponentRole.SUBJECT),
				dictionary.insert(predicate, TripleComponentRole.PREDICATE),
				dictionary.insert(object, TripleComponentRole.OBJECT)
				);
	}
	
	/* (non-Javadoc)
	 * @see hdt.hdt.ModifiableHDT#insert(hdt.triples.TripleString[])
	 */
	@Override
	public void insert(IteratorTripleString newTriples) {
		while(newTriples.hasNext()) {
			TripleString triple = newTriples.next();
			this.insert(triple.getSubject(), triple.getPredicate(), triple.getObject());
		}
	}

	/* (non-Javadoc)
	 * @see hdt.hdt.ModifiableHDT#remove(hdt.triples.TripleString[])
	 */
	@Override
	public void remove(IteratorTripleString triples) {
		// FIXME: Removing from triples is easy, but how to know if the dictionary entry should be removed??
		throw new NotImplementedException();
	}

	/* (non-Javadoc)
	 * @see hdt.hdt.ModifiableHDT#remove(java.lang.CharSequence, java.lang.CharSequence, java.lang.CharSequence)
	 */
	@Override
	public void remove(CharSequence subject, CharSequence predicate, CharSequence object) {
		// FIXME: Removing from triples is easy, but how to know if the dictionary entry should be removed??
		throw new NotImplementedException();
	}

	public void reorganize(HDTSpecification spec, ProgressListener listener) {
		// Reorganize dictionary and update IDs on Triples accordingly.
		StopWatch reorgStp = new StopWatch();
		dictionary.reorganize(triples);
		System.out.println("Reorganized in "+reorgStp.stopAndShow());

		// Reorganize triples.
		String orderStr = spec.get("triples.component.order");
		if(orderStr==null) {
			orderStr = "SPO";
		}
		StopWatch sortDupTime = new StopWatch();
		triples.sort(TripleComponentOrder.valueOf(orderStr), listener);
		triples.removeDuplicates(listener);
		System.out.println("Sort and duplicates time: "+sortDupTime.stopAndShow());
	}

	/* (non-Javadoc)
	 * @see hdt.hdt.HDT#loadFromModifiableHDT(hdt.hdt.ModifiableHDT, hdt.listener.ProgressListener)
	 */
	@Override
	public void loadFromModifiableHDT(ModifiableHDT modHdt,	ProgressListener listener) {
		
	}

	@Override
	public void loadOrCreateIndex(ProgressListener listener) {
		
	}

	@Override
	public void clear() {
		dictionary.clear();
		triples.clear();
	}

}
