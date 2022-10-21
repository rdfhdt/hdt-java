package org.rdfhdt.hdt.dictionary.impl.section;

import org.rdfhdt.hdt.compact.integer.VByte;
import org.rdfhdt.hdt.compact.sequence.SequenceLog64BigDisk;
import org.rdfhdt.hdt.dictionary.DictionarySectionPrivate;
import org.rdfhdt.hdt.dictionary.TempDictionarySection;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.listener.MultiThreadListener;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.util.crc.CRC32;
import org.rdfhdt.hdt.util.crc.CRC8;
import org.rdfhdt.hdt.util.crc.CRCOutputStream;
import org.rdfhdt.hdt.util.io.CloseSuppressPath;
import org.rdfhdt.hdt.util.io.CountOutputStream;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.listener.ListenerUtil;
import org.rdfhdt.hdt.util.string.ByteStringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * Implementation of {@link org.rdfhdt.hdt.dictionary.DictionarySectionPrivate} that write loaded
 * {@link org.rdfhdt.hdt.dictionary.TempDictionarySection} on disk before saving, reducing the size in ram
 *
 * @author Antoine Willerval
 */
public class WriteDictionarySection implements DictionarySectionPrivate {
	private final CloseSuppressPath tempFilename;
	private final CloseSuppressPath blockTempFilename;
	private SequenceLog64BigDisk blocks;
	private final long blockSize;
	private final int bufferSize;
	private long numberElements = 0;
	private long byteoutSize;

	public WriteDictionarySection(HDTOptions spec, Path filename, int bufferSize) {
		this.bufferSize = bufferSize;
		String fn = filename.getFileName().toString();
		tempFilename = CloseSuppressPath.of(filename.resolveSibling(fn + "_temp"));
		blockTempFilename = CloseSuppressPath.of(filename.resolveSibling(fn + "_tempblock"));
		long blockSize = spec.getInt("pfc.blocksize");
		if (blockSize < 0) {
			throw new IllegalArgumentException("negative pfc.blocksize");
		} else if (blockSize == 0) {
			this.blockSize = PFCDictionarySection.DEFAULT_BLOCK_SIZE;
		} else {
			this.blockSize = blockSize;
		}
	}

	@Override
	public void load(TempDictionarySection other, ProgressListener plistener) {
		MultiThreadListener listener = ListenerUtil.multiThreadListener(plistener);
		long otherN = other.getNumberOfElements();
		long block = otherN < 10 ? 1 : otherN / 10;
		long currentCount = 0;
		blocks = new SequenceLog64BigDisk(blockTempFilename.toAbsolutePath().toString(), 64, otherN / blockSize);

		listener.notifyProgress(0, "Filling section");
		try (CountOutputStream out = new CountOutputStream(tempFilename.openOutputStream(bufferSize))) {
			CRCOutputStream crcout = new CRCOutputStream(out, new CRC32());
			String previousStr = null;
			for (Iterator<? extends CharSequence> it = other.getSortedEntries(); it.hasNext(); currentCount++) {
				CharSequence sec = it.next();
				String str = sec.toString();
				if (numberElements % blockSize == 0) {
					blocks.append(out.getTotalBytes());

					// Copy full string
					ByteStringUtil.append(out, str, 0);
				} else {
					// Find common part.
					int delta = ByteStringUtil.longestCommonPrefix(previousStr, str);
					// Write Delta in VByte
					VByte.encode(out, delta);
					// Write remaining
					ByteStringUtil.append(out, str, delta);
				}
				out.write(0);
				previousStr = str;
				numberElements++;
				if (currentCount % block == 0) {
					listener.notifyProgress((float) (currentCount * 100 / otherN), "Filling section");
				}
			}

			byteoutSize = out.getTotalBytes();
			crcout.writeCRC();
		} catch (IOException e) {
			throw new RuntimeException("can't load section", e);
		}
		blocks.append(byteoutSize);
		// Trim text/blocks
		blocks.aggressiveTrimToSize();
		if (numberElements % 100_000 == 0) {
			listener.notifyProgress(100, "Completed section filling");
		}
	}

	@Override
	public void save(OutputStream output, ProgressListener listener) throws IOException {
		CRCOutputStream out = new CRCOutputStream(output, new CRC8());
		out.write(PFCDictionarySection.TYPE_INDEX);
		VByte.encode(out, numberElements);

		VByte.encode(out, byteoutSize);
		VByte.encode(out, blockSize);
		out.writeCRC();
		// Write blocks directly to output, they have their own CRC check.
		blocks.save(output, listener);
		// Write blocks data directly to output, the load was writing using a CRC check.
		Files.copy(tempFilename, output);
	}

	@Override
	public void load(InputStream input, ProgressListener listener) throws IOException {
		throw new NotImplementedException();
	}

	@Override
	public long locate(CharSequence s) {
		throw new NotImplementedException();
	}

	@Override
	public CharSequence extract(long pos) {
		throw new NotImplementedException();
	}

	@Override
	public long size() {
		return numberElements;
	}

	@Override
	public long getNumberOfElements() {
		return numberElements;
	}

	@Override
	public Iterator<? extends CharSequence> getSortedEntries() {
		throw new NotImplementedException();
	}

	@Override
	public void close() throws IOException {
		IOUtil.closeAll(blocks, tempFilename, blockTempFilename);
	}
}
