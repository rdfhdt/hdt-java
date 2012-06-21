/**
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
/**
 * 
 */
package hdt.triples.impl;

import hdt.compact.array.ArrayFactory;
import hdt.compact.array.LogArray64;
import hdt.compact.array.StaticArray;
import hdt.compact.bitmap.AdjacencyList;
import hdt.compact.bitmap.BitSequence375;
import hdt.compact.bitmap.Bitmap;
import hdt.compact.bitmap.BitmapFactory;
import hdt.compact.bitmap.ModifiableBitmap;
import hdt.enums.TripleComponentOrder;
import hdt.hdt.HDTVocabulary;
import hdt.header.Header;
import hdt.iterator.IteratorTripleID;
import hdt.iterator.SequentialSearchIteratorTripleID;
import hdt.listener.IntermediateListener;
import hdt.listener.ListenerUtil;
import hdt.listener.ProgressListener;
import hdt.options.ControlInformation;
import hdt.options.HDTSpecification;
import hdt.triples.ModifiableTriples;
import hdt.triples.TripleID;
import hdt.triples.Triples;
import hdt.util.BitUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author mck
 *
 */
public class BitmapTriples implements Triples {
	protected TripleComponentOrder order=TripleComponentOrder.SPO;
	
	protected StaticArray arrayY, arrayZ, indexZ;
	protected Bitmap bitmapY, bitmapZ, bitmapIndexZ;
	
	// Index for Y
	protected ArrayList<byte[]> indexYBuffers;

	public BitmapTriples() {
		this(new HDTSpecification());
	}
	
	public BitmapTriples(HDTSpecification spec) {
		String orderStr = spec.get("triples.component.order");
		if(orderStr!=null) {
			order = TripleComponentOrder.valueOf(orderStr);
		}
		
		arrayY = ArrayFactory.createStream(spec.get("array.y"));
		arrayZ = ArrayFactory.createStream(spec.get("array.z"));
		bitmapY = BitmapFactory.createBitmap(spec.get("bitmap.z"));
		bitmapZ = BitmapFactory.createBitmap(spec.get("bitmap.z"));
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
		
		// DEBUG
//		AdjacencyList adjY = new AdjacencyList(arrayY, bitmapY);
//		AdjacencyList adjZ = new AdjacencyList(arrayZ, bitmapZ);
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
		//ci.save(output);
		
		IntermediateListener iListener = new IntermediateListener(listener);
		output.write(order.ordinal());
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
		/*order = TripleComponentOrder.values()[(int)ci.getInt("componentarray)];
		arrayY = StreamFactory.createStream(ci.get("array.y"));
		arrayZ = StreamFactory.createStream(ci.get("array.z"));
		bitmapY = BitmapFactory.createBitmap(ci.get("bitmap.y"));
		bitmapZ = BitmapFactory.createBitmap(ci.get("bitmap.z"));*/
		
		IntermediateListener iListener = new IntermediateListener(listener);
		order = TripleComponentOrder.values()[input.read()];
		bitmapY.load(input, iListener);
		bitmapZ.load(input, iListener);
		arrayY.load(input, iListener);
		arrayZ.load(input, iListener);
	}
	
	private void createIndexObjects() {
		class Pair {
			int predicate;
			int position;
		};
		
		ArrayList<List<Pair>> list=new ArrayList<List<Pair>>();
		AdjacencyList adjZ = new AdjacencyList(arrayZ, bitmapZ);
		
		// Generate lists
		for(long i=0;i<arrayZ.getNumberOfElements();i++) {
			Pair pair = new Pair();
			pair.position = (int)adjZ.findListIndex(i);
			pair.predicate = (int) arrayY.get(pair.position);
			
			long obj = arrayZ.get(i);

			if(list.size()<=(int)obj) {
				list.ensureCapacity((int)obj);
				while(list.size()<obj) {
					list.add(new ArrayList<Pair>());	
				}
			}
			
			List<Pair> inner = list.get((int)obj-1);
			if(inner==null) {
				inner = new ArrayList<Pair>();
				list.set((int)obj-1, inner);
			}
			
			inner.add(pair);
		}
		
		// Serialize
		LogArray64 indexZ = new LogArray64(BitUtil.log2(arrayY.getNumberOfElements()), list.size());
		BitSequence375 bitmapIndexZ = new BitSequence375(arrayY.getNumberOfElements());
		long pos = 0;
		
		for(int i=0;i<list.size(); i++) {
			List<Pair> inner = list.get(i);
			
			// Sort by predicate
			Collections.sort(inner, new Comparator<Pair>() {
				@Override
				public int compare(Pair o1, Pair o2) {
					return o1.predicate-o2.predicate;
				}
			});
			
			// Serialize
			for(int j=0;j<inner.size();j++){
				indexZ.append(inner.get(j).position);

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
	}
	
	
	
	public void generateIndex(ProgressListener listener) {
//		createIndexPredicates();
		createIndexObjects();
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#populateHeader(hdt.header.Header, java.lang.String)
	 */
	@Override
	public void populateHeader(Header head, String rootNode) {
		// TODO Auto-generated method stub

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
	}


}
