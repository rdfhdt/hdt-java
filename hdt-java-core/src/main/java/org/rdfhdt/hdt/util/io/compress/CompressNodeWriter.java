package org.rdfhdt.hdt.util.io.compress;

import org.rdfhdt.hdt.compact.integer.VByte;
import org.rdfhdt.hdt.triples.IndexedNode;
import org.rdfhdt.hdt.util.crc.CRC32;
import org.rdfhdt.hdt.util.crc.CRC8;
import org.rdfhdt.hdt.util.crc.CRCOutputStream;
import org.rdfhdt.hdt.util.string.ByteString;
import org.rdfhdt.hdt.util.string.ByteStringUtil;
import org.rdfhdt.hdt.util.string.CompactString;
import org.rdfhdt.hdt.util.string.ReplazableString;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Class to write a compress node file
 *
 * @author Antoine Willerval
 */
public class CompressNodeWriter implements Closeable {
	private final CRCOutputStream out;
	private final ReplazableString previousStr = new ReplazableString();

	public CompressNodeWriter(OutputStream stream, long size) throws IOException {
		this.out = new CRCOutputStream(stream, new CRC8());
		VByte.encode(this.out, size);
		this.out.writeCRC();
		this.out.setCRC(new CRC32());
	}

	public void appendNode(IndexedNode node) throws IOException {
		ByteString str = node.getNode();
		long index = node.getIndex();

		// Find common part.
		int delta = ByteStringUtil.longestCommonPrefix(previousStr, str);
		// Write Delta in VByte
		VByte.encode(out, delta);
		// Write remaining
		ByteStringUtil.append(out, str, delta);
		out.write(0); // End of string
		VByte.encode(out, index); // index of the node
		previousStr.replace(str);
	}

	public void writeCRC() throws IOException {
		out.writeCRC();
	}

	@Override
	public void close() throws IOException{
		writeCRC();
		out.close();
	}
}
