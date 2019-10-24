/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/triples/impl/TriplesList.java $
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

package org.rdfhdt.hdt.triples.impl;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;

import org.rdfhdt.hdt.dictionary.impl.DictionaryIDMapping;
import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.header.Header;
import org.rdfhdt.hdt.iterator.SequentialSearchIteratorTripleID;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInfo;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TempTriples;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleIDComparator;
import org.rdfhdt.hdt.triples.Triples;
import org.rdfhdt.hdt.util.RDFInfo;
import org.rdfhdt.hdt.util.io.CountInputStream;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.listener.ListenerUtil;


/**
 * Implementation of TempTriples using a List of TripleID.
 * 
 */
public class TriplesListLong implements TempTriples {

	/** The array to hold the triples */
	private ArrayList<TripleID> arrayOfTriples;

	/** The order of the triples */
	private TripleComponentOrder order;
	private long numValidTriples;

	private boolean sorted = false;

	/**
	 * Constructor, given an order to sort by
	 * 
	 * @param order
	 *            The order to sort by
	 */
	public TriplesListLong(HDTOptions specification) {

		//precise allocation of the array (minimal memory wasting)
		long numTriples = RDFInfo.getTriples(specification);
		numTriples = (numTriples>0)?numTriples:100;
		this.arrayOfTriples = new ArrayList<TripleID>((int)numTriples);

		//choosing starting(or default) component order
		String orderStr = specification.get("triplesOrder");
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
		} else {
			return false;
		}
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
		return this.getNumberOfElements()*TripleID.size();
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.Triples#save(java.io.OutputStream)
	 */
	@Override
	public void save(OutputStream output, ControlInfo controlInformation, ProgressListener listener) throws IOException {
		controlInformation.clear();
		controlInformation.setInt("numTriples", numValidTriples);
		controlInformation.setFormat(HDTVocabulary.TRIPLES_TYPE_TRIPLESLIST);
		controlInformation.setInt("order", order.ordinal());
		controlInformation.save(output);

		DataOutputStream dout = new DataOutputStream(output);
		int count = 0;
		for (TripleID triple : arrayOfTriples) {
			if(triple.isValid()) {
				// FIXME: writeLong ??
				dout.writeInt((int) triple.getSubject());
				dout.writeInt((int) triple.getPredicate());
				dout.writeInt((int) triple.getObject());
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
	public void load(InputStream input, ControlInfo controlInformation, ProgressListener listener) throws IOException {
		order = TripleComponentOrder.values()[(int)controlInformation.getInt("order")];
		long totalTriples = controlInformation.getInt("numTriples");

		int numRead=0;
		
		while(numRead<totalTriples) {
			arrayOfTriples.add(new TripleID(IOUtil.readLong(input), IOUtil.readLong(input), IOUtil.readLong(input)));
			numRead++;
			numValidTriples++;
			ListenerUtil.notifyCond(listener, "Loading TriplesList", numRead, totalTriples);
		}

		sorted = false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.Triples#load(hdt.triples.TempTriples)
	 */
	@Override
	public void load(TempTriples input, ProgressListener listener) {
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
	 * @see hdt.triples.TempTriples#insert(hdt.triples.TripleID[])
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
	 * @see hdt.triples.TempTriples#insert(int, int, int)
	 */
	@Override
	public boolean insert(long subject, long predicate, long object) {
		arrayOfTriples.add(new TripleID(subject,predicate,object));
		numValidTriples++;
		sorted = false;
		return true;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.TempTriples#delete(hdt.triples.TripleID[])
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
	 * @see hdt.triples.TempTriples#sort(datatypes.TripleComponentOrder)
	 */
	@Override
	public void sort(ProgressListener listener) {
		if(!sorted) {
			Collections.sort(arrayOfTriples, TripleIDComparator.getComparator(order));
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

		if(order==TripleComponentOrder.Unknown || !sorted) {
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
	public void loadIndex(InputStream input, ControlInfo ci, ProgressListener listener) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void saveIndex(OutputStream output, ControlInfo ci, ProgressListener listener) throws IOException {
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

		private TriplesListLong triplesList;
		private int pos;

		public TriplesListIterator(TriplesListLong triplesList) {
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
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public void mapFromFile(CountInputStream in, File f, ProgressListener listener) throws IOException {
		throw new NotImplementedException();
	}

	@Override
	public void mapIndex(CountInputStream input, File f, ControlInfo ci, ProgressListener listener) throws IOException {		
	}

	@Override
	public void replaceAllIds(DictionaryIDMapping mapSubj, DictionaryIDMapping mapPred, DictionaryIDMapping mapObj) {
		sorted = false;
		for(TripleID triple : arrayOfTriples) {
			triple.setAll(
					mapSubj.getNewID(triple.getSubject()-1),
					mapPred.getNewID(triple.getPredicate()-1),
					mapObj.getNewID(triple.getObject()-1)
				);
		}
	}
	
}
