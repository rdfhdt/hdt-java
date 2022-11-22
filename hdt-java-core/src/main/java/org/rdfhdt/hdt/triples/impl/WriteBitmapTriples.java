package org.rdfhdt.hdt.triples.impl;

import org.rdfhdt.hdt.compact.bitmap.AppendableWriteBitmap;
import org.rdfhdt.hdt.compact.sequence.SequenceLog64BigDisk;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.exceptions.IllegalFormatException;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.header.Header;
import org.rdfhdt.hdt.iterator.SuppliableIteratorTripleID;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInfo;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTOptionsKeys;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TempTriples;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TriplesPrivate;
import org.rdfhdt.hdt.util.BitUtil;
import org.rdfhdt.hdt.util.io.CloseSuppressPath;
import org.rdfhdt.hdt.util.io.CountInputStream;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.listener.IntermediateListener;
import org.rdfhdt.hdt.util.listener.ListenerUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * Appendable write {@link org.rdfhdt.hdt.triples.impl.BitmapTriples} version
 *
 * @author Antoine Willerval
 */
public class WriteBitmapTriples implements TriplesPrivate {
	protected TripleComponentOrder order;
	private long numTriples;
	private final AppendableWriteBitmap bitY, bitZ;
	private final CloseSuppressPath seqY, seqZ, triples;
	private SequenceLog64BigDisk vectorY, vectorZ;

	public WriteBitmapTriples(HDTOptions spec, CloseSuppressPath triples, int bufferSize) throws IOException {
		String orderStr = spec.get(HDTOptionsKeys.TRIPLE_ORDER_KEY);
		if(orderStr == null) {
			this.order = TripleComponentOrder.SPO;
		} else {
			this.order = TripleComponentOrder.valueOf(orderStr);
		}
		triples.mkdirs();
		triples.closeWithDeleteRecurse();
		this.triples = triples;
		bitY = new AppendableWriteBitmap(triples.resolve("bitmapY"), bufferSize);
		bitZ = new AppendableWriteBitmap(triples.resolve("bitmapZ"), bufferSize);
		seqY = triples.resolve("seqY");
		seqZ = triples.resolve("seqZ");
	}

	@Override
	public void save(OutputStream output, ControlInfo ci, ProgressListener listener) throws IOException {
		ci.clear();
		ci.setFormat(getType());
		ci.setInt("order", order.ordinal());
		ci.setType(ControlInfo.Type.TRIPLES);
		ci.save(output);

		IntermediateListener iListener = new IntermediateListener(listener);
		bitY.save(output, iListener);
		bitZ.save(output, iListener);
		vectorY.save(output, iListener);
		vectorZ.save(output, iListener);
	}

	@Override
	public IteratorTripleID searchAll() {
		throw new NotImplementedException();
	}

	@Override
	public SuppliableIteratorTripleID search(TripleID pattern) {
		throw new NotImplementedException();
	}

	@Override
	public long getNumberOfElements() {
		return numTriples;
	}

	@Override
	public long size() {
		return numTriples * 4;
	}

	@Override
	public void populateHeader(Header header, String rootNode) {
		if (rootNode == null || rootNode.length() == 0) {
			throw new IllegalArgumentException("Root node for the header cannot be null");
		}

		header.insert(rootNode, HDTVocabulary.TRIPLES_TYPE, getType());
		header.insert(rootNode, HDTVocabulary.TRIPLES_NUM_TRIPLES, getNumberOfElements());
		header.insert(rootNode, HDTVocabulary.TRIPLES_ORDER, order.toString());
//		header.insert(rootNode, HDTVocabulary.TRIPLES_SEQY_TYPE, seqY.getType() );
//		header.insert(rootNode, HDTVocabulary.TRIPLES_SEQZ_TYPE, seqZ.getType() );
//		header.insert(rootNode, HDTVocabulary.TRIPLES_SEQY_SIZE, seqY.size() );
//		header.insert(rootNode, HDTVocabulary.TRIPLES_SEQZ_SIZE, seqZ.size() );
//		if(bitmapY!=null) {
//			header.insert(rootNode, HDTVocabulary.TRIPLES_BITMAPY_SIZE, bitmapY.getSizeBytes() );
//		}
//		if(bitmapZ!=null) {
//			header.insert(rootNode, HDTVocabulary.TRIPLES_BITMAPZ_SIZE, bitmapZ.getSizeBytes() );
//		}
	}

	@Override
	public String getType() {
		return HDTVocabulary.TRIPLES_TYPE_BITMAP;
	}

	@Override
	public TripleID findTriple(long position) {
		throw new NotImplementedException();
	}

	@Override
	public void load(InputStream input, ControlInfo ci, ProgressListener listener) {
		throw new NotImplementedException();
	}

	@Override
	public void mapFromFile(CountInputStream in, File f, ProgressListener listener) {
		throw new NotImplementedException();
	}

	@Override
	public void generateIndex(ProgressListener listener, HDTOptions disk) {
		throw new NotImplementedException();
	}

	@Override
	public void loadIndex(InputStream input, ControlInfo ci, ProgressListener listener) {
		throw new NotImplementedException();
	}

	@Override
	public void mapIndex(CountInputStream input, File f, ControlInfo ci, ProgressListener listener) {
		throw new NotImplementedException();
	}

	@Override
	public void saveIndex(OutputStream output, ControlInfo ci, ProgressListener listener) {
		throw new NotImplementedException();
	}

	@Override
	public void load(TempTriples triples, ProgressListener listener) {
		triples.setOrder(order);
		triples.sort(listener);

		IteratorTripleID it = triples.searchAll();

		long number = it.estimatedNumResults();

		vectorY = new SequenceLog64BigDisk(seqY.toAbsolutePath().toString(), BitUtil.log2(number));
		vectorZ = new SequenceLog64BigDisk(seqZ.toAbsolutePath().toString(), BitUtil.log2(number));

		long lastX = 0, lastY = 0, lastZ = 0;
		long x, y, z;
		numTriples = 0;

		while (it.hasNext()) {
			TripleID triple = it.next();
			TripleOrderConvert.swapComponentOrder(triple, TripleComponentOrder.SPO, order);

			x = triple.getSubject();
			y = triple.getPredicate();
			z = triple.getObject();
			if (x == 0 || y == 0 || z == 0) {
				throw new IllegalFormatException("None of the components of a triple can be null");
			}

			if (numTriples == 0) {
				// First triple
				vectorY.append(y);
				vectorZ.append(z);
			} else if (x != lastX) {
				if (x != lastX + 1) {
					throw new IllegalFormatException("Upper level must be increasing and correlative.");
				}
				// X changed
				bitY.append(true);
				vectorY.append(y);

				bitZ.append(true);
				vectorZ.append(z);
			} else if (y != lastY) {
				if (y < lastY) {
					throw new IllegalFormatException("Middle level must be increasing for each parent.");
				}

				// Y changed
				bitY.append(false);
				vectorY.append(y);

				bitZ.append(true);
				vectorZ.append(z);
			} else {
				if (z < lastZ) {
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

		if (numTriples > 0) {
			bitY.append(true);
			bitZ.append(true);
		}

		vectorY.aggressiveTrimToSize();
		vectorZ.aggressiveTrimToSize();
	}

	@Override
	public TripleComponentOrder getOrder() {
		return order;
	}

	@Override
	public void close() throws IOException {
		IOUtil.closeAll(
				bitY,
				bitZ,
				vectorY,
				seqY,
				vectorZ,
				seqZ,
				triples
		);
	}
}
