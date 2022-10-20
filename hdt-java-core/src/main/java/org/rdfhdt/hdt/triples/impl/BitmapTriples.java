/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/triples/impl/BitmapTriples.java $
 * Revision: $Rev: 203 $
 * Last modified: $Date: 2013-05-24 10:48:53 +0100 (vie, 24 may 2013) $
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

import org.apache.commons.io.file.PathUtils;
import org.rdfhdt.hdt.compact.bitmap.AdjacencyList;
import org.rdfhdt.hdt.compact.bitmap.Bitmap;
import org.rdfhdt.hdt.compact.bitmap.Bitmap375;
import org.rdfhdt.hdt.compact.bitmap.Bitmap375Disk;
import org.rdfhdt.hdt.compact.bitmap.BitmapFactory;
import org.rdfhdt.hdt.compact.bitmap.ModifiableBitmap;
import org.rdfhdt.hdt.compact.sequence.DynamicSequence;
import org.rdfhdt.hdt.compact.sequence.Sequence;
import org.rdfhdt.hdt.compact.sequence.SequenceFactory;
import org.rdfhdt.hdt.compact.sequence.SequenceLog64;
import org.rdfhdt.hdt.compact.sequence.SequenceLog64Big;
import org.rdfhdt.hdt.compact.sequence.SequenceLog64BigDisk;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.exceptions.IllegalFormatException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.header.Header;
import org.rdfhdt.hdt.iterator.SequentialSearchIteratorTripleID;
import org.rdfhdt.hdt.iterator.SuppliableIteratorTripleID;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.*;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TempTriples;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TriplesPrivate;
import org.rdfhdt.hdt.util.BitUtil;
import org.rdfhdt.hdt.util.StopWatch;
import org.rdfhdt.hdt.util.io.CountInputStream;
import org.rdfhdt.hdt.util.io.CountOutputStream;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.listener.IntermediateListener;
import org.rdfhdt.hdt.util.listener.ListenerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author mario.arias
 *
 */
public class BitmapTriples implements TriplesPrivate {
	private static final Logger log = LoggerFactory.getLogger(BitmapTriples.class);

	protected TripleComponentOrder order;
	
	protected Sequence seqY, seqZ, indexZ, predicateCount;
	protected Bitmap bitmapY, bitmapZ, bitmapIndexZ;

	protected AdjacencyList adjY, adjZ, adjIndex;
	
	// Index for Y
	public PredicateIndex predicateIndex;

	boolean diskSequence;
	CreateOnUsePath diskSequenceLocation;

	private boolean isClosed;

	public BitmapTriples() throws IOException {
		this(new HDTSpecification());
	}
	
	public BitmapTriples(HDTOptions spec) throws IOException {
		String orderStr = spec.get(HDTOptionsKeys.TRIPLE_ORDER_KEY);
		if(orderStr == null) {
			this.order = TripleComponentOrder.SPO;
		} else {
			this.order = TripleComponentOrder.valueOf(orderStr);
		}

		loadDiskSequence(spec);

		bitmapY = BitmapFactory.createBitmap(spec.get("bitmap.y"));
		bitmapZ = BitmapFactory.createBitmap(spec.get("bitmap.z"));

		seqY = SequenceFactory.createStream(spec.get("seq.y"));
		seqZ = SequenceFactory.createStream(spec.get("seq.z"));

		adjY = new AdjacencyList(seqY, bitmapY);
		adjZ = new AdjacencyList(seqZ, bitmapZ);
		
		isClosed=false;
	}

	public BitmapTriples(HDTOptions spec, Sequence seqY, Sequence seqZ, Bitmap bitY, Bitmap bitZ, TripleComponentOrder order) throws IOException {
		this.seqY = seqY;
		this.seqZ = seqZ;
		this.bitmapY = bitY;
		this.bitmapZ = bitZ;
		this.order = order;

		loadDiskSequence(spec);

		adjY = new AdjacencyList(seqY, bitmapY);
		adjZ = new AdjacencyList(seqZ, bitmapZ);

		isClosed=false;
	}

