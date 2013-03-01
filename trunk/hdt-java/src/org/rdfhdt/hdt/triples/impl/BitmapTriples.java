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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.rdfhdt.hdt.compact.bitmap.AdjacencyList;
import org.rdfhdt.hdt.compact.bitmap.Bitmap;
import org.rdfhdt.hdt.compact.bitmap.Bitmap375;
import org.rdfhdt.hdt.compact.bitmap.BitmapFactory;
import org.rdfhdt.hdt.compact.bitmap.ModifiableBitmap;
import org.rdfhdt.hdt.compact.sequence.Sequence;
import org.rdfhdt.hdt.compact.sequence.SequenceFactory;
import org.rdfhdt.hdt.compact.sequence.SequenceLog64;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.exceptions.IllegalFormatException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.header.Header;
import org.rdfhdt.hdt.iterator.SequentialSearchIteratorTripleID;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInfo;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TempTriples;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TriplesPrivate;
import org.rdfhdt.hdt.util.BitUtil;
import org.rdfhdt.hdt.util.StopWatch;
import org.rdfhdt.hdt.util.io.CountInputStream;
import org.rdfhdt.hdt.util.listener.IntermediateListener;
import org.rdfhdt.hdt.util.listener.ListenerUtil;

/**
 * @author mario.arias
 *
 */
public class BitmapTriples implements TriplesPrivate {
	protected TripleComponentOrder order=TripleComponentOrder.SPO;
	
	protected Sequence seqY, seqZ, indexZ, predicateCount;
	protected Bitmap bitmapY, bitmapZ, bitmapIndexZ;
	
	protected AdjacencyList adjY, adjZ, adjIndex;
	
	// Index for Y
	protected ArrayList<byte[]> indexYBuffers;

	public BitmapTriples() {
		this(new HDTSpecification());
	}
	
	public BitmapTriples(HDTOptions spec) {
		String orderStr = spec.get("triplesOrder");
		if(orderStr!=null) {
			order = TripleComponentOrder.valueOf(orderStr);
		}

		bitmapY = BitmapFactory.createBitmap(spec.get("bitmap.y"));
		bitmapZ = BitmapFactory.createBitmap(spec.get("bitmap.z"));

		seqY = SequenceFactory.createStream(spec.get("seq.y"));
		seqZ = SequenceFactory.createStream(spec.get("seq.z"));

		adjY = new AdjacencyList(seqY, bitmapY);
		adjZ = new AdjacencyList(seqZ, bitmapZ);
	}
	
	public Sequence getPredicateCount() {
		return predicateCount;
	}
	
