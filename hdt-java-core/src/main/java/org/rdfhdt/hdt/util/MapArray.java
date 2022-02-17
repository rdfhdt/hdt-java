package org.rdfhdt.hdt.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Iterator;

import org.rdfhdt.hdt.compact.sequence.Sequence;
import org.rdfhdt.hdt.compact.sequence.SequenceLog64;
import org.rdfhdt.hdt.compact.sequence.SequenceLog64Map;
import org.rdfhdt.hdt.listener.ProgressOut;
import org.rdfhdt.hdt.util.io.CountInputStream;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.string.ByteStringUtil;


public class MapArray {

	private ByteBuffer[] buffers; // Encoded sequence
	private FileChannel ch;
	private int block = 0;
	private int buffer = 0;
	private long bytePos = 0;
	private int BLOCKS_PER_BYTEBUFFER = 50000;
	private long numstrings;
	long[] posFirst;
	private Sequence blocks;
	
	static int blocksize = 16;

	public void write(Iterator<String> input, int totalEntries, String fileName)
			throws IOException {
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream(16 * 1024);

		numstrings = 0;

		blocks = new SequenceLog64(32, totalEntries / blocksize);

		// iterate an save in ByteArrayOutputStream
		while (input.hasNext()) {
			if (numstrings % blocksize == 0) {
				// Add new block pointer
				((SequenceLog64)blocks).append(byteOut.size());
			}
			CharSequence str = input.next();
			// Copy full string
			ByteStringUtil.append(byteOut, str, 0);
			byteOut.write(0); // End of string
			numstrings++;
		}
	
		// Ending block pointer.
		((SequenceLog64)blocks).append(byteOut.size());
		// Trim text/blocks
		((SequenceLog64)blocks).aggressiveTrimToSize();

		byteOut.flush();
		byte[] text = byteOut.toByteArray(); // save to byte[]

		OutputStream output = new FileOutputStream(fileName);
	    DataOutputStream dos = new DataOutputStream(output);
		dos.writeLong(numstrings);
		
		blocks.save(output, new ProgressOut()); // Write blocks directly to
												// output, they have their own
												// CRC check.
		IOUtil.writeBuffer(output, text, 0, text.length, new ProgressOut()); // write
																				// in
																				// a
																				// file
		dos.close();
		output.close();
		blocks.close();
	}

	public void map(String fileName) throws IOException {

		File f = new File(fileName);
		CountInputStream input = new CountInputStream(new BufferedInputStream(
				new FileInputStream(fileName)));

		// Read blocks
		DataInputStream in = new DataInputStream(input);
		numstrings = in.readLong();
		blocks = new SequenceLog64Map(input, f);
		long numBlocks = blocks.getNumberOfElements();

		long base = input.getTotalBytes();
		// Read packed data
		FileInputStream fs = new FileInputStream(f);
		ch = fs.getChannel();
		long numBuffers = 1 + numBlocks / BLOCKS_PER_BYTEBUFFER;
		posFirst = new long[(int) numBuffers];
		buffers = new ByteBuffer[(int) numBuffers];

		while (block < numBlocks - 1) {
			int nextBlock = (int) Math.min(numBlocks - 1, block
					+ BLOCKS_PER_BYTEBUFFER);
			long nextBytePos = blocks.get(nextBlock);

			buffers[buffer] = ch.map(MapMode.READ_ONLY, base + bytePos,
					nextBytePos - bytePos);
			buffers[buffer].order(ByteOrder.LITTLE_ENDIAN);

			posFirst[buffer] = bytePos;

			bytePos = nextBytePos;
			block += BLOCKS_PER_BYTEBUFFER;
			buffer++;
		}
		fs.close();
		ch.close();
	}
	public String extract (int id){
		if ((id >= 0) && (id < numstrings)) {
			int blockSearch = (id - 1) / blocksize;
			ByteBuffer bufferSearch = buffers[blockSearch
					/ BLOCKS_PER_BYTEBUFFER].duplicate();
			bufferSearch
					.position((int) (blocks.get(blockSearch) - posFirst[blockSearch
							/ BLOCKS_PER_BYTEBUFFER]));

			CharSequence ret = "";
			try {

				ret = getStringByteBuffer(bufferSearch);
				int stringid = id % blocksize;
				for (int i = 0; i < stringid; i++) {
					ret = getStringByteBuffer(bufferSearch);
				}
				return ret.toString();
			} catch (IOException e) {
				e.printStackTrace();
				return "";
			}
		} else {
			System.err.println("Incorrect id");
			return "";
		}
	}
	public long getNumElements(){
		return numstrings;
	}
	private  String getStringByteBuffer(ByteBuffer in) throws IOException {
		byte[] buffer = new byte[16 * 1024];
		int used = 0;

		int n = in.capacity() - in.position();
		while (n-- != 0) {
			byte value = in.get();
			if (value == 0) {
				return new String(buffer, 0, used,
						ByteStringUtil.STRING_ENCODING);
			}

			buffer[used++] = value;
		}
		throw new IllegalArgumentException(
				"Was reading a string but stream ended before finding the null terminator");
	}

}
