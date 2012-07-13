/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/compact/array/LongArray.java $
 * Revision: $Rev: 17 $
 * Last modified: $Date: 2012-07-03 21:43:15 +0100 (mar, 03 jul 2012) $
 * Last modified by: $Author: mario.arias@gmail.com $
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

package org.rdfhdt.hdt.compact.array;

import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.listener.ProgressListener;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * Stream of Integer objects
 * 
 */
public class LongArray implements DynamicArray {

	/** The array that holds the objects **/
	long [] data;
	long numelements = 0;

	/**
	 * Basic constructor
	 * 
	 */
	public LongArray() {
		this(10);
	}
	
	public LongArray(long capacity) {
		assert capacity>=0 && capacity<=Integer.MAX_VALUE;
		
		data = new long[(int)capacity];
		numelements = 0;
	}
	
	private void resizeArray(int size) {
		long [] newData = new long[size];
		System.arraycopy(data, 0, newData, 0, Math.min(newData.length, data.length));
		data = newData;
	}
	
	public void trimToSize() {
		resizeArray((int)numelements);
	}
	
	public void aggresiveTrimToSize() {
		trimToSize();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.array.Stream#get(int)
	 */
	@Override
	public long get(long position) {
		assert position>=0 && position<=Integer.MAX_VALUE;
		
		return data[(int)position];
	}
	
	public void set(long position, long value) {
		assert position>=0 && position<=Integer.MAX_VALUE;
		assert value>=0 && value<=Integer.MAX_VALUE;
		
		data[(int)position] = value;
		numelements = (int) Math.max(numelements, position+1);
	}
	
	public void append(long value) {
		assert value>=0 && value<=Long.MAX_VALUE;
		assert numelements<Integer.MAX_VALUE;
		
		if(data.length<numelements+1) {
			resizeArray(data.length*2);
		}
		data[(int)numelements++] = value;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.array.Stream#getNumberOfElements()
	 */
	@Override
	public long getNumberOfElements() {
		return numelements;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.array.Stream#save(java.io.OutputStream)
	 */
	@Override
	public void save(OutputStream output, ProgressListener listener) throws IOException {
		DataOutputStream dos = new DataOutputStream(output);
		dos.writeLong(numelements);
		for (int i=0;i<numelements;i++) {
			dos.writeLong(data[i]);
		}
		dos.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.array.Stream#load(java.io.InputStream)
	 */
	@Override
	public void load(InputStream input, ProgressListener listener) throws IOException {
		DataInputStream dis = new DataInputStream(input);
		long size = dis.readLong();
		
		if(size>Integer.MAX_VALUE) {
			throw new IOException("Cannot load values bigger than Integer with this data structure");
		}
		
		numelements = 0;
		data = new long[(int)size];

		for(long i=0;i<size;i++) {
			append(dis.readLong());
		}
		dis.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.array.Stream#add(int)
	 */
	@Override
	public void add(Iterator<Long> iterator) {
		while (iterator.hasNext()) {
			long value = iterator.next().longValue();
			append(value);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		throw new NotImplementedException();
	}

	/* (non-Javadoc)
	 * @see hdt.triples.array.Stream#size()
	 */
	@Override
	public long size() {
		return 4*numelements;
	}

	/* (non-Javadoc)
	 * @see hdt.compact.array.Stream#getType()
	 */
	@Override
	public String getType() {
		return HDTVocabulary.ARRAY_TYPE_INTEGER;
	}
}
