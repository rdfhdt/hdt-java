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

package hdt.triples.impl;

import hdt.compact.array.ArrayFactory;
import hdt.compact.array.StaticArray;
import hdt.enums.TripleComponentOrder;
import hdt.enums.TripleComponentRole;
import hdt.hdt.HDTVocabulary;
import hdt.header.Header;
import hdt.iterator.IteratorTripleID;
import hdt.iterator.SequentialSearchIteratorTripleID;
import hdt.listener.ProgressListener;
import hdt.options.ControlInformation;
import hdt.options.HDTSpecification;
import hdt.triples.ModifiableTriples;
import hdt.triples.TripleID;
import hdt.triples.Triples;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;


/**
 * Basic implementation of Triples.
 * 
 */
public class PlainTriples implements Triples {
	HDTSpecification spec;
	
	protected StaticArray arrayX;
	protected StaticArray arrayZ;
	protected StaticArray arrayY;
	protected TripleComponentOrder order;

	public PlainTriples() {
		this(new HDTSpecification());
	}
	
	class ComponentIterator implements Iterator<Long> {
		private IteratorTripleID iterator;
		private TripleComponentRole role;

		public ComponentIterator(IteratorTripleID iterator, TripleComponentRole role) {
			this.iterator = iterator;
			this.role = role;
		}
		
		/* (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		@Override
		public Long next() {
			switch(role) {
			case SUBJECT:
				return new Long(iterator.next().getSubject());
			case PREDICATE:
				return new Long(iterator.next().getPredicate());
			case OBJECT:
				return new Long(iterator.next().getObject());
			}
			return null;
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#remove()
		 */
		@Override
		public void remove() {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	/**
	 * Basic constructor
	 * 
	 */
	public PlainTriples(HDTSpecification spec) {
		this.spec = spec;
		String orderStr = spec.get("triples.component.order");
		if(orderStr!=null) {
			order = TripleComponentOrder.valueOf(orderStr);
		}
		
		arrayX = ArrayFactory.createStream(spec.get("array.x"));
		arrayY = ArrayFactory.createStream(spec.get("array.y"));
		arrayZ = ArrayFactory.createStream(spec.get("array.z"));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.Triples#search(hdt.triples.TripleID)
	 */
	@Override
	public IteratorTripleID search(TripleID pattern) {
		return new SequentialSearchIteratorTripleID(pattern, new PlainTriplesIterator(this));
	}
	
	/* (non-Javadoc)
	 * @see hdt.triples.Triples#searchAll()
	 */
	@Override
	public IteratorTripleID searchAll() {
		return this.search(new TripleID());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.Triples#cost(hdt.triples.TripleID)
	 */
	@Override
	public float cost(TripleID triple) {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.Triples#getNumberOfElements()
	 */
	@Override
	public long getNumberOfElements() {
		return arrayX.getNumberOfElements();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.Triples#size()
	 */
	@Override
	public long size() {
		return arrayX.size()+arrayY.size()+arrayZ.size();
	}

	

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#save(java.io.OutputStream, hdt.ControlInformation, hdt.ProgressListener)
	 */
	@Override
	public void save(OutputStream output, ControlInformation ci, ProgressListener listener) throws IOException {
		ci.set("array.x", arrayX.getType());
		ci.set("array.y", arrayY.getType());
		ci.set("array.z", arrayZ.getType());
		ci.save(output);
		arrayX.save(output, listener);
		arrayY.save(output, listener);
		arrayZ.save(output, listener);
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#load(java.io.InputStream, hdt.ControlInformation, hdt.ProgressListener)
	 */
	@Override
	public void load(InputStream input, ControlInformation ci, ProgressListener listener) throws IOException {
		arrayX = ArrayFactory.createStream(spec.get("array.x"));
		arrayY = ArrayFactory.createStream(spec.get("array.y"));
		arrayZ = ArrayFactory.createStream(spec.get("array.z"));
		arrayX.load(input, listener);
		arrayY.load(input, listener);
		arrayZ.load(input, listener);
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#load(hdt.triples.ModifiableTriples, hdt.ProgressListener)
	 */
	@Override
	public void load(ModifiableTriples input, ProgressListener listener) {
		arrayX.add(new ComponentIterator(input.searchAll(), TripleComponentRole.SUBJECT));
		arrayY.add(new ComponentIterator(input.searchAll(), TripleComponentRole.PREDICATE));
		arrayZ.add(new ComponentIterator(input.searchAll(), TripleComponentRole.OBJECT));
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#populateHeader(hdt.header.Header, java.lang.String)
	 */
	@Override
	public void populateHeader(Header head, String rootNode) {
		
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#getType()
	 */
	@Override
	public String getType() {
		return HDTVocabulary.TRIPLES_TYPE_PLAIN;
	}

	@Override
	public void generateIndex(ProgressListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void loadIndex(InputStream input, ControlInformation ci,
			ProgressListener listener) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveIndex(OutputStream output, ControlInformation ci,
			ProgressListener listener) throws IOException {
		// TODO Auto-generated method stub
		
	}

}
