package org.rdfhdt.hdt.util.io.compress;

import org.rdfhdt.hdt.compact.integer.VByte;
import org.rdfhdt.hdt.exceptions.CRCException;
import org.rdfhdt.hdt.iterator.utils.ExceptionIterator;
import org.rdfhdt.hdt.triples.IndexedNode;
import org.rdfhdt.hdt.util.crc.CRC32;
import org.rdfhdt.hdt.util.crc.CRC8;
import org.rdfhdt.hdt.util.crc.CRCInputStream;
import org.rdfhdt.hdt.util.string.ReplazableString;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * Class to read a compress node file
 *
 * @author Antoine Willerval
 */
public class CompressNodeReader implements ExceptionIterator<IndexedNode, IOException>, Closeable {
	private final CRCInputStream stream;
	private final long size;
	private long index;
	private boolean waiting;
	private final IndexedNode last;
	private final ReplazableString tempString;

	public CompressNodeReader(InputStream stream) throws IOException {
		this.stream = new CRCInputStream(stream, new CRC8());
		this.size = VByte.decode(this.stream);
		if(!this.stream.readCRCAndCheck()) {
			throw new CRCException("CRC Error while merging Section Plain Front Coding Header.");
		}
		this.stream.setCRC(new CRC32());
		this.tempString = new ReplazableString();
		this.last = new IndexedNode(tempString, -1);
	}

	public long getSize() {
		return size;
	}

	public void checkComplete() throws IOException {
		if(!this.stream.readCRCAndCheck()) {
			throw new CRCException("CRC Error while merging Section Plain Front Coding Header.");
		}
	}

	/**
	 * @return the next element without passing to the next element
	 * @throws IOException reading exception
	 */
	public IndexedNode read() throws IOException {
		if (waiting) {
			return last;
		}
		int delta = (int) VByte.decode(stream);
		tempString.replace2(stream, delta);
		long index = VByte.decode(stream);
		last.setIndex(index);
		waiting = true;
		return last;
	}

	/**
	 * pass to the next element, mandatory with {@link #read()}
	 */
	public void pass() {
		waiting = false;
		index++;
	}

	@Override
	public IndexedNode next() throws IOException {
		IndexedNode node = read();
		pass();
		return node;
	}
	@Override
	public boolean hasNext() throws IOException {
		return index < size;
	}

	@Override
	public void close() throws IOException {
		stream.close();
	}

}
