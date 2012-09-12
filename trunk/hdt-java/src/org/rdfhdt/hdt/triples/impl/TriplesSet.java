/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/triples/impl/TriplesList.java $
 * Revision: $Rev: 45 $
 * Last modified: $Date: 2012-07-31 12:00:35 +0100 (Tue, 31 Jul 2012) $
 * Last modified by: $Author: mario.arias $
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
import java.util.Iterator;
import java.util.TreeSet;

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

/**
 * This in-memory implementation of ModifiableTriples is better memorywise 
 * and timewise than TriplesList because it uses TreeSet instead of ArrayList for
 * holding TripleID's in memory. Because of that it is always sorted and takes only
 * as much memory as it needs and removing duplicates is not necessary.
 * 
 * HOWEVER...
 * 1) If it needs to be resorted then the comparative gain to TriplesList becomes
 * negligable because then it also needs 2x more memory than resulting set.
 * 2) It's iterator (TriplesSetIterator) cannot go backwards (hasPrevious, previous)
 * and hop wherever (goTo)
 * 3) It's update method CAN NOT be used while iterating over the triples!!!
 * 
 * Because of 3) this implementation CAN NOT be used in the one-pass way of
 * operation (reading RDF once and then rearranging the triples in memory)
 * 
 * @author Eugen Rozic
 *
 */
public class TriplesSet implements ModifiableTriples {

	private HDTSpecification specs;

	/** The array to hold the triples */
	private TreeSet<TripleID> triples;

	/** The order of the triples */
	private TripleComponentOrder order;
	private int numValidTriples;

