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

package org.rdfhdt.hdt.triples.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;

import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.header.Header;
import org.rdfhdt.hdt.iterator.IteratorTripleID;
import org.rdfhdt.hdt.iterator.SequentialSearchIteratorTripleID;
import org.rdfhdt.hdt.listener.ListenerUtil;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.ModifiableTriples;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleIDComparator;
import org.rdfhdt.hdt.triples.Triples;
import org.rdfhdt.hdt.util.RDFInfo;


/**
 * Implementation of ModifiableTriples. This is a iterable structure.
 * 
 */
public class TriplesList implements ModifiableTriples {

	/** The array to hold the triples */
	private ArrayList<TripleID> arrayOfTriples;

	/** The order of the triples */
	private TripleComponentOrder order;
	private int numValidTriples;

	private boolean sorted = false;

	/**
	 * Constructor, given an order to sort by
	 * 
	 * @param order
	 *            The order to sort by
	 */
	public TriplesList(HDTSpecification specification) {

		//precise allocation of the array (minimal memory wasting)
		Long numTriples = RDFInfo.getLines(specification);
		numTriples = (numTriples!=null)?numTriples:100;
		this.arrayOfTriples = new ArrayList<TripleID>(numTriples.intValue());

		//choosing starting(or default) component order
		String orderStr = specification.get("triples.component.order");
		if(orderStr==null) {
			orderStr = "SPO";
		}
		this.order = TripleComponentOrder.valueOf(orderStr);

		this.numValidTriples = 0;
	}

