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

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.SortedSet;

import org.apache.jdbm.DB;
import org.apache.jdbm.DBMaker;
import org.apache.jdbm.Serializer;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.header.Header;
import org.rdfhdt.hdt.iterator.SequentialSearchIteratorTripleID;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInfo;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TempTriples;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleIDComparator;
import org.rdfhdt.hdt.triples.Triples;
import org.rdfhdt.hdt.util.listener.ListenerUtil;
import org.rdfhdt.hdtdisk.util.CacheCalculator;

/**
 * This is an on-disc implementation of TempTriples based on a TreeMap,
 * Because of that it is always sorted and removing duplicates is not necessary.
 * 
 * HOWEVER...
 * 1) Resorting (changing order) implies making a new database and
 * populating it fully from the old before deleting the old one (very slow and inefficient)
 * 2) It's iterator (TriplesJDBMIterator) cannot go backwards (hasPrevious, previous)
 * and hop wherever (goTo)
 * 3) It's update method CAN NOT be used while iterating over the triples!!!
 * 
 * Because of 3) this implementation (like any other on-disc) CAN NOT be used
 * in the one-pass way of operation (reading RDF once and then rearranging the triples in memory)
 * 
 * @author Eugen Rozic
 *
 */
public class TriplesJDBM implements TempTriples {

	private HDTSpecification specs;

	/** database holding the triples */
	private DB db;
	/** The array to hold the triples */
	private SortedSet<TripleID> triples;
	/** base of the name of the collection in db */
	private String triplesSetName;
	/** version of the collection in db */
	private int triplesSetVersion;

	/** The order of the triples */
	private TripleComponentOrder order;
	private int numValidTriples;

	/**
	 * Constructor, given an order to sort by
	 * 
	 * @param order
	 *            The order to sort by
	 */
	public TriplesJDBM(HDTSpecification specification) {

		this.specs = specification;

		String orderStr = specs.get("triples.component.order");
		if(orderStr==null) {
			orderStr = "SPO";
		}
		this.order = TripleComponentOrder.valueOf(orderStr);

		setupDB();

		this.numValidTriples = 0;
	}

	/**method for setting up the db and the db-backed treeSet*/
	private void setupDB(){

		//FIXME read from specs...
		File folder = new File("DB");
		if (!folder.exists() && !folder.mkdir()){
			throw new RuntimeException("Unable to create DB folder...");
		}

		DBMaker dbMaker = DBMaker.openFile(folder.getPath()+"/triples");
		dbMaker.closeOnExit();
		dbMaker.deleteFilesAfterClose();
		dbMaker.disableTransactions(); //more performance
		dbMaker.setMRUCacheSize(CacheCalculator.getJDBMTriplesCache(specs));
		
		this.db = dbMaker.make();

		triplesSetName = "triplesSet";
		triplesSetVersion = 1;
		this.triples = db.<TripleID>createTreeSet(triplesSetName+triplesSetVersion, new TripleIDComparator(order), new TripleIDSerializer());

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
	 * For this reason this implementation of TempTriples is NOT TO BE USED
	 * in the one-pass way of operation.
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
		//do nothing because a SortedSet is always sorted
	}

	/**
	 * This method does nothing because the triples in this implementation
	 * are always unique
	 */
	@Override
	public void removeDuplicates(ProgressListener listener) {
		//do nothing because a Set can't have duplicates anyway...
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
		//if order has changed then copy all triples with a different ordering - O(n*logn) time, 2x memory (extra slow because of disc)
		int previousVersion = triplesSetVersion;
		triplesSetVersion++;
		SortedSet<TripleID> newTriples = db.<TripleID>createTreeSet(triplesSetName+triplesSetVersion, new TripleIDComparator(order), new TripleIDSerializer());
		newTriples.addAll(triples);
		triples = newTriples;
		//TODO not really necessary? takes time and bothers no one just lying there on disc, everything's going to be deleted on close anyway...
		db.deleteCollection(triplesSetName+previousVersion);
		//
		this.order = order;
	}

	@Override
	public TripleComponentOrder getOrder() {
		return this.order;
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
	public void clear() {
		triples.clear();
		numValidTriples = 0;
	}

	@Override
	public void close() throws IOException {
		db.close();
	}

	@Override
	public long getNumberOfElements() {
		return numValidTriples;
	}

	@Override
	public long size() {
		return this.getNumberOfElements()*TripleID.size();
	}

	@Override
	public void save(OutputStream output, ControlInfo controlInformation, ProgressListener listener) throws IOException {
		controlInformation.clear();
		controlInformation.setInt("numTriples", numValidTriples);
		controlInformation.setFormat(HDTVocabulary.TRIPLES_TYPE_TRIPLESLIST);
		controlInformation.setInt("order", order.ordinal());
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
	public void load(InputStream input, ControlInfo controlInformation, ProgressListener listener) throws IOException {
		this.setOrder(TripleComponentOrder.values()[(int)controlInformation.getInt("order")]);

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
	public void load(TempTriples input, ProgressListener listener) {
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
	public void populateHeader(Header header, String rootNode) {
		header.insert(rootNode, HDTVocabulary.TRIPLES_TYPE, HDTVocabulary.TRIPLES_TYPE_TRIPLESLIST);
		header.insert(rootNode, HDTVocabulary.TRIPLES_NUM_TRIPLES, getNumberOfElements() );
		header.insert(rootNode, HDTVocabulary.TRIPLES_ORDER, order.ordinal() );
	}

	public String getType() {
		return "TriplesJDBM";
	}
	
	/**
	 * A class implementing an interface defined in JMDB, used for serializing an object for purposes of storing on disc
	 * (in this case object=TripleID)
	 * 
	 * @author Eugen
	 *
	 */
	private static class TripleIDSerializer implements Serializer<TripleID>, Serializable {
		
		/** for the purpose of serialization, automatically generated value */
		private static final long serialVersionUID = 743374468899769026L;
		
		@Override
		public void serialize(DataOutput out, TripleID triple) throws IOException {
			out.writeInt(triple.getSubject());
			out.writeInt(triple.getPredicate());
			out.writeInt(triple.getObject());
		}
		@Override
		public TripleID deserialize(DataInput in) throws IOException, ClassNotFoundException {
			TripleID triple = new TripleID();
			triple.setSubject(in.readInt());
			triple.setPredicate(in.readInt());
			triple.setObject(in.readInt());
			return triple;
		}
	}

}
