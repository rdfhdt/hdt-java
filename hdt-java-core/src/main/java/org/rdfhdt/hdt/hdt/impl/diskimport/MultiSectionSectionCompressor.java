package org.rdfhdt.hdt.hdt.impl.diskimport;

import org.rdfhdt.hdt.iterator.utils.AsyncIteratorFetcher;
import org.rdfhdt.hdt.listener.MultiThreadListener;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.util.LiteralsUtils;
import org.rdfhdt.hdt.util.io.CloseSuppressPath;
import org.rdfhdt.hdt.util.string.ByteString;
import org.rdfhdt.hdt.util.string.CompactString;

/**
 * Implementation of SectionCompressor for MultiSection
 */
public class MultiSectionSectionCompressor extends SectionCompressor {
	public MultiSectionSectionCompressor(CloseSuppressPath baseFileName, AsyncIteratorFetcher<TripleString> source, MultiThreadListener listener, int bufferSize, long chunkSize, int k, boolean debugSleepKwayDict) {
		super(baseFileName, source, listener, bufferSize, chunkSize, k, debugSleepKwayDict);
	}

	@Override
	protected ByteString convertObject(CharSequence seq) {
		return new CompactString(LiteralsUtils.litToPref(seq));
	}
}