	public void load(IteratorTripleID it, ProgressListener listener) {

		long number = it.estimatedNumResults();
		
		SequenceLog64 vectorY = new SequenceLog64(BitUtil.log2(number), number);
		SequenceLog64 vectorZ = new SequenceLog64(BitUtil.log2(number), number);
		ModifiableBitmap bitY = new Bitmap375(number);
		ModifiableBitmap bitZ = new Bitmap375(number);
		
		int lastX=0, lastY=0, lastZ=0;
		int x, y, z;
		int numTriples=0;
		
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
		
		bitY.append(true);
		bitZ.append(true);
		
		vectorY.aggresiveTrimToSize();
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
	public IteratorTripleID search(TripleID pattern) {
		TripleID reorderedPat = new TripleID(pattern);
		TripleOrderConvert.swapComponentOrder(reorderedPat, TripleComponentOrder.SPO, order);
		String patternString = reorderedPat.getPatternString();
		
		if(patternString.equals("?P?")) {
			return new BitmapTriplesIteratorY(this, pattern);
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
		
		IteratorTripleID bitIt = new BitmapTriplesIterator(this, pattern);
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
		return seqZ.getNumberOfElements();
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#size()
	 */
	@Override
	public long size() {
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
		
	}
	
	private void createIndexPredicates() {
			ArrayList<List<Integer>> list=new ArrayList<List<Integer>>();
			
			// Generate lists
			for(long i=0;i<seqY.getNumberOfElements();i++) {
				long pred = seqY.get(i);
				
				// Grow outter list
				if(list.size()<=(int)pred) {
					list.ensureCapacity((int)pred);
					while(list.size()<pred) {
						list.add(new ArrayList<Integer>());	
					}
				}
				
				// Create inner list
				List<Integer> inner = list.get((int)pred-1);
				if(inner==null) {
					inner = new ArrayList<Integer>();
					list.set((int)pred-1, inner);
				}
				
				// Add value
				inner.add((int)i);
			}
			
			// Serialize to array incremental.
//			StopWatch st = new StopWatch();
//			LogArray64 indexY = new LogArray64(BitUtil.log2(arrayY.getNumberOfElements()), 0);
//			BitSequence375 bitmapIndexY = new BitSequence375(arrayY.getNumberOfElements());
//			long pos=0;
//			
			long [] count = new long[(int)seqY.getNumberOfElements()];
			for(int i=0;i<list.size();i++) {
//				//		System.out.println("List "+i);
				List<Integer> inner = list.get(i);
				long last = 0;


				for(int j=0;j<inner.size();j++){
					//			System.out.println("\t"+inner.get(j));

					long newval = inner.get(j);

					//			System.out.println((int)(newval-last));
//					indexY.append(newval-last);
					
					if((newval-last)>0 && (newval-last)<=count.length) {
						count[(int)(newval-last-1)] ++;
					} else {
						System.out.println("WRONG: "+ newval+ " => "+last);
					}

					last = newval;
					
//					if(j==inner.size()-1) {
//						bitmapIndexY.set(pos, true);
//					} else {
//						bitmapIndexY.set(pos, false);
//					}
//					pos++;
				}

				//		System.out.println("Predicate: "+i+" Elements: "+ inner.size()+" Size: "+ compressed.length);	
			}
	//
//			indexY.aggresiveTrimToSize();
//			
//			System.out.println("Compressed in "+st.stopAndShow());
			//	
//			System.out.println("Size of Y: "+seqY.size());
//			long size = indexY.size()/*+bitmapIndexY.getSizeBytes()*/;
//			System.out.println("Compressed Index total: "+ size + " "+StringUtil.getPercent(size, seqY.size()));
//			
//			for(int i=0;i<count.length;i++) {
////				if(count[i]>0) {
////				System.out.println(i+";"+count[i]);
////				}
//			}
			
			int p;
			
			for(p=count.length-1; p>=0; p--){
				if(count[p]>0) {
					break;
				}
			}
			
			long [] newCount = new long[p+1];
			for(; p>0; --p){
				newCount[p] = count[p];
			}
			
			// Huffman encode
			
//			HuffmanCodec codec = new HuffmanCodec(newCount);
//			
//			ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
//			BitOutputStream bitOut = new BitOutputStream(byteOut);
//			
//					
//			for(int i=0;i<list.size();i++) {
//				//		System.out.println("List "+i);
//				List<Integer> inner = list.get(i);
//				long last = 0;
//
//				for(int j=0;j<inner.size();j++){
//					//			System.out.println("\t"+inner.get(j));
//
//					long newval = inner.get(j);
//
//					if((newval-last)>0 && (newval-last)<=newCount.length) {
//					codec.encode((int)(newval-last-1), bitOut);
//					} else {
//						System.out.println("WRONG: "+ newval+ " => "+last);
//					}
//					last = newval;
//				}
//
//				bitOut.flush();
//				//		System.out.println("Predicate: "+i+" Elements: "+ inner.size()+" Size: "+ compressed.length);	
//			}
//			
//			System.out.println("Huffman Stream size: "+byteOut.size());
//			System.out.println("Size of Y: "+seqY.size());
//			long size = byteOut.size()+(list.size()*BitUtil.log2(seqY.size()));
//			System.out.println("Compressed Index total: "+ size+ " "+StringUtil.getPercent(size, seqY.size()));
//			
			
			// Serialize lists
			
//			System.out.println("Serialize");
//			indexYBuffers = new ArrayList<byte[]>();
//			
//			long size = 0;
//			StopWatch st = new StopWatch();
//			
//			for(int i=0;i<list.size();i++) {
////				System.out.println("List "+i);
//				List<Integer> inner = list.get(i);
//				long last = 0;
//				
//				ByteArrayOutputStream out = new ByteArrayOutputStream();
//				DataOutputStream datout;
//				if(inner.size()>100) {
//					DeflaterOutputStream dout = new DeflaterOutputStream(out, true){{def.setLevel(1);def.setStrategy(Deflater.BEST_SPEED);}};
////					SnappyOutputStream dout = new SnappyOutputStream(out);
//					datout = new DataOutputStream(dout);
//					datout.writeByte(1);
//				} else {
//					datout = new DataOutputStream(out);
//					datout.writeByte(2);
//				}
//				for(int j=0;j<inner.size();j++){
////					System.out.println("\t"+inner.get(j));
//					
//					long newval = inner.get(j);
//					
////					System.out.println((int)(newval-last));
//					datout.writeInt((int)(newval-last));
//					
//					last = newval;
//				}
//				datout.close();
//				
//				byte [] compressed = out.toByteArray(); 
//				indexYBuffers.add(compressed);
//				size+=1+compressed.length;
////				System.out.println("Predicate: "+i+" Elements: "+ inner.size()+" Size: "+ compressed.length);	
//			}
//			
//			System.out.println("Compressed in "+st.stopAndShow());
//			
//			System.out.println("Size of Y: "+seqY.size());
//			System.out.println("Compressed Index total: "+ size+ " "+StringUtil.getPercent(size, seqY.size()));
			
		
			
//			
//			System.out.println("Buffer: ");
//			DataInputStream din = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(indexYBuffers.get(0))));
////			
//			try {
//				int pos = 0;
//				while(true) {
//					int readval = din.readInt();
////					System.out.println("\tPos="+pos+" Value: "+readval);
//					pos+=4;
//				}
//			} catch (Exception e) {
//				
//			}
//			
//			System.out.println("Samples: ");
//			for(int i=0;i<this.indexYsamples.getNumberOfElements();i++) {
//				System.out.println("\ti= "+i + " => " +this.indexYsamples.get(i));
//			}
//			
			
//			DeflateIntegerIterator iter = new DeflateIntegerIterator(this.indexYBuffers);
//			
//			for(int i=0;i<3;i++) {
//				System.out.println("Predicate "+i);
//				iter.reset(i);
	//
//				while(iter.hasNext()) {
//					System.out.println("\t"+iter.next());
//				}
//			}
		}

	private void createIndexObjectMemoryEfficient() {
		StopWatch global = new StopWatch();
		StopWatch st = new StopWatch();

		// Count the number of appearances of each object
		SequenceLog64 objectCount = new SequenceLog64(BitUtil.log2(seqZ.getNumberOfElements()));
		long maxCount = 0;
		for(long i=0;i<seqZ.getNumberOfElements(); i++) {
			long val = seqZ.get(i);
			if(val==0) {
				throw new RuntimeException("ERROR: There is a zero value in the Z level.");
			}
			if(objectCount.getNumberOfElements()<val) {
				// TODO: How to improve this?
				objectCount.resize(val);
			}
			long count = objectCount.get(val-1)+1;
			maxCount = count>maxCount ? count : maxCount;
			objectCount.set(val-1, count);
		}
		System.out.println("Count Objects in " + st.stopAndShow() + " Max was: " + maxCount);
		st.reset();

		// Calculate bitmap that separates each object sublist.
		Bitmap375 bitmapIndex = new Bitmap375(seqZ.getNumberOfElements());
		long tmpCount=0;
		for(long i=0;i<objectCount.getNumberOfElements();i++) {
			tmpCount += objectCount.get(i);
			bitmapIndex.set(tmpCount-1, true);
		}
		bitmapIndex.set(seqZ.getNumberOfElements()-1, true);
		System.out.println("Bitmap in " + st.stopAndShow());
		objectCount=null;
		st.reset();

		// Copy each object reference to its position
		SequenceLog64 objectInsertedCount = new SequenceLog64(BitUtil.log2(maxCount), bitmapIndex.countOnes());
		objectInsertedCount.resize(bitmapIndex.countOnes());

		SequenceLog64 objectArray = new SequenceLog64(BitUtil.log2(seqY.getNumberOfElements()), seqZ.getNumberOfElements());
		objectArray.resize(seqZ.getNumberOfElements());

		for(long i=0;i<seqZ.getNumberOfElements(); i++) {
				long objectValue = seqZ.get(i);
				long posY = i>0 ?  bitmapZ.rank1(i-1) : 0;

				long insertBase = objectValue==1 ? 0 : bitmapIndex.select1(objectValue-1)+1;
				long insertOffset = objectInsertedCount.get(objectValue-1);
				objectInsertedCount.set(objectValue-1, insertOffset+1);

				objectArray.set(insertBase+insertOffset, posY);
		}
		System.out.println("Object references in " + st.stopAndShow());
		objectInsertedCount=null;
		st.reset();

		
		long object=1;
		long first = 0;
		long last = bitmapIndex.select1(object)+1;
		do {
			long listLen = last-first;

			// Sublists of one element do not need to be sorted.

			// Hard-coded size-2 for speed (They are quite common).
			if(listLen==2) {
				long aPos = objectArray.get(first);
				long a = seqY.get(aPos);
				long bPos = objectArray.get(first+1);
				long b = seqY.get(bPos);
				if(a>b) {
					objectArray.set(first, bPos);
					objectArray.set(first+1, aPos);
				}
			} else if(listLen>2) {
				class Pair {
					int valueY;
					int positionY;
				};
				
				// FIXME: Sort directly without copying?
				ArrayList<Pair> list=new ArrayList<Pair>((int)listLen);

				// Create temporary list of (position, predicate)
				for(long i=first; i<last;i++) {
					Pair p = new Pair();
					p.positionY=(int)objectArray.get(i);
					p.valueY=(int)seqY.get(p.positionY);
					list.add(p);
				}

				// Sort
				Collections.sort(list, new Comparator<Pair>() {
					@Override
					public int compare(Pair o1, Pair o2) {
						if(o1.valueY==o2.valueY) {
							return o1.positionY-o2.positionY;
						}
						return o1.valueY-o2.valueY;
					}
				});
				
				// Copy back
				for(long i=first; i<last;i++) {
					objectArray.set(i, list.get((int)(i-first)).positionY);
				}
			}

			first = last;
			last = bitmapIndex.select1(object)+1;
			object++;
		} while(object<=bitmapIndex.countOnes());

		System.out.println("Sort object sublists in "+st.stopAndShow());
		st.reset();

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
		System.out.println("Count predicates in "+st.stopAndShow());
		this.predicateCount = predCount;
		st.reset();

		// Save Object Index
		this.indexZ = objectArray;
		this.bitmapIndexZ = bitmapIndex;
		this.adjIndex = new AdjacencyList(this.indexZ, this.bitmapIndexZ);
		
		System.out.println("Index generated in "+global.stopAndShow());
	}
	
	private void createIndexObjects() {
		// FIXME: Fast but very memory inefficient.
		class Pair {
			int valueY;
			int positionY;
		};
		
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
				inner = new ArrayList<Pair>(1);
				list.set((int)valueZ-1, inner);
			}
			
			inner.add(pair);

			if((i%100000)==0) {
				System.out.println("Processed: "+i+" objects out of "+total);
			}
		}
		
		System.out.println("Serialize object lists");
		// Serialize
		SequenceLog64 indexZ = new SequenceLog64(BitUtil.log2(seqY.getNumberOfElements()), list.size());
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

				if(j==inner.size()-1) {
					bitmapIndexZ.set(pos, true);
				} else {
					bitmapIndexZ.set(pos, false);
				}
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
	
	
	
	public void generateIndex(ProgressListener listener) {		
//		createIndexPredicates();
		//createIndexObjects();
		createIndexObjectMemoryEfficient();	
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#populateHeader(hdt.header.Header, java.lang.String)
	 */
	@Override
	public void populateHeader(Header header, String rootNode) {
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
		
		predicateCount.save(output, iListener);
		bitmapIndexZ.save(output, iListener);
		indexZ.save(output, iListener);
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
		
		predicateCount = SequenceFactory.createStream(input);
		predicateCount.load(input, iListener);

		bitmapIndexZ = BitmapFactory.createBitmap(input);
		bitmapIndexZ.load(input, iListener);
		
		indexZ = SequenceFactory.createStream(input);
		indexZ.load(input, iListener);

		this.adjIndex = new AdjacencyList(this.indexZ, this.bitmapIndexZ);
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
		
		predicateCount = SequenceFactory.createStream(input, f);

		bitmapIndexZ = BitmapFactory.createBitmap(input);
		bitmapIndexZ.load(input, iListener);
		
		indexZ = SequenceFactory.createStream(input, f);

		this.adjIndex = new AdjacencyList(this.indexZ, this.bitmapIndexZ);
	}

}