	/**
	 * Constructor, given an order to sort by
	 * 
	 * @param order
	 *            The order to sort by
	 */
	public TriplesSet(HDTSpecification specification) {

		this.specs = specification;

		String orderStr = specs.get("triples.component.order");
		if(orderStr==null) {
			orderStr = "SPO";
		}
		this.order = TripleComponentOrder.valueOf(orderStr);

		this.triples = new TreeSet<TripleID>(new TripleIDComparator(this.order));

		this.numValidTriples = 0;
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
			return new TriplesSetIterator(this);
		} else {
			return new SequentialSearchIteratorTripleID(pattern, new TriplesSetIterator(this));
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
	 * @see hdt.triples.Triples#cost(hdt.triples.TripleID)
	 */
	@Override
	public float cost(TripleID triple) {
		// TODO Schedule a meeting to discuss how to do this
		return 0;
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
		return this.getNumberOfElements()*24;
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
		for (TripleID triple : triples) {
			if(triple.isValid()) {
				dout.writeInt(triple.getSubject());
				dout.writeInt(triple.getPredicate());
				dout.writeInt(triple.getObject());
				ListenerUtil.notifyCond(listener, "Saving TriplesList", count, triples.size());
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
		this.setOrder(TripleComponentOrder.values()[(int)controlInformation.getInt("componentOrder")]);

		long totalTriples = controlInformation.getInt("numTriples");

		int numRead=0;
		DataInputStream din = new DataInputStream(input);

		while(numRead<totalTriples) {
			triples.add(new TripleID(din.readInt(), din.readInt(), din.readInt()));
			numRead++;
			numValidTriples++;
			ListenerUtil.notifyCond(listener, "Loading TriplesList", numRead, totalTriples);
		}
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
			this.triples.add(iterator.next());
			numValidTriples++;
		}
	}

	@Override
	public void load(Triples triples, ProgressListener listener) {
		this.clear();
		IteratorTripleID it = triples.searchAll();
		while(it.hasNext()) {
			this.triples.add(it.next());
			numValidTriples++;
		}
	}

	/**
	 * Sets the order of triples.
	 * In this implementation this causes an automatic re-sort if the order
	 * changes (the sort method does nothing), so BE CAREFUL with it!
	 * (that's why it's deprecated, the implementation is perfectly fine :)
	 */
	@Deprecated
	public void setOrder(TripleComponentOrder order) {
		if (this.order==order)
			return;
		//if order has changed then copy all triples with a different ordering - O(n*logn) time, 2x memory (same as would be with Collection.sort and an ArrayList)
		TreeSet<TripleID> newTriples = new TreeSet<TripleID>(new TripleIDComparator(order));
		newTriples.addAll(triples);
		triples = newTriples;
		this.order = order;
	}

	@Override
	public TripleComponentOrder getOrder() {
		return this.order;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.ModifiableTriples#insert(hdt.triples.TripleID[])
	 */
	@Override
	public boolean insert(TripleID... triples) {
		for (TripleID triple : triples) {
			this.triples.add(new TripleID(triple));
			numValidTriples++;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see hdt.triples.ModifiableTriples#insert(int, int, int)
	 */
	@Override
	public boolean insert(int subject, int predicate, int object) {
		triples.add(new TripleID(subject,predicate,object));
		numValidTriples++;
		return true;
	}

	/**
	 * This method DOES NOT work if used during iterating through the set!!!
	 * (meaning if the "search" method of this class was used or a TriplesSetIterator
	 * manually constructed)
	 * 
	 * This is because the method removes the triple and then adds it back in the
	 * set (no other way of keeping it sorted).
	 * 
	 * For this reason this implementation of ModifiableTriples is NOT TO BE USED
	 * in the one-pass way of operation.
	 * 
	 */
	@Deprecated
	@Override
	public boolean update(TripleID triple, int subj, int pred, int obj) {
		if (triple==null)
			return false;

		triples.remove(triple);
		triple.setAll(subj, pred, obj);
		return triples.add(triple);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.ModifiableTriples#delete(hdt.triples.TripleID[])
	 */
	@Override
	public boolean remove(TripleID... patterns) {
		boolean removed = false;
		Iterator<TripleID> iter = triples.iterator();

		while (iter.hasNext()){
			TripleID triple = iter.next();
			for(TripleID pattern : patterns) {
				if(triple.match(pattern)) {
					iter.remove();
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
		//do nothing because a TreeSet is always sorted
	}

	/* (non-Javadoc)
	 * @see hdt.triples.ModifiableTriples#removeDuplicates(hdt.ProgressListener)
	 */
	@Override
	public void removeDuplicates(ProgressListener listener) {
		//do nothing because a Set can't have duplicates anyway...
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TriplesList [" + triples + "\n order=" + order + "]";
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
		return HDTVocabulary.TRIPLES_TYPE_TRIPLESSET;
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
		this.triples.clear();
		this.numValidTriples=0;
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
	public class TriplesSetIterator implements IteratorTripleID {

		private TriplesSet triplesSet;
		private Iterator<TripleID> triplesSetIterator;

		public TriplesSetIterator(TriplesSet triplesSet) {
			this.triplesSet = triplesSet;
			this.triplesSetIterator = triplesSet.triples.iterator();
		}

		/* (non-Javadoc)
		 * @see hdt.iterator.IteratorTripleID#hasNext()
		 */
		@Override
		public boolean hasNext() {
			return triplesSetIterator.hasNext();
		}

		/* (non-Javadoc)
		 * @see hdt.iterator.IteratorTripleID#next()
		 */
		@Override
		public TripleID next() {
			return triplesSetIterator.next();
		}

		/**
		 * Not implemented in this implementation of ModifiableTriples because not available for TreeSet
		 */
		@Override
		public boolean hasPrevious() {
			throw new NotImplementedException();
		}

		/**
		 * Not implemented in this implementation of ModifiableTriples because not available for TreeSet
		 */
		@Override
		public TripleID previous() {
			throw new NotImplementedException();
		}

		/* (non-Javadoc)
		 * @see hdt.iterator.IteratorTripleID#goToStart()
		 */
		@Override
		public void goToStart() {
			triplesSetIterator = triplesSet.triples.iterator();
		}

		/* (non-Javadoc)
		 * @see hdt.iterator.IteratorTripleID#estimatedNumResults()
		 */
		@Override
		public long estimatedNumResults() {
			return triplesSet.getNumberOfElements();
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
			return false;
		}

		/**
		 * Not implemented in this implementation of ModifiableTriples because not available for TreeSet
		 */
		@Override
		public void goTo(long pos) {
			throw new NotImplementedException();
		}

		/* (non-Javadoc)
		 * @see hdt.iterator.IteratorTripleID#getOrder()
		 */
		@Override
		public TripleComponentOrder getOrder() {
			return triplesSet.getOrder();
		}

		/**
		 * Not implemented in this implementation of ModifiableTriples because not available for TreeSet
		 */
		@Override
		public void remove() {
			triplesSetIterator.remove();
		}
	}

}
