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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;

import org.rdfhdt.hdt.enums.TripleComponentOrder;
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

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.collections.StoredSortedKeySet;
import com.sleepycat.je.CacheMode;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

/**
 * This is an on-disc implementation of ModifiableTriples based on a TreeMap,
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
public class TriplesBerkeley implements ModifiableTriples {

	private HDTSpecification specs;

	/** environment of the database */
	private Environment env;
	/** database suporting the set of triples */
	private Database db;
	/** The array to hold the triples */
	private SortedSet<TripleID> triples;
	/** base of the name of the collection in db */
	private String triplesSetName;
	/** version of the collection in db */
	private int triplesSetVersion;
	
	private TripleIDTupleBinding binding;

	/** The order of the triples */
	private TripleComponentOrder order;
	private int numValidTriples;

	/**
	 * Constructor, given an order to sort by
	 * 
	 * @param order
	 *            The order to sort by
	 */
	public TriplesBerkeley(HDTSpecification specification) {

		this.specs = specification;

		String orderStr = specs.get("triples.component.order");
		if(orderStr==null) {
			orderStr = "SPO";
		}
		this.order = TripleComponentOrder.valueOf(orderStr);
		
		this.binding = new TripleIDTupleBinding();

		setupDatabaseEnv();

		this.numValidTriples = 0;
	}

	/**method for setting up the db and the db-backed treeSet*/
	private void setupDatabaseEnv(){

		//FIXME read from specs...
		File folder = new File("DB");
		if (!folder.exists() && !folder.mkdir()){
			throw new RuntimeException("Unable to create DB folder...");
		}

		EnvironmentConfig envConf = new EnvironmentConfig();
		envConf.setAllowCreateVoid(true);
		envConf.setTransactionalVoid(false);
		envConf.setCacheModeVoid(CacheMode.DEFAULT);
		envConf.setCacheSizeVoid(specs.getBytesProperty("tempTriples.cache"));

		env = new Environment(folder, envConf);

		triplesSetName = "triples";
		triplesSetVersion = 1;
		this.db = createDB();

		this.triples = new StoredSortedKeySet<TripleID>(db, binding, true);

	}
	
	/** method for creating a new DB in the environment */
	private Database createDB(){
		DatabaseConfig dbConf = new DatabaseConfig();
		dbConf.setExclusiveCreateVoid(true); //so that it throws exception if already exists
		dbConf.setAllowCreateVoid(true);
		dbConf.setTransactionalVoid(false);
		dbConf.setTemporaryVoid(true);
		dbConf.setBtreeComparatorVoid(new TripleIDByteComparator(binding, new TripleIDComparator(this.order)));
		
		return env.openDatabase(null, triplesSetName+triplesSetVersion, dbConf);
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
		this.order = order;
		//if order has changed then copy all triples with a different ordering - O(n*logn) time, 2x memory (extra slow because of disc)
		int oldVersion = triplesSetVersion;
		triplesSetVersion++;
		Database newDB = createDB();
		SortedSet<TripleID> newTriples = new StoredSortedKeySet<TripleID>(newDB, binding, true);
		newTriples.addAll(triples);
		triples = newTriples;
		db.close();
		db = newDB;
		//TODO not really necessary? takes time and bothers no one just lying there on disc, everything's going to be deleted on close anyway...
		env.removeDatabase(null, triplesSetName+oldVersion);
		//
	}

	@Override
	public TripleComponentOrder getOrder() {
		return this.order;
	}

	@Override
	public IteratorTripleID search(TripleID pattern) {
		String patternStr = pattern.getPatternString();
		if(patternStr.equals("???")) {
			return new TriplesSet.TriplesSetIterator(triples, order, getNumberOfElements());
		} else {
			return new SequentialSearchIteratorTripleID(pattern, new TriplesSet.TriplesSetIterator(triples, order, getNumberOfElements()));
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
		db = null;
		
		//File envHome = env.getHome();
		env.cleanLog();
		env.close();
		env = null;
/*
		for (File f : envHome.listFiles()){
			String fname = f.getName();
			if (fname.equalsIgnoreCase("je.properties"))
				continue;
			if (fname.endsWith(".jdb") || fname.startsWith("je."))
				f.delete();
		}*/
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
	public void populateHeader(Header header, String rootNode) {
		header.insert(rootNode, HDTVocabulary.TRIPLES_TYPE, HDTVocabulary.TRIPLES_TYPE_TRIPLESLIST);
		header.insert(rootNode, HDTVocabulary.TRIPLES_NUM_TRIPLES, getNumberOfElements() );
		header.insert(rootNode, HDTVocabulary.TRIPLES_ORDER, order.ordinal() );
	}

	public String getType() {
		return HDTVocabulary.TRIPLES_TYPE_TRIPLESSET;
	}
	
	/**
	 * A class extending a class defined in Berkeley, used for serializing an object for purposes of storing on disc
	 * (in this case object=TripleID)
	 * 
	 * @author Eugen
	 *
	 */
	private static class TripleIDTupleBinding extends TupleBinding<TripleID> implements Serializable {
		
		private static final long serialVersionUID = -3205835251920853240L;

		@Override
		public TripleID entryToObject(TupleInput input) {
			TripleID triple = new TripleID();
			triple.setSubject(input.readInt());
			triple.setPredicate(input.readInt());
			triple.setObject(input.readInt());
			return triple;
		}
		
		@Override
		public void objectToEntry(TripleID triple, TupleOutput output) {
			output.writeInt(triple.getSubject());
			output.writeInt(triple.getPredicate());
			output.writeInt(triple.getObject());
		}
	}
	
	/**
	 * A comparator of TripleID objects while in serialized form.
	 * @author Eugen
	 *
	 */
	private static class TripleIDByteComparator implements Comparator<byte[]>, Serializable {

		/** for the purpose of serialization... automatically generated */
		private static final long serialVersionUID = 5754536296951600403L;
		/** method for serialization of an instance of this class as specified by Serializable */
		private void writeObject(java.io.ObjectOutputStream out) throws IOException {
			out.defaultWriteObject();
		}
		/** method for deserialization of an instance of this class as specified by Serializable */
		private void readObject(java.io.ObjectInputStream in) 
				throws IOException, ClassNotFoundException {
			in.defaultReadObject();
		}
		
		private TripleIDTupleBinding binding;
		private TripleIDComparator comparator;
		
		public TripleIDByteComparator(TripleIDTupleBinding binding, TripleIDComparator comparator) {
			this.binding = binding;
			this.comparator = comparator;
		}

		@Override
		public int compare(byte[] o1, byte[] o2) {
			TupleInput in1 = new TupleInput(o1);
			TripleID triple1 = binding.entryToObject(in1);
			TupleInput in2 = new TupleInput(o2);
			TripleID triple2 = binding.entryToObject(in2);
			return comparator.compare(triple1, triple2);
		}
		
	}
	
}
