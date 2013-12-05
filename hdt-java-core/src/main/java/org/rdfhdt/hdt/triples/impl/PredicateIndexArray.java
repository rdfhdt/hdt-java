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
import org.rdfhdt.hdt.util.listener.IntermediateListener;
import org.rdfhdt.hdt.util.listener.ListenerUtil;

class PredicateIndexArray implements PredicateIndex {
	
	BitmapTriples triples;
	Sequence array;
	Bitmap bitmap;
	
	public PredicateIndexArray(BitmapTriples triples) {
		this.triples = triples;
	}
	
	private final long calculatePos(int pred) {
		if(pred<=1) {
			return 0;
		}
		return bitmap.select1(pred-1)+1;
	}
	
	public long getNumOcurrences(int pred) {
		return bitmap.select1(pred)-bitmap.select1(pred-1);
	}
	
	public long getOccurrence(int pred, long occ) {
		return array.get(calculatePos(pred)+occ-1);
	}
	
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
		SequenceLog64 predCount = new SequenceLog64(BitUtil.log2(triples.seqY.getNumberOfElements()));
	
	    long maxCount = 0;
	    for(long i=0;i<triples.seqY.getNumberOfElements(); i++) {
	        // Read value
	        long val = triples.seqY.get(i);

	        // Grow if necessary
	        if(predCount.getNumberOfElements()<val) {
	            predCount.resize(val);
	        }

	        // Increment
	        long count = predCount.get(val-1)+1;
	        maxCount = count>maxCount ? count : maxCount;
	        predCount.set(val-1, count);

	        ListenerUtil.notifyCond(iListener,  "Counting appearances of predicates", i, triples.seqY.getNumberOfElements(), 20000);
	    }
	    predCount.aggresiveTrimToSize();
	    
	    // Convert predicate count to bitmap
	    Bitmap375 bitmap = new Bitmap375(triples.seqY.getNumberOfElements());
	    long tempCountPred=0;
	    for(long i=0;i<predCount.getNumberOfElements();i++) {
	        tempCountPred += predCount.get(i);
	        bitmap.set(tempCountPred-1,true);
	        ListenerUtil.notifyCond(iListener, "Creating Predicate bitmap", i, predCount.getNumberOfElements(), 100000);
	    }
	    bitmap.set(triples.seqY.getNumberOfElements()-1, true);
	    System.out.println("Predicate Bitmap in " + st.stopAndShow());
	    st.reset();

	    predCount=null;
	    
	    
	    // Create predicate index
	    SequenceLog64 array = new SequenceLog64(BitUtil.log2(triples.seqY.getNumberOfElements()), triples.seqY.getNumberOfElements());
	    array.resize(triples.seqY.getNumberOfElements());

	    SequenceLog64 insertArray = new SequenceLog64(BitUtil.log2(triples.seqY.getNumberOfElements()), bitmap.countOnes());
	    insertArray.resize(bitmap.countOnes());

	    for(long i=0;i<triples.seqY.getNumberOfElements(); i++) {
	            long predicateValue = triples.seqY.get(i);

	            long insertBase = predicateValue==1 ? 0 : bitmap.select1(predicateValue-1)+1;
	            long insertOffset = insertArray.get(predicateValue-1);
	            insertArray.set(predicateValue-1, insertOffset+1);

	            array.set(insertBase+insertOffset, i);
	            
	            ListenerUtil.notifyCond(iListener,  "Generating predicate references", i, triples.seqY.getNumberOfElements(), 100000);
	    }

	    this.array = array;
	    this.bitmap = bitmap;
	    System.out.println("Count predicates in "+st.stopAndShow());
	}

	@Override
	public void mapIndex(CountInputStream input, File f, ProgressListener listener) throws IOException {
		bitmap = BitmapFactory.createBitmap(input);
		bitmap.load(input, null);
		
		array = new SequenceLog64Map(input, f);
	}
}