	/**
	 * A method for setting the size of the arrayList (so no reallocation occurs).
	 * If not empty does nothing and returns false.
	 */
	public boolean reallocateIfEmpty(int numTriples){
		if (arrayOfTriples.isEmpty()) {
			arrayOfTriples = new ArrayList<TripleID>(numTriples);
			return true;
		} else
			return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.Triples#search(hdt.triples.TripleID)
	 */
	@Override
	public IteratorTripleID search(TripleID pattern) {
		String patternStr = pattern.getPatternString();
		if(patternStr.equals("???")) {
			return new TriplesListIterator(this);
		} else {
			return new SequentialSearchIteratorTripleID(pattern, new TriplesListIterator(this));
		}
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#searchAll()
	 */
	@Override
	public IteratorTripleID searchAll() {
		TripleID all = new TripleID(0,0,0);
		return this.search(all);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.Triples#getNumberOfElements()
	 */
	@Override
	public long getNumberOfElements() {
		return numValidTriples;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.Triples#size()
	 */
	@Override
	public long size() {
		return this.getNumberOfElements()*TripleID.sizeOf();
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.Triples#save(java.io.OutputStream)
	 */
	@Override
	public void save(OutputStream output, ControlInformation controlInformation, ProgressListener listener) throws IOException {
		controlInformation.clear();
		controlInformation.setInt("numTriples", numValidTriples);
		controlInformation.set("codification", HDTVocabulary.TRIPLES_TYPE_TRIPLESLIST);
		controlInformation.setInt("componentOrder", order.ordinal());
		controlInformation.save(output);

		DataOutputStream dout = new DataOutputStream(output);
		int count = 0;
		for (TripleID triple : arrayOfTriples) {
			if(triple.isValid()) {
				dout.writeInt(triple.getSubject());
				dout.writeInt(triple.getPredicate());
				dout.writeInt(triple.getObject());
				ListenerUtil.notifyCond(listener, "Saving TriplesList", count, arrayOfTriples.size());
			}
			count++;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.Triples#load(java.io.InputStream)
	 */
	@Override
	public void load(InputStream input, ControlInformation controlInformation, ProgressListener listener) throws IOException {
		order = TripleComponentOrder.values()[(int)controlInformation.getInt("componentOrder")];
		long totalTriples = controlInformation.getInt("numTriples");

		int numRead=0;
		DataInputStream din = new DataInputStream(input);

		while(numRead<totalTriples) {
			arrayOfTriples.add(new TripleID(din.readInt(), din.readInt(), din.readInt()));
			numRead++;
			numValidTriples++;
			ListenerUtil.notifyCond(listener, "Loading TriplesList", numRead, totalTriples);
		}

		sorted = false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.Triples#load(hdt.triples.ModifiableTriples)
	 */
	@Override
	public void load(ModifiableTriples input, ProgressListener listener) {
		IteratorTripleID iterator = input.searchAll();
		while (iterator.hasNext()) {
			arrayOfTriples.add(iterator.next());
			numValidTriples++;
		}

		sorted = false;
	}

	/**
	 * @param order
	 *            the order to set
	 */
	public void setOrder(TripleComponentOrder order) {
		if (this.order.equals(order))
			return;
		this.order = order;
		sorted = false;
	}

	@Override
	public TripleComponentOrder getOrder() {
		return order;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.ModifiableTriples#insert(hdt.triples.TripleID[])
	 */
	@Override
	public boolean insert(TripleID... triples) {
		for (TripleID triple : triples) {
			arrayOfTriples.add(new TripleID(triple));
			numValidTriples++;
		}
		sorted = false;
		return true;
	}

	/* (non-Javadoc)
	 * @see hdt.triples.ModifiableTriples#insert(int, int, int)
	 */
	@Override
	public boolean insert(int subject, int predicate, int object) {
		arrayOfTriples.add(new TripleID(subject,predicate,object));
		numValidTriples++;
		sorted = false;
		return true;
	}

	@Override
	public boolean update(TripleID triple, int subj, int pred, int obj) {
		if (triple==null)
			return false;

		triple.setAll(subj, pred, obj);
		sorted = false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.ModifiableTriples#delete(hdt.triples.TripleID[])
	 */
	@Override
	public boolean remove(TripleID... patterns) {
		boolean removed = false;
		for(TripleID triple : arrayOfTriples){
			for(TripleID pattern : patterns) {
				if(triple.match(pattern)) {
					triple.clear();
					removed = true;
					numValidTriples--;
					break;
				}
			}
		}

		return removed;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.ModifiableTriples#sort(datatypes.TripleComponentOrder)
	 */
	@Override
	public void sort(ProgressListener listener) {
		if(!sorted) {
			Collections.sort(arrayOfTriples, new TripleIDComparator(order));
		}
		sorted = true;
	}

	/**
	 * If called while triples not sorted nothing will happen!
	 */
	@Override
	public void removeDuplicates(ProgressListener listener) {
		if(arrayOfTriples.size()<=1 || !sorted) {
			return;
		}

		if(order==TripleComponentOrder.Unknown) {
			throw new IllegalArgumentException("Cannot remove duplicates unless sorted");
		}

		int j = 0;

		for(int i=1; i<arrayOfTriples.size(); i++) {
			if(arrayOfTriples.get(i).compareTo(arrayOfTriples.get(j))!=0) {
				j++;
				arrayOfTriples.set(j, arrayOfTriples.get(i));
			}
			ListenerUtil.notifyCond(listener, "Removing duplicate triples", i, arrayOfTriples.size());
		}

		while(arrayOfTriples.size()>j+1) {
			arrayOfTriples.remove(arrayOfTriples.size()-1);
		}
		arrayOfTriples.trimToSize();
		numValidTriples = j+1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TriplesList [" + arrayOfTriples + "\n order=" + order + "]";
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#populateHeader(hdt.header.Header, java.lang.String)
	 */
	@Override
	public void populateHeader(Header header, String rootNode) {
		header.insert(rootNode, HDTVocabulary.TRIPLES_TYPE, HDTVocabulary.TRIPLES_TYPE_TRIPLESLIST);
		header.insert(rootNode, HDTVocabulary.TRIPLES_NUM_TRIPLES, getNumberOfElements() );
		header.insert(rootNode, HDTVocabulary.TRIPLES_ORDER, order.ordinal() );
	}

	public String getType() {
		return HDTVocabulary.TRIPLES_TYPE_TRIPLESLIST;
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

	@Override
	public void clear() {
		this.arrayOfTriples.clear();
		this.numValidTriples=0;
		this.order = TripleComponentOrder.Unknown;
		sorted = false;
	}

	@Override
	public void load(Triples triples, ProgressListener listener) {
		this.clear();
		IteratorTripleID it = triples.searchAll();
		while(it.hasNext()) {
			TripleID triple = it.next();
			this.insert(triple.getSubject(), triple.getPredicate(), triple.getObject());
		}
		sorted = false;
	}

	@Override
	public void close() throws IOException {

	}

	/**
	 * Iterator implementation to iterate over a TriplesList object
	 * 
	 * @author mario.arias
	 *
	 */
	public class TriplesListIterator implements IteratorTripleID {

		private TriplesList triplesList;
		private int pos;

		public TriplesListIterator(TriplesList triplesList) {
			this.triplesList = triplesList;
			this.pos = 0;
		}

		/* (non-Javadoc)
		 * @see hdt.iterator.IteratorTripleID#hasNext()
		 */
		@Override
		public boolean hasNext() {
			return pos<triplesList.getNumberOfElements();
		}

		/* (non-Javadoc)
		 * @see hdt.iterator.IteratorTripleID#next()
		 */
		@Override
		public TripleID next() {
			return triplesList.arrayOfTriples.get((int)pos++);
		}

		/* (non-Javadoc)
		 * @see hdt.iterator.IteratorTripleID#hasPrevious()
		 */
		@Override
		public boolean hasPrevious() {
			return pos>0;
		}

		/* (non-Javadoc)
		 * @see hdt.iterator.IteratorTripleID#previous()
		 */
		@Override
		public TripleID previous() {
			return triplesList.arrayOfTriples.get((int)--pos);
		}

		/* (non-Javadoc)
		 * @see hdt.iterator.IteratorTripleID#goToStart()
		 */
		@Override
		public void goToStart() {
			pos = 0;
		}

		/* (non-Javadoc)
		 * @see hdt.iterator.IteratorTripleID#estimatedNumResults()
		 */
		@Override
		public long estimatedNumResults() {
			return triplesList.getNumberOfElements();
		}

		/* (non-Javadoc)
		 * @see hdt.iterator.IteratorTripleID#numResultEstimation()
		 */
		@Override
		public ResultEstimationType numResultEstimation() {
			return ResultEstimationType.EXACT;
		}

		/* (non-Javadoc)
		 * @see hdt.iterator.IteratorTripleID#canGoTo()
		 */
		@Override
		public boolean canGoTo() {
			return true;
		}

		/* (non-Javadoc)
		 * @see hdt.iterator.IteratorTripleID#goTo(int)
		 */
		@Override
		public void goTo(long pos) {
			this.pos = (int)pos;
		}

		/* (non-Javadoc)
		 * @see hdt.iterator.IteratorTripleID#getOrder()
		 */
		@Override
		public TripleComponentOrder getOrder() {
			return triplesList.getOrder();
		}

		@Override
		public void remove() {
			// TODO Auto-generated method stub
			throw new NotImplementedException();
		}
	}

}