	private void loadDiskSequence(HDTOptions spec) throws IOException {
		String optDisk = spec == null ? null : spec.get("bitmaptriples.sequence.disk");
		diskSequence = optDisk != null && optDisk.equalsIgnoreCase("true");


		if (diskSequenceLocation != null) {
			diskSequenceLocation.close();
		}

		if (diskSequence) {
			assert spec != null; // happy compiler
			String optDiskLocation = spec.get("bitmaptriples.sequence.disk.location");
			if (optDiskLocation != null && !optDiskLocation.isEmpty()) {
				diskSequenceLocation = new CreateOnUsePath(Path.of(optDiskLocation));
			} else {
				diskSequenceLocation = new CreateOnUsePath();
			}
		} else {
			diskSequenceLocation = null;
		}
	}

	public PredicateIndex getPredicateIndex() {
		return predicateIndex;
	}

	public Sequence getPredicateCount() {
		return predicateCount;
	}
	
	public void load(IteratorTripleID it, ProgressListener listener) {

		long number = it.estimatedNumResults();
		
		DynamicSequence vectorY = new SequenceLog64Big(BitUtil.log2(number), number);
		DynamicSequence vectorZ = new SequenceLog64Big(BitUtil.log2(number), number);
		
		ModifiableBitmap bitY = new Bitmap375(number);
		ModifiableBitmap bitZ = new Bitmap375(number);
		
		long lastX=0, lastY=0, lastZ=0;
		long x, y, z;
		long numTriples=0;
		
		while(it.hasNext()) {
			TripleID triple = it.next();
			TripleOrderConvert.swapComponentOrder(triple, TripleComponentOrder.SPO, order);
			
			x = triple.getSubject();
			y = triple.getPredicate();
			z = triple.getObject();
			if(x==0 || y==0 || z==0) {
				throw new IllegalFormatException("None of the components of a triple can be null");
			}
			
			if(numTriples==0) {
				// First triple
				vectorY.append(y);
				vectorZ.append(z);
			} else if(x!=lastX) {
				if(x!=lastX+1) {
					throw new IllegalFormatException("Upper level must be increasing and correlative.");
				}
				// X changed
				bitY.append(true);
				vectorY.append(y);
				
				bitZ.append(true);
				vectorZ.append(z);
			} else if(y!=lastY) {
				if(y<lastY) {
					throw new IllegalFormatException("Middle level must be increasing for each parent.");
				}
				
				// Y changed
				bitY.append(false);
				vectorY.append(y);
				
				bitZ.append(true);
				vectorZ.append(z);
			} else {
				if(z<lastZ) {
					throw new IllegalFormatException("Lower level must be increasing for each parent.");
				}
				
				// Z changed
				bitZ.append(false);
				vectorZ.append(z);
			}
			
			lastX = x;
			lastY = y;
			lastZ = z;
			
			ListenerUtil.notifyCond(listener, "Converting to BitmapTriples", numTriples, numTriples, number);
			numTriples++;
		}
		
		if(numTriples>0) {
			bitY.append(true);
			bitZ.append(true);
		}
		
		vectorY.aggressiveTrimToSize();
		vectorZ.trimToSize();
		
		// Assign local variables to BitmapTriples Object
		seqY = vectorY;
		seqZ = vectorZ;
		bitmapY = bitY;
		bitmapZ = bitZ;
		
		adjY = new AdjacencyList(seqY, bitmapY);
		adjZ = new AdjacencyList(seqZ, bitmapZ);

		// DEBUG
//		adjY.dump();
//		adjZ.dump();
		
		isClosed=false;
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#load(hdt.triples.TempTriples, hdt.ProgressListener)
	 */
	@Override
	public void load(TempTriples triples, ProgressListener listener) {
		triples.setOrder(order);
		triples.sort(listener);
		
		IteratorTripleID it = triples.searchAll();
		this.load(it, listener);
	}


	/* (non-Javadoc)
	 * @see hdt.triples.Triples#search(hdt.triples.TripleID)
	 */
	@Override
	public SuppliableIteratorTripleID search(TripleID pattern) {
		if(isClosed) {
			throw new IllegalStateException("Cannot search on BitmapTriples if it's already closed");
		}
		
		if (getNumberOfElements() == 0 || pattern.isNoMatch()) {
			return new EmptyTriplesIterator(order);
		}
		
		
		TripleID reorderedPat = new TripleID(pattern);
		TripleOrderConvert.swapComponentOrder(reorderedPat, TripleComponentOrder.SPO, order);
		String patternString = reorderedPat.getPatternString();
		
		if(patternString.equals("?P?")) {
			if(this.predicateIndex!=null) {
				return new BitmapTriplesIteratorYFOQ(this, pattern);
			} else {
				return new BitmapTriplesIteratorY(this, pattern);
			}
		}
		
		if(indexZ!=null && bitmapIndexZ!=null) {
			// USE FOQ
			if(patternString.equals("?PO") || patternString.equals("??O")) {
				return new BitmapTriplesIteratorZFOQ(this, pattern);	
			}			
		} else {
			if(patternString.equals("?PO")) {
				return new SequentialSearchIteratorTripleID(pattern, new BitmapTriplesIteratorZ(this, pattern));
			}

			if(patternString.equals("??O")) {
				return new BitmapTriplesIteratorZ(this, pattern);	
			}
		}

		SuppliableIteratorTripleID bitIt = new BitmapTriplesIterator(this, pattern);
		if(patternString.equals("???") || patternString.equals("S??") || patternString.equals("SP?") || patternString.equals("SPO")) {
			return bitIt;
		} else {
			return new SequentialSearchIteratorTripleID(pattern, bitIt);
		}

	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#searchAll()
	 */
	@Override
	public IteratorTripleID searchAll() {
		return this.search(new TripleID());
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#getNumberOfElements()
	 */
	@Override
	public long getNumberOfElements() {
		if(isClosed) return 0;
		return seqZ.getNumberOfElements();
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#size()
	 */
	@Override
	public long size() {
		if(isClosed) return 0;
		return seqY.size()+seqZ.size()+bitmapY.getSizeBytes()+bitmapZ.getSizeBytes();
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#save(java.io.OutputStream, hdt.ControlInfo, hdt.ProgressListener)
	 */
	@Override
	public void save(OutputStream output, ControlInfo ci, ProgressListener listener) throws IOException {
		ci.clear();
		ci.setFormat(getType());
		ci.setInt("order", order.ordinal());
		ci.setType(ControlInfo.Type.TRIPLES);
		ci.save(output);
		
		IntermediateListener iListener = new IntermediateListener(listener);
		bitmapY.save(output, iListener);
		bitmapZ.save(output, iListener);
		seqY.save(output, iListener);
		seqZ.save(output, iListener);
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#load(java.io.InputStream, hdt.ControlInfo, hdt.ProgressListener)
	 */
	@Override
	public void load(InputStream input, ControlInfo ci, ProgressListener listener) throws IOException {
		
		if(ci.getType()!=ControlInfo.Type.TRIPLES) {
			throw new IllegalFormatException("Trying to read a triples section, but was not triples.");
		}
		
		if(!ci.getFormat().equals(getType())) {
			throw new IllegalFormatException("Trying to read BitmapTriples, but the data does not seem to be BitmapTriples");
		}
		
		order = TripleComponentOrder.values()[(int)ci.getInt("order")];
		
		IntermediateListener iListener = new IntermediateListener(listener);
		
		bitmapY = BitmapFactory.createBitmap(input);
		bitmapY.load(input, iListener);
		
		bitmapZ = BitmapFactory.createBitmap(input);
		bitmapZ.load(input, iListener);
		
		seqY = SequenceFactory.createStream(input);
		seqY.load(input, iListener);
		
		seqZ = SequenceFactory.createStream(input);
		seqZ.load(input, iListener);
		
		adjY = new AdjacencyList(seqY, bitmapY);
		adjZ = new AdjacencyList(seqZ, bitmapZ);
		
		isClosed=false;
	}
	

	@Override
	public void mapFromFile(CountInputStream input, File f,	ProgressListener listener) throws IOException {
		ControlInformation ci = new ControlInformation();
		ci.load(input);
		if(ci.getType()!=ControlInfo.Type.TRIPLES) {
			throw new IllegalFormatException("Trying to read a triples section, but was not triples.");
		}
		
		if(!ci.getFormat().equals(getType())) {
			throw new IllegalFormatException("Trying to read BitmapTriples, but the data does not seem to be BitmapTriples");
		}
		
		order = TripleComponentOrder.values()[(int)ci.getInt("order")];
		
		IntermediateListener iListener = new IntermediateListener(listener);
		
		bitmapY = BitmapFactory.createBitmap(input);
		bitmapY.load(input, iListener);
		
		bitmapZ = BitmapFactory.createBitmap(input);
		bitmapZ.load(input, iListener);
		
		seqY = SequenceFactory.createStream(input, f);
		seqZ = SequenceFactory.createStream(input, f);
		
		adjY = new AdjacencyList(seqY, bitmapY);
		adjZ = new AdjacencyList(seqZ, bitmapZ);
		
		isClosed=false;
	}

	private DynamicSequence createSequence64(Path baseDir, String name, int size, long capacity, boolean forceDisk) throws IOException {
		if (forceDisk && !diskSequence) {
			Path path = Files.createTempFile(name, ".bin");
			return new SequenceLog64BigDisk(path, size, capacity, true) {
				@Override
				public void close() throws IOException {
					try {
						super.close();
					} finally {
						Files.deleteIfExists(path);
					}
				}
			};
		} else {
			return createSequence64(baseDir, name, size, capacity);
		}
	}

	private DynamicSequence createSequence64(Path baseDir, String name, int size, long capacity) {
		if (diskSequence) {
			Path path = baseDir.resolve(name);
			return new SequenceLog64BigDisk(path, size, capacity, true) {
				@Override
				public void close() throws IOException {
					try {
						super.close();
					} finally {
						Files.deleteIfExists(path);
					}
				}
			};
		} else {
			return new SequenceLog64Big(size, capacity, true);
		}
	}

	private ModifiableBitmap createBitmap375(Path baseDir, String name, long size) {
		if (diskSequence) {
			Path path = baseDir.resolve(name);
			return new Bitmap375Disk(path, size) {
				@Override
				public void close() throws IOException {
					try {
						super.close();
					} finally {
						Files.deleteIfExists(path);
					}
				}
			};
		} else {
			return new Bitmap375(size);
		}
	}

	private void createIndexObjectMemoryEfficient(HDTOptions specIndex) throws IOException {
		loadDiskSequence(specIndex);

		Path diskLocation;
		if (diskSequence) {
			diskLocation = diskSequenceLocation.createOrGetPath();
		} else {
			diskLocation = null;
		}

		StopWatch global = new StopWatch();
		StopWatch st = new StopWatch();

		// Count the number of appearances of each object
		long maxCount = 0;
		long numDifferentObjects = 0;
		long numReservedObjects = 8192;
		ModifiableBitmap bitmapIndex = null;
		DynamicSequence predCount = null;
		DynamicSequence objectArray = null;

		try {
			try (DynamicSequence objectCount = createSequence64(diskLocation, "objectCount", BitUtil.log2(seqZ.getNumberOfElements()), numReservedObjects)) {
				for (long i = 0; i < seqZ.getNumberOfElements(); i++) {
					long val = seqZ.get(i);
					if (val == 0) {
						throw new RuntimeException("ERROR: There is a zero value in the Z level.");
					}
					if (numReservedObjects < val) {
						while (numReservedObjects < val) {
							numReservedObjects <<= 1;
						}
						objectCount.resize(numReservedObjects);
					}
					if (numDifferentObjects < val) {
						numDifferentObjects = val;
					}

					long count = objectCount.get(val - 1) + 1;
					maxCount = Math.max(count, maxCount);
					objectCount.set(val - 1, count);
				}
				log.info("Count Objects in {} Max was: {}", st.stopAndShow(), maxCount);
				st.reset();

				// Calculate bitmap that separates each object sublist.
				bitmapIndex = createBitmap375(diskLocation, "bitmapIndex", seqZ.getNumberOfElements());
				long tmpCount = 0;
				for (long i = 0; i < numDifferentObjects; i++) {
					tmpCount += objectCount.get(i);
					bitmapIndex.set(tmpCount - 1, true);
				}
				bitmapIndex.set(seqZ.getNumberOfElements() - 1, true);
				log.info("Bitmap in {}", st.stopAndShow());
			}
			st.reset();

			objectArray = createSequence64(diskLocation, "objectArray", BitUtil.log2(seqY.getNumberOfElements()), seqZ.getNumberOfElements(), true);
			objectArray.resize(seqZ.getNumberOfElements());

			// Copy each object reference to its position
			try (DynamicSequence objectInsertedCount = createSequence64(diskLocation, "objectInsertedCount", BitUtil.log2(maxCount), numDifferentObjects)) {
				objectInsertedCount.resize(numDifferentObjects);

				for (long i = 0; i < seqZ.getNumberOfElements(); i++) {
					long objectValue = seqZ.get(i);
					long posY = i > 0 ? bitmapZ.rank1(i - 1) : 0;

					long insertBase = objectValue == 1 ? 0 : bitmapIndex.select1(objectValue - 1) + 1;
					long insertOffset = objectInsertedCount.get(objectValue - 1);
					objectInsertedCount.set(objectValue - 1, insertOffset + 1);

					objectArray.set(insertBase + insertOffset, posY);
				}
				log.info("Object references in {}", st.stopAndShow());
			}
			st.reset();


			long object = 1;
			long first = 0;
			long last = bitmapIndex.selectNext1(first) + 1;
			do {
				long listLen = last - first;

				// Sublists of one element do not need to be sorted.

				// Hard-coded size 2 for speed (They are quite common).
				if (listLen == 2) {
					long aPos = objectArray.get(first);
					long a = seqY.get(aPos);
					long bPos = objectArray.get(first + 1);
					long b = seqY.get(bPos);
					if (a > b) {
						objectArray.set(first, bPos);
						objectArray.set(first + 1, aPos);
					}
				} else if (listLen > 2) {
					class Pair {
						Long valueY;
						Long positionY;

						@Override
						public String toString() {
							return String.format("%d %d", valueY, positionY);
						}
					}

					// FIXME: Sort directly without copying?
					ArrayList<Pair> list = new ArrayList<>((int) listLen);

					// Create temporary list of (position, predicate)
					for (long i = first; i < last; i++) {
						Pair p = new Pair();
						p.positionY = objectArray.get(i);
						p.valueY = seqY.get(p.positionY);
						list.add(p);
					}

					// Sort
					list.sort((Pair o1, Pair o2) -> {
						if (o1.valueY.equals(o2.valueY)) {
							return o1.positionY.compareTo(o2.positionY);
						}
						return o1.valueY.compareTo(o2.valueY);
					});

					// Copy back
					for (long i = first; i < last; i++) {
						Pair pair = list.get((int) (i - first));
						objectArray.set(i, pair.positionY);
					}
				}

				first = last;
				last = bitmapIndex.selectNext1(first) + 1;
				object++;
			} while (object <= numDifferentObjects);

			log.info("Sort object sublists in {}", st.stopAndShow());
			st.reset();

			// Count predicates
			predCount = createSequence64(diskLocation, "predCount", BitUtil.log2(seqY.getNumberOfElements()), 0);
			for (long i = 0; i < seqY.getNumberOfElements(); i++) {
				// Read value
				long val = seqY.get(i);

				// Grow if necessary
				if (predCount.getNumberOfElements() < val) {
					predCount.resize(val);
				}

				// Increment
				predCount.set(val - 1, predCount.get(val - 1) + 1);
			}
			predCount.trimToSize();
			log.info("Count predicates in {}", st.stopAndShow());
		} catch (Throwable t) {
			try {
				throw t;
			} finally {
				try {
					if (bitmapIndex instanceof Closeable) {
						((Closeable) bitmapIndex).close();
					}
				} finally {
					try {
						if (objectArray != null) {
							objectArray.close();
						}
					} finally {
						if (predCount != null) {
							predCount.close();
						}
					}
				}
			}
		}

		this.predicateCount = predCount;
		st.reset();

		// Save Object Index
		this.indexZ = objectArray;
		this.bitmapIndexZ = bitmapIndex;
		this.adjIndex = new AdjacencyList(this.indexZ, this.bitmapIndexZ);

		log.info("Index generated in {}", global.stopAndShow());
	}
	
	@SuppressWarnings("unused")
	private void createIndexObjects() {
		// FIXME: Fast but very memory inefficient.
		class Pair {
			int valueY;
			int positionY;
		}

		ArrayList<List<Pair>> list=new ArrayList<List<Pair>>();
		
		System.out.println("Generating HDT Index for ?PO, and ??O queries.");
		// Generate lists
		long total=seqZ.getNumberOfElements();
		for(long i=0;i<total;i++) {
			Pair pair = new Pair();
			pair.positionY = (int)adjZ.findListIndex(i);
			pair.valueY = (int) seqY.get(pair.positionY);
			
			long valueZ = seqZ.get(i);

			if(list.size()<=(int)valueZ) {
				list.ensureCapacity((int)valueZ);
				while(list.size()<valueZ) {
					list.add(new ArrayList<Pair>(1));	
				}
			}
			
			List<Pair> inner = list.get((int)valueZ-1);
			if(inner==null) {
				inner = new ArrayList<>(1);
				list.set((int)valueZ-1, inner);
			}
			
			inner.add(pair);

			if((i%100000)==0) {
				System.out.println("Processed: "+i+" objects out of "+total);
			}
		}
		
		System.out.println("Serialize object lists");
		// Serialize
		DynamicSequence indexZ = new SequenceLog64(BitUtil.log2(seqY.getNumberOfElements()), list.size());
		Bitmap375 bitmapIndexZ = new Bitmap375(seqY.getNumberOfElements());
		long pos = 0;
		
		total = list.size();
		for(int i=0;i<total; i++) {
			List<Pair> inner = list.get(i);
			
			// Sort by Y
			Collections.sort(inner, new Comparator<Pair>() {
				@Override
				public int compare(Pair o1, Pair o2) {
					return o1.valueY-o2.valueY;
				}
			});
			
			// Serialize
			for(int j=0;j<inner.size();j++){
				indexZ.append(inner.get(j).positionY);

				bitmapIndexZ.set(pos, j == inner.size() - 1);
				pos++;
			}
			
			if((i%100000)==0) {
				System.out.println("Serialized: "+i+" lists out of "+total);
			}
			
			// Dereference processed list to let GC release the memory.
			list.set(i, null);
		}
		
		// Count predicates
		SequenceLog64 predCount = new SequenceLog64(BitUtil.log2(seqY.getNumberOfElements()));
		for(long i=0;i<seqY.getNumberOfElements(); i++) {
			// Read value
			long val = seqY.get(i);

			// Grow if necessary
			if(predCount.getNumberOfElements()<val) {
				predCount.resize(val);
			}

			// Increment
			predCount.set(val-1, predCount.get(val-1)+1);
		}
		predCount.trimToSize();
		this.predicateCount = predCount;
		
		this.indexZ = indexZ;
		this.bitmapIndexZ = bitmapIndexZ;
		this.adjIndex = new AdjacencyList(this.indexZ, this.bitmapIndexZ);
	}
	
	
	
	@Override
	public void generateIndex(ProgressListener listener, HDTOptions specIndex) throws IOException{
		predicateIndex = new PredicateIndexArray(this);
		predicateIndex.generate(listener, specIndex);
		
		//createIndexObjects();
		createIndexObjectMemoryEfficient(specIndex);
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#populateHeader(hdt.header.Header, java.lang.String)
	 */
	@Override
	public void populateHeader(Header header, String rootNode) {
		if(rootNode==null || rootNode.length()==0) {
			throw new IllegalArgumentException("Root node for the header cannot be null");
		}
		
		header.insert(rootNode, HDTVocabulary.TRIPLES_TYPE, getType());
		header.insert(rootNode, HDTVocabulary.TRIPLES_NUM_TRIPLES, getNumberOfElements() );
		header.insert(rootNode, HDTVocabulary.TRIPLES_ORDER, order.toString() );
//		header.insert(rootNode, HDTVocabulary.TRIPLES_SEQY_TYPE, seqY.getType() );
//		header.insert(rootNode, HDTVocabulary.TRIPLES_SEQZ_TYPE, seqZ.getType() );
//		header.insert(rootNode, HDTVocabulary.TRIPLES_SEQY_SIZE, seqY.size() );
//		header.insert(rootNode, HDTVocabulary.TRIPLES_SEQZ_SIZE, seqZ.size() );
//		if(bitmapY!=null) {
//			header.insert(rootNode, HDTVocabulary.TRIPLES_BITMAPY_SIZE, bitmapY.getSizeBytes() );
//		}
//		if(bitmapZ!=null) {
//			header.insert(rootNode, HDTVocabulary.TRIPLES_BITMAPZ_SIZE, bitmapZ.getSizeBytes() );
//		}
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#getType()
	 */
	@Override
	public String getType() {
		return HDTVocabulary.TRIPLES_TYPE_BITMAP;
	}

	@Override
	public TripleID findTriple(long position) {
		if (position == 0) {
			// remove this special case so we can use position-1
			return new TripleID(
					1,
					seqY.get(0),
					seqZ.get(0));
		}
		// get the object at the given position
		long z = seqZ.get(position);

		// -1 so we don't count end of tree
		long posY = bitmapZ.rank1(position - 1);
		long y = seqY.get(posY);

		if (posY == 0) {
			// remove this case to do posY - 1
			return new TripleID(1, y, z);
		}

		// -1 so we don't count end of tree
		long posX = bitmapY.rank1(posY - 1);
		long x = posX + 1; // the subject ID is the position + 1, IDs start from 1 not zero

		return new TripleID(x, y, z);
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#saveIndex(java.io.OutputStream, hdt.options.ControlInfo, hdt.listener.ProgressListener)
	 */
	@Override
	public void saveIndex(OutputStream output, ControlInfo ci, ProgressListener listener) throws IOException {
		IntermediateListener iListener = new IntermediateListener(listener);
		
		ci.clear();
		ci.setType(ControlInfo.Type.INDEX);
		ci.setInt("numTriples", getNumberOfElements());
		ci.setInt("order", order.ordinal());
		ci.setFormat(HDTVocabulary.INDEX_TYPE_FOQ);
		ci.save(output);
		
		bitmapIndexZ.save(output, iListener);
		indexZ.save(output, iListener);

		predicateIndex.save(output);

		predicateCount.save(output, iListener);
	}
	
	/* (non-Javadoc)
	 * @see hdt.triples.Triples#loadIndex(java.io.InputStream, hdt.options.ControlInfo, hdt.listener.ProgressListener)
	 */
	@Override
	public void loadIndex(InputStream input, ControlInfo ci, ProgressListener listener) throws IOException {
		IntermediateListener iListener = new IntermediateListener(listener);
		
		if(ci.getType()!=ControlInfo.Type.INDEX) {
			throw new IllegalFormatException("Trying to read an Index Section but it was not an Index.");
		}
		
		if(!HDTVocabulary.INDEX_TYPE_FOQ.equals(ci.getFormat())) {
			throw new IllegalFormatException("Trying to read wrong format of Index. Remove the .hdt.index file and let the app regenerate it.");
		}
		
		long numTriples = ci.getInt("numTriples");
		if(this.getNumberOfElements()!=numTriples) {
			throw new IllegalFormatException("This index is not associated to the HDT file");
		}
		
		TripleComponentOrder indexOrder = TripleComponentOrder.values()[(int)ci.getInt("order")];
		if(indexOrder != order) {
			throw new IllegalFormatException("The order of the triples is not the same of the index.");
		}
		

		IOUtil.closeObject(bitmapIndexZ);
		bitmapIndexZ = null;

		bitmapIndexZ = BitmapFactory.createBitmap(input);
		try {
			bitmapIndexZ.load(input, iListener);

			if (indexZ != null) {
				try {
					indexZ.close();
				} finally {
					indexZ = null;
				}
			}

			indexZ = SequenceFactory.createStream(input);
			try {
				indexZ.load(input, iListener);

				if (predicateIndex != null) {
					try {
						predicateIndex.close();
					} finally {
						predicateIndex = null;
					}
				}

				predicateIndex = new PredicateIndexArray(this);
				try {
					predicateIndex.load(input);

					if (predicateCount != null) {
						try {
							predicateCount.close();
						} finally {
							predicateCount = null;
						}
					}

					predicateCount = SequenceFactory.createStream(input);
					try {
						predicateCount.load(input, iListener);

						this.adjIndex = new AdjacencyList(this.indexZ, this.bitmapIndexZ);
					} catch (Throwable t) {
						try {
							predicateCount.close();
						} finally {
							predicateCount = null;
							this.adjIndex = null;
						}
						throw t;
					}
				} catch (Throwable t) {
					try {
						predicateIndex.close();
					} finally {
						predicateIndex = null;
					}
					throw t;
				}
			} catch (Throwable t) {
				try {
					indexZ.close();
				} finally {
					indexZ = null;
				}
				throw t;
			}
		} catch (Throwable t) {
			try {
				IOUtil.closeObject(bitmapIndexZ);
			} finally {
				bitmapIndexZ = null;
			}
			throw t;
		}
	}

	@Override
	public void mapIndex(CountInputStream input, File f, ControlInfo ci, ProgressListener listener) throws IOException {
		IntermediateListener iListener = new IntermediateListener(listener);
		
		if(ci.getType()!=ControlInfo.Type.INDEX) {
			throw new IllegalFormatException("Trying to read an Index Section but it was not an Index.");
		}
		
		if(!HDTVocabulary.INDEX_TYPE_FOQ.equals(ci.getFormat())) {
			throw new IllegalFormatException("Trying to read wrong format of Index. Remove the .hdt.index file and let the app regenerate it.");
		}
		
		long numTriples = ci.getInt("numTriples");
		if(this.getNumberOfElements()!=numTriples) {
			throw new IllegalFormatException("This index is not associated to the HDT file");
		}
		
		TripleComponentOrder indexOrder = TripleComponentOrder.values()[(int)ci.getInt("order")];
		if(indexOrder != order) {
			throw new IllegalFormatException("The order of the triples is not the same of the index.");
		}

		bitmapIndexZ = BitmapFactory.createBitmap(input);
		bitmapIndexZ.load(input, iListener);
		
		indexZ = SequenceFactory.createStream(input, f);

		if (predicateIndex != null) {
			predicateIndex.close();
		}

		predicateIndex = new PredicateIndexArray(this);
		predicateIndex.mapIndex(input, f, iListener);

		predicateCount = SequenceFactory.createStream(input, f);
		
		this.adjIndex = new AdjacencyList(this.indexZ, this.bitmapIndexZ);
	}

	@Override
	public void close() throws IOException {
		isClosed=true;
		try {
			if (diskSequenceLocation != null) {
				try {
					diskSequenceLocation.close();
				} finally {
					diskSequenceLocation = null;
				}
			}
		} finally {
			try {
				if (seqY != null) {
					seqY.close();
					seqY = null;
				}
			} finally {
				try {
					if (seqZ != null) {
						seqZ.close();
						seqZ=null;
					}
				} finally {
					try {
						if (indexZ != null) {
							indexZ.close();
							indexZ=null;
						}
					} finally {
						try {
							if (predicateCount != null) {
								predicateCount.close();
								predicateCount=null;
							}
						} finally {
							try {
								if (predicateIndex != null) {
									predicateIndex.close();
									predicateIndex = null;
								}
							} finally {
								if (bitmapIndexZ instanceof Closeable) {
									((Closeable) bitmapIndexZ).close();
									bitmapIndexZ = null;
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	public TripleComponentOrder getOrder() {
		return this.order;
	}

	public Sequence getIndexZ() {
		return indexZ;
	}

	public Sequence getSeqY() {
		return seqY;
	}

	public Sequence getSeqZ() {
		return seqZ;
	}

	public Bitmap getBitmapY() {
		return bitmapY;
	}

	public Bitmap getBitmapZ() {
		return bitmapZ;
	}
	
	public Bitmap getBitmapIndex(){
		return bitmapIndexZ;
	}

	private static class CreateOnUsePath implements Closeable {
		boolean mkdir;
		Path path;

		public CreateOnUsePath() {
			this(null);
		}

		public CreateOnUsePath(Path path) {
			this.path = path;
		}

		public Path createOrGetPath() throws IOException {
			if (path == null) {
				path = Files.createTempDirectory("bitmapTriple");
			} else {
				Files.createDirectories(path);
			}
			return path;
		}

		@Override
		public void close() throws IOException {
			if (mkdir) {
				PathUtils.deleteDirectory(path);
			}
		}
	}
}

