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
import hdt.enums.TripleComponentOrder;
import hdt.hdt.HDTVocabulary;
import hdt.header.Header;
import hdt.iterator.IteratorTripleID;
import hdt.iterator.SequentialSearchIteratorTripleID;
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


/**
 * @author mck
 *
 */
public class CompactTriples implements Triples {
	private HDTSpecification spec;
	protected TripleComponentOrder order;
	protected int numTriples;
	
	protected StaticArray arrayY, arrayZ;
	
	public CompactTriples(HDTSpecification spec) {
		this.spec = spec;
		String orderStr = spec.get("triples.component.order");
		if(orderStr!=null) {
			order = TripleComponentOrder.valueOf(orderStr);
		}
		
		arrayY = ArrayFactory.createStream(spec.get("array.y"));
		arrayZ = ArrayFactory.createStream(spec.get("array.z"));
	}
	
	/* (non-Javadoc)
	 * @see hdt.triples.Triples#load(hdt.triples.ModifiableTriples, hdt.listener.ProgressListener)
	 */
	@Override
	public void load(ModifiableTriples triples, ProgressListener listener) {
		triples.sort(order, listener);

		LogArray64 vectorY = new LogArray64(BitUtil.log2(triples.getNumberOfElements()));
		LogArray64 vectorZ = new LogArray64(BitUtil.log2(triples.getNumberOfElements()), (int)triples.getNumberOfElements());

		int lastX=0, lastY=0;
		int x, y, z;
		numTriples=0;

		IteratorTripleID it = triples.searchAll();
		while(it.hasNext()) {
			TripleID triple = it.next();
			TripleOrderConvert.swapComponentOrder(triple, TripleComponentOrder.SPO, order);
			
			x = triple.getSubject();
			y = triple.getPredicate();
			z = triple.getObject();

			if(numTriples==0) {
				vectorY.append(y);
				vectorZ.append(z);
			} else if(x!=lastX) {
				vectorY.append(0);
				vectorY.append(y);

				vectorZ.append(0);
				vectorZ.append(z);
			} else if(y!=lastY) {
				vectorY.append(y);

				vectorZ.append(0);
				vectorZ.append(z);
			} else {
				vectorZ.append(z);
			}

			lastX = x;
			lastY = y;

			ListenerUtil.notifyCond(listener, "Converting to CompactTriples", numTriples, numTriples, triples.getNumberOfElements());
			numTriples++;
		}
		vectorY.aggresiveTrimToSize();
		vectorZ.trimToSize();

		// Assign local variables to BitmapTriples Object
		arrayY = vectorY;
		arrayZ = vectorZ;
	}
	
	/* (non-Javadoc)
	 * @see hdt.triples.Triples#search(hdt.triples.TripleID)
	 */
	@Override
	public IteratorTripleID search(TripleID pattern) {
		return new SequentialSearchIteratorTripleID(pattern, new CompactTriplesIterator(this));
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
		return numTriples;
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#size()
	 */
	@Override
	public long size() {
		return arrayY.size()+arrayZ.size();
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#save(java.io.OutputStream, hdt.ControlInformation, hdt.listener.ProgressListener)
	 */
	@Override
	public void save(OutputStream output, ControlInformation ci, ProgressListener listener) throws IOException {
		ci.set("array.y", arrayY.getType());
		ci.set("array.z", arrayZ.getType());
		ci.save(output);
		arrayY.save(output, listener);
		arrayZ.save(output, listener);
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#load(java.io.InputStream, hdt.ControlInformation, hdt.listener.ProgressListener)
	 */
	@Override
	public void load(InputStream input, ControlInformation ci, ProgressListener listener) throws IOException {
		arrayY = ArrayFactory.createStream(spec.get("array.y"));
		arrayZ = ArrayFactory.createStream(spec.get("array.z"));
		arrayY.load(input, listener);
		arrayZ.load(input, listener);
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
		return HDTVocabulary.TRIPLES_TYPE_COMPACT;
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

}
