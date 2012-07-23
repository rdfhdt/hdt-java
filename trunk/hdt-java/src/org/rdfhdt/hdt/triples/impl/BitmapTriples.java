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

import org.rdfhdt.hdt.compact.array.ArrayFactory;
import org.rdfhdt.hdt.compact.array.LogArray64;
import org.rdfhdt.hdt.compact.array.StaticArray;
import org.rdfhdt.hdt.compact.bitmap.AdjacencyList;
import org.rdfhdt.hdt.compact.bitmap.BitSequence375;
import org.rdfhdt.hdt.compact.bitmap.Bitmap;
import org.rdfhdt.hdt.compact.bitmap.BitmapFactory;
import org.rdfhdt.hdt.compact.bitmap.ModifiableBitmap;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.header.Header;
import org.rdfhdt.hdt.iterator.IteratorTripleID;
import org.rdfhdt.hdt.iterator.SequentialSearchIteratorTripleID;
import org.rdfhdt.hdt.listener.IntermediateListener;
import org.rdfhdt.hdt.listener.ListenerUtil;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.ModifiableTriples;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.Triples;
import org.rdfhdt.hdt.util.BitUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author mario.arias
 *
 */
public class BitmapTriples implements Triples {
	protected TripleComponentOrder order=TripleComponentOrder.SPO;
	
	protected StaticArray arrayY, arrayZ, indexZ;
	protected Bitmap bitmapY, bitmapZ, bitmapIndexZ;
	
	protected AdjacencyList adjY, adjZ, adjIndex;
	
	// Index for Y
	protected ArrayList<byte[]> indexYBuffers;

	public BitmapTriples() {
		this(new HDTSpecification());
	}
	
