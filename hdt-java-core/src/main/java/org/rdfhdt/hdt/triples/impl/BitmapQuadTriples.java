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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.rdfhdt.hdt.compact.bitmap.AdjacencyList;
import org.rdfhdt.hdt.compact.bitmap.Bitmap;
import org.rdfhdt.hdt.compact.bitmap.Bitmap375Big;
import org.rdfhdt.hdt.compact.bitmap.ModifiableBitmap;
import org.rdfhdt.hdt.compact.sequence.DynamicSequence;
import org.rdfhdt.hdt.compact.sequence.Sequence;
import org.rdfhdt.hdt.compact.sequence.SequenceLog64Big;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.exceptions.IllegalFormatException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.iterator.SuppliableIteratorTripleID;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInfo;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TempTriples;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.util.BitUtil;
import org.rdfhdt.hdt.util.io.CountInputStream;
import org.rdfhdt.hdt.util.listener.IntermediateListener;
import org.rdfhdt.hdt.util.listener.ListenerUtil;

import com.github.andrewoma.dexx.collection.Pair;

import java.io.File;

/**
 * @author mario.arias
 *
 */
public class BitmapQuadTriples extends BitmapTriples {

	protected List<ModifiableBitmap> quadInfoAG;

	public BitmapQuadTriples() throws IOException {
		super();
	}
	
	public BitmapQuadTriples(HDTOptions spec) throws IOException {
		super(spec);

		quadInfoAG = new ArrayList<>();
	}

	public BitmapQuadTriples(HDTOptions spec, Sequence seqY, Sequence seqZ, Bitmap bitY, Bitmap bitZ, TripleComponentOrder order) throws IOException {
		super(spec, seqY, seqZ, bitY, bitZ, order);
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#getType()
	 */
	@Override
	public String getType() {
		return HDTVocabulary.TRIPLES_TYPE_BITMAP_QUAD;
	}

	@Override
	public void load(IteratorTripleID it, ProgressListener listener) {
		System.out.println("Loading BitmapQuadTriples");
		long number = it.estimatedNumResults();

		DynamicSequence vectorY = new SequenceLog64Big(BitUtil.log2(number), number + 1);
		DynamicSequence vectorZ = new SequenceLog64Big(BitUtil.log2(number), number + 1);
		
		ModifiableBitmap bitY = Bitmap375Big.memory(number);
		ModifiableBitmap bitZ = Bitmap375Big.memory(number);
		
		long lastX=0, lastY=0, lastZ=0;
		long x, y, z, g;
		long numTriples=0;
		long numGraphs=0;

		long tripleIndex = -1;

		List<Pair<Long, Long>> triplesInGraph = new ArrayList<>();
		
		while(it.hasNext()) {
			TripleID triple = it.next();
			TripleOrderConvert.swapComponentOrder(triple, TripleComponentOrder.SPO, order);
			
			x = triple.getSubject();
			y = triple.getPredicate();
			z = triple.getObject();
			g = triple.getGraph();
			if(x==0 || y==0 || z==0 || g==0) {
				throw new IllegalFormatException("None of the components of a quad can be null");
			}
			numGraphs = Math.max(numGraphs, g);
			long graphIndex = g - 1;
			boolean sameAsLast = x == lastX && y == lastY && z == lastZ;
			if (!sameAsLast) {
				tripleIndex += 1;
			}
			triplesInGraph.add(new Pair<>(tripleIndex, graphIndex));
			if (sameAsLast) {
				continue;
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
		
		for (int i = 0; i < numGraphs; i++) {
			ModifiableBitmap b = Bitmap375Big.memory(numTriples);
			for (int j = 0; j < numTriples; j++) {
				b.append(false);
			}
			quadInfoAG.add(b);
		}

		for (Pair<Long, Long> tripleInGraph : triplesInGraph) {
			long iTriple = tripleInGraph.component1();
			long iGraph = tripleInGraph.component2();
			quadInfoAG
				.get((int) iGraph)
				.set(iTriple, true);
		}

		for (int i = 0; i < quadInfoAG.size(); i++) {
			Bitmap b = quadInfoAG.get(i);
			System.out.println("Bitmap "+i+": "+b);
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
		
		isClosed=false;
	}

	@Override
	public void load(TempTriples triples, ProgressListener listener) {
		super.load(triples, listener);
	}

	@Override
	public void load(InputStream input, ControlInfo ci, ProgressListener listener) throws IOException {
		this.todo("load 3");
	}

	@Override
	public long getNumberOfElements() {
		return super.getNumberOfElements();
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#size()
	 */
	@Override
	public long size() {
		if(isClosed) return 0;
		int numGraphs = quadInfoAG.size();
		long sizeFirstGraph = 0;
		if (numGraphs > 0) {
			sizeFirstGraph = quadInfoAG.get(0).getSizeBytes();
		}
		return seqY.size()+seqZ.size()+bitmapY.getSizeBytes()+bitmapZ.getSizeBytes()+sizeFirstGraph*numGraphs;
	}

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
		for (Bitmap b : quadInfoAG) {
			b.save(output, iListener);
		}
	}

	@Override
	public SuppliableIteratorTripleID search(TripleID pattern) {
		this.todo("search");
		return null;
	}

	@Override
	public void mapFromFile(CountInputStream input, File f,	ProgressListener listener) throws IOException {
		this.todo("mapFromFile");
	}

	private void todo(String name) {
		System.out.println("NOT SUPPORTED YET: " + name);
		System.exit(1);
	}
}

