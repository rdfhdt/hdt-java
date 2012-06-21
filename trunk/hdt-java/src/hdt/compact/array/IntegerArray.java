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
package hdt.compact.array;

import hdt.exceptions.NotImplementedException;
import hdt.hdt.HDTVocabulary;
import hdt.listener.ProgressListener;

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
public class IntegerArray implements DynamicArray {

	/** The array that holds the objects **/
	int [] data;
	int numelements = 0;

	/**
	 * Basic constructor
	 * 
	 */
	public IntegerArray() {
		this(10);
	}
	
	public IntegerArray(int capacity) {
		data = new int[capacity];
		numelements = capacity;
	}
	
	private void resizeArray(int size) {
		int [] newData = new int[size];
		System.arraycopy(data, 0, newData, 0, Math.min(newData.length, data.length));
		data = newData;
	}
	
	public void trimToSize() {
		resizeArray(numelements);
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
		return data[(int)position];
	}
	
	public void set(int position, long value) {
		data[position] = (int)value;
		numelements = Math.max(numelements, position+1);
	}
	
	public void append(long value) {
		if(data.length<numelements+1) {
			resizeArray(data.length*2);
		}
		data[numelements++] = (int) value;
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
			dos.writeInt(data[i]);
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
		numelements = 0;
		data = new int[(int)size];

		for(long i=0;i<size;i++) {
			append(dis.readInt());
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
			append(iterator.next().longValue());
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
