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
import java.util.Set;
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

	@Override
	public IteratorTripleID search(TripleID pattern) {
		String patternStr = pattern.getPatternString();
		if(patternStr.equals("???")) {
			return new TriplesSetIterator(triples, order, getNumberOfElements());
		} else {
			return new SequentialSearchIteratorTripleID(pattern, new TriplesSetIterator(triples, order, getNumberOfElements()));
		}
	}

	@Override
	public IteratorTripleID searchAll() {
		TripleID all = new TripleID(0,0,0);
		return this.search(all);
	}

	@Override
	public long getNumberOfElements() {
		return numValidTriples;
	}

	@Override
	public long size() {
		return this.getNumberOfElements()*24;
	}

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
		if (this.order.equals(order))
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

	@Override
	public boolean insert(TripleID... triples) {
		for (TripleID triple : triples) {
			this.triples.add(new TripleID(triple));
			numValidTriples++;
		}
		return true;
	}

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

		if (!triples.remove(triple))
			return false;
		triple.setAll(subj, pred, obj);
		return triples.add(triple);
	}

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

	/**
	 * This method does nothing because the triples in this implementation
	 * are always sorted by the current ordering.
	 * To resort them (not recomended) use "setOrder".
	 */
	@Override
	public void sort(ProgressListener listener) {
		//do nothing because a TreeSet is always sorted
	}

	/**
	 * This method does nothing because the triples in this implementation
	 * are always unique
	 */
	@Override
	public void removeDuplicates(ProgressListener listener) {
		//do nothing because a Set can't have duplicates anyway...
	}

	@Override
	public String toString() {
		return "TriplesList [" + triples + "\n order=" + order + "]";
	}

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
	public static class TriplesSetIterator implements IteratorTripleID {

		private Set<TripleID> triples;
		private TripleComponentOrder order;
		private long numElem;
		
		private Iterator<TripleID> iterator;

		public TriplesSetIterator(Set<TripleID> triples, TripleComponentOrder order, long numElem) {
			this.triples = triples;
			this.order = order;
			this.numElem = numElem;
			this.iterator = triples.iterator();
		}
		

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public TripleID next() {
			return iterator.next();
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

		@Override
		public void goToStart() {
			iterator = triples.iterator();
		}

		@Override
		public long estimatedNumResults() {
			return numElem;
		}

		@Override
		public ResultEstimationType numResultEstimation() {
			return ResultEstimationType.EXACT;
		}

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

		@Override
		public TripleComponentOrder getOrder() {
			return order;
		}

		@Override
		public void remove() {
			iterator.remove();
		}
	}

}
