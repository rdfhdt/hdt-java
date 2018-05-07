package org.rdfhdt.hdt.triples.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.rdfhdt.hdt.compact.bitmap.Bitmap;
import org.rdfhdt.hdt.compact.bitmap.Bitmap375;
import org.rdfhdt.hdt.compact.bitmap.BitmapFactory;
import org.rdfhdt.hdt.compact.sequence.Sequence;
import org.rdfhdt.hdt.compact.sequence.SequenceFactory;
import org.rdfhdt.hdt.compact.sequence.SequenceLog64;
import org.rdfhdt.hdt.compact.sequence.SequenceLog64Map;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.util.BitUtil;
import org.rdfhdt.hdt.util.StopWatch;
import org.rdfhdt.hdt.util.io.CountInputStream;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.listener.IntermediateListener;
import org.rdfhdt.hdt.util.listener.ListenerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PredicateIndexArray implements PredicateIndex {
	private static final Logger log = LoggerFactory.getLogger(PredicateIndexArray.class);
	
	BitmapTriples triples;
	Sequence array;
	Bitmap bitmap;
	
	public PredicateIndexArray(BitmapTriples triples) {
		this.triples = triples;
	}
	
	public long getBase(long pred) {
		if(pred<=1) {
			return 0;
		}
		return bitmap.select1(pred-1)+1;
	}

	@Override
	public long getNumOcurrences(long pred) {
		return bitmap.select1(pred)-bitmap.select1(pred-1);
	}
	
	@Override
	public long getOccurrence(long base, long occ) {
		return array.get(base+occ-1);
	}
	
	@Override
	public void load(InputStream input) throws IOException {
		bitmap = BitmapFactory.createBitmap(input);
		bitmap.load(input, null);
		
		array = SequenceFactory.createStream(input);
		array.load(input, null);
	}
	
	@Override
	public void save(OutputStream out) throws IOException {
		bitmap.save(out, null);	
		array.save(out, null);
	}
	
	@Override
	public void generate(ProgressListener listener) {
		IntermediateListener iListener = new IntermediateListener(listener);
		StopWatch st = new StopWatch();
		@SuppressWarnings("resource")
		SequenceLog64 predCount = new SequenceLog64(BitUtil.log2(triples.getSeqY().getNumberOfElements()));
	
	    long maxCount = 0;
	    for(long i=0;i<triples.getSeqY().getNumberOfElements(); i++) {
	        // Read value
	        long val = triples.getSeqY().get(i);

	        // Grow if necessary
	        if(predCount.getNumberOfElements()<val) {
	            predCount.resize(val);
	        }

	        // Increment
	        long count = predCount.get(val-1)+1;
	        maxCount = count>maxCount ? count : maxCount;
	        predCount.set(val-1, count);

	        ListenerUtil.notifyCond(iListener,  "Counting appearances of predicates", i, triples.getSeqY().getNumberOfElements(), 20000);
	    }
	    predCount.aggressiveTrimToSize();
	    
	    // Convert predicate count to bitmap
	    Bitmap375 bitmap = new Bitmap375(triples.getSeqY().getNumberOfElements());
	    long tempCountPred=0;
	    for(long i=0;i<predCount.getNumberOfElements();i++) {
	        tempCountPred += predCount.get(i);
	        bitmap.set(tempCountPred-1,true);
	        ListenerUtil.notifyCond(iListener, "Creating Predicate bitmap", i, predCount.getNumberOfElements(), 100000);
	    }
	    bitmap.set(triples.getSeqY().getNumberOfElements()-1, true);
        log.info("Predicate Bitmap in {}", st.stopAndShow());
	    st.reset();
	    IOUtil.closeQuietly(predCount);
	    predCount=null;
	    
	    
	    // Create predicate index
	    SequenceLog64 array = new SequenceLog64(BitUtil.log2(triples.getSeqY().getNumberOfElements()), triples.getSeqY().getNumberOfElements());
	    array.resize(triples.getSeqY().getNumberOfElements());

	    SequenceLog64 insertArray = new SequenceLog64(BitUtil.log2(triples.getSeqY().getNumberOfElements()), bitmap.countOnes());
	    insertArray.resize(bitmap.countOnes());

	    for(long i=0;i<triples.getSeqY().getNumberOfElements(); i++) {
	            long predicateValue = triples.getSeqY().get(i);

	            long insertBase = predicateValue==1 ? 0 : bitmap.select1(predicateValue-1)+1;
	            long insertOffset = insertArray.get(predicateValue-1);
	            insertArray.set(predicateValue-1, insertOffset+1);

	            array.set(insertBase+insertOffset, i);
	            
	            ListenerUtil.notifyCond(iListener,  "Generating predicate references", i, triples.getSeqY().getNumberOfElements(), 100000);
	    }
	    IOUtil.closeQuietly(insertArray);

	    this.array = array;
	    this.bitmap = bitmap;
        log.info("Count predicates in {}", st.stopAndShow());
	}

	@Override
	public void mapIndex(CountInputStream input, File f, ProgressListener listener) throws IOException {
		bitmap = BitmapFactory.createBitmap(input);
		bitmap.load(input, null);
		
		array = new SequenceLog64Map(input, f);
	}

	@Override
	public void close() throws IOException {
		bitmap=null;
		if(array!=null) {
			array.close();
		}
		array=null;
	}
}