	public BitmapTriples(HDTSpecification spec) {
		String orderStr = spec.get("componentOrder");
		if(orderStr!=null) {
			order = TripleComponentOrder.valueOf(orderStr);
		}

		bitmapY = BitmapFactory.createBitmap(spec.get("bitmap.z"));
		bitmapZ = BitmapFactory.createBitmap(spec.get("bitmap.z"));

		arrayY = ArrayFactory.createStream(spec.get("array.y"));
		arrayZ = ArrayFactory.createStream(spec.get("array.z"));

		adjY = new AdjacencyList(arrayY, bitmapY);
		adjZ = new AdjacencyList(arrayZ, bitmapZ);
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#load(hdt.triples.ModifiableTriples, hdt.ProgressListener)
	 */
	@Override
	public void load(ModifiableTriples triples, ProgressListener listener) {
		triples.sort(order, listener);
		
		LogArray64 vectorY = new LogArray64(BitUtil.log2(triples.getNumberOfElements()));
		LogArray64 vectorZ = new LogArray64(BitUtil.log2(triples.getNumberOfElements()), triples.getNumberOfElements());
		ModifiableBitmap bitY = new BitSequence375();
		ModifiableBitmap bitZ = new BitSequence375(triples.getNumberOfElements());
		
		int lastX=0, lastY=0;
		int x, y, z;
		int numTriples=0;
		
		IteratorTripleID it = triples.searchAll();
		while(it.hasNext()) {
			TripleID triple = it.next();
			TripleOrderConvert.swapComponentOrder(triple, TripleComponentOrder.SPO, order);
			
			x = triple.getSubject();
			y = triple.getPredicate();
			z = triple.getObject();
			
			if(numTriples==0) {
				// First triple
				vectorY.append(y);
				vectorZ.append(z);
			} else if(x!=lastX) {
				// X changed
				bitY.append(true);
				vectorY.append(y);
				
				bitZ.append(true);
				vectorZ.append(z);
			} else if(y!=lastY) {
				// Y changed
				bitY.append(false);
				vectorY.append(y);
				
				bitZ.append(true);
				vectorZ.append(z);
			} else {
				// Z changed
				bitZ.append(false);
				vectorZ.append(z);
			}
			
			lastX = x;
			lastY = y;
			
			ListenerUtil.notifyCond(listener, "Converting to BitmapTriples", numTriples, numTriples, triples.getNumberOfElements());
			numTriples++;
		}
		
		bitY.append(true);
		bitZ.append(true);
		
		vectorY.aggresiveTrimToSize();
		vectorZ.trimToSize();
		
		// Assign local variables to BitmapTriples Object
		arrayY = vectorY;
		arrayZ = vectorZ;
		bitmapY = bitY;
		bitmapZ = bitZ;
		
		adjY = new AdjacencyList(arrayY, bitmapY);
		adjZ = new AdjacencyList(arrayZ, bitmapZ);

		// DEBUG
//		adjY.dump();
//		adjZ.dump();
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
	 * @see hdt.triples.Triples#cost(hdt.triples.TripleID)
	 */
	@Override
	public float cost(TripleID pattern) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#getNumberOfElements()
	 */
	@Override
	public long getNumberOfElements() {
		return arrayZ.getNumberOfElements();
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#size()
	 */
	@Override
	public long size() {
		return arrayY.size()+arrayZ.size()+bitmapY.getSizeBytes()+bitmapZ.getSizeBytes();
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#save(java.io.OutputStream, hdt.ControlInformation, hdt.ProgressListener)
	 */
	@Override
	public void save(OutputStream output, ControlInformation ci, ProgressListener listener) throws IOException {
		ci.clear();
		ci.set("codification", getType());
		ci.setInt("componentOrder", order.ordinal());
		ci.set("array.y", arrayY.getType());
		ci.set("array.z", arrayZ.getType());
		ci.set("bitmap.y", bitmapY.getType());
		ci.set("bitmap.z", bitmapZ.getType());
		ci.save(output);
		
		IntermediateListener iListener = new IntermediateListener(listener);
		bitmapY.save(output, iListener);
		bitmapZ.save(output, iListener);
		arrayY.save(output, iListener);
		arrayZ.save(output, iListener);
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#load(java.io.InputStream, hdt.ControlInformation, hdt.ProgressListener)
	 */
	@Override
	public void load(InputStream input, ControlInformation ci, ProgressListener listener) throws IOException {
		order = TripleComponentOrder.values()[(int)ci.getInt("componentOrder")];
		arrayY = ArrayFactory.createStream(ci.get("array.y"));
		arrayZ = ArrayFactory.createStream(ci.get("array.z"));
		bitmapY = BitmapFactory.createBitmap(ci.get("bitmap.y"));
		bitmapZ = BitmapFactory.createBitmap(ci.get("bitmap.z"));
		
		IntermediateListener iListener = new IntermediateListener(listener);
		bitmapY.load(input, iListener);
		bitmapZ.load(input, iListener);
		arrayY.load(input, iListener);
		arrayZ.load(input, iListener);
		
		adjY = new AdjacencyList(arrayY, bitmapY);
		adjZ = new AdjacencyList(arrayZ, bitmapZ);
	}
	
	private void createIndexPredicates() {
			ArrayList<List<Integer>> list=new ArrayList<List<Integer>>();
			
			// Generate lists
			for(long i=0;i<arrayY.getNumberOfElements();i++) {
				long pred = arrayY.get(i);
				
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
			long [] count = new long[(int)arrayY.getNumberOfElements()];
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
//			System.out.println("Size of Y: "+arrayY.size());
//			long size = indexY.size()/*+bitmapIndexY.getSizeBytes()*/;
//			System.out.println("Compressed Index total: "+ size + " "+StringUtil.getPercent(size, arrayY.size()));
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
//			System.out.println("Size of Y: "+arrayY.size());
//			long size = byteOut.size()+(list.size()*BitUtil.log2(arrayY.size()));
//			System.out.println("Compressed Index total: "+ size+ " "+StringUtil.getPercent(size, arrayY.size()));
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
//			System.out.println("Size of Y: "+arrayY.size());
//			System.out.println("Compressed Index total: "+ size+ " "+StringUtil.getPercent(size, arrayY.size()));
			
		
			
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

	private void createIndexObjects() {
		class Pair {
			int valueY;
			int positionY;
		};
		
		ArrayList<List<Pair>> list=new ArrayList<List<Pair>>();
			
		// Generate lists
		for(long i=0;i<arrayZ.getNumberOfElements();i++) {
			Pair pair = new Pair();
			pair.positionY = (int)adjZ.findListIndex(i);
			pair.valueY = (int) arrayY.get(pair.positionY);
			
			long valueZ = arrayZ.get(i);

			if(list.size()<=(int)valueZ) {
				list.ensureCapacity((int)valueZ);
				while(list.size()<valueZ) {
					list.add(new ArrayList<Pair>());	
				}
			}
			
			List<Pair> inner = list.get((int)valueZ-1);
			if(inner==null) {
				inner = new ArrayList<Pair>();
				list.set((int)valueZ-1, inner);
			}
			
			inner.add(pair);
		}
		
		// Serialize
		LogArray64 indexZ = new LogArray64(BitUtil.log2(arrayY.getNumberOfElements()), list.size());
		BitSequence375 bitmapIndexZ = new BitSequence375(arrayY.getNumberOfElements());
		long pos = 0;
		
		for(int i=0;i<list.size(); i++) {
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
		}
		
		this.indexZ = indexZ;
		this.bitmapIndexZ = bitmapIndexZ;
		this.adjIndex = new AdjacencyList(this.indexZ, this.bitmapIndexZ);
	}
	
	
	
	public void generateIndex(ProgressListener listener) {
//		createIndexPredicates();
		createIndexObjects();
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#populateHeader(hdt.header.Header, java.lang.String)
	 */
	@Override
	public void populateHeader(Header header, String rootNode) {
		header.insert(rootNode, HDTVocabulary.TRIPLES_TYPE, getType());
		header.insert(rootNode, HDTVocabulary.TRIPLES_NUM_TRIPLES, getNumberOfElements() );
		header.insert(rootNode, HDTVocabulary.TRIPLES_ORDER, order.toString() );
		header.insert(rootNode, HDTVocabulary.TRIPLES_ARRAYY_TYPE, arrayY.getType() );
		header.insert(rootNode, HDTVocabulary.TRIPLES_ARRAYZ_TYPE, arrayZ.getType() );
		header.insert(rootNode, HDTVocabulary.TRIPLES_ARRAYY_SIZE, arrayY.size() );
		header.insert(rootNode, HDTVocabulary.TRIPLES_ARRAYZ_SIZE, arrayZ.size() );
		if(bitmapY!=null) {
			header.insert(rootNode, HDTVocabulary.TRIPLES_BITMAPY_SIZE, bitmapY.getSizeBytes() );
		}
		if(bitmapZ!=null) {
			header.insert(rootNode, HDTVocabulary.TRIPLES_BITMAPZ_SIZE, bitmapZ.getSizeBytes() );
		}
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#getType()
	 */
	@Override
	public String getType() {
		return HDTVocabulary.TRIPLES_TYPE_BITMAP;
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#saveIndex(java.io.OutputStream, hdt.options.ControlInformation, hdt.listener.ProgressListener)
	 */
	@Override
	public void saveIndex(OutputStream output, ControlInformation ci, ProgressListener listener) throws IOException {
		IntermediateListener iListener = new IntermediateListener(listener);
		
		ci.clear();
		ci.setIndex(true);
		ci.setInt("numTriples", getNumberOfElements());
		ci.save(output);
		
		indexZ.save(output, iListener);
		bitmapIndexZ.save(output, iListener);
	}
	
	/* (non-Javadoc)
	 * @see hdt.triples.Triples#loadIndex(java.io.InputStream, hdt.options.ControlInformation, hdt.listener.ProgressListener)
	 */
	@Override
	public void loadIndex(InputStream input, ControlInformation ci,	ProgressListener listener) throws IOException {
		IntermediateListener iListener = new IntermediateListener(listener);
		
		long numTriples = ci.getInt("numTriples");

		if(this.getNumberOfElements()!=numTriples) {
			throw new IllegalArgumentException("This index is not associated to the HDT file");
		}
		
		indexZ = new LogArray64();
		bitmapIndexZ = new BitSequence375();
		
		indexZ.load(input, iListener);
		bitmapIndexZ.load(input, iListener);
		
		this.adjIndex = new AdjacencyList(this.indexZ, this.bitmapIndexZ);
	}


}
