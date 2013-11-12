package org.rdfhdt.hdt.dictionary.impl.section;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.rdfhdt.hdt.compact.integer.VByte;
import org.rdfhdt.hdt.compact.sequence.Sequence;
import org.rdfhdt.hdt.util.string.CompactString;
import org.rdfhdt.hdt.util.string.ReplazableString;

/**
 * Performs extract keeping local state that can be reused to read consecutive positions.
 * 
 * Not thread friendly.
 * 
 * @author Mario Arias
 *
 */

public class PFCOptimizedExtractor {
	PFCDictionarySectionMap pfc;
	long numstrings;
	int blocksize;
	ByteBuffer [] buffers;
	ByteBuffer buffer;
	long [] posFirst;
	Sequence blocks;

	long bytebufferIndex=0;
	ReplazableString tempString = new ReplazableString();
	long id = 0;

	public PFCOptimizedExtractor(PFCDictionarySectionMap pfc) {
		this.pfc = pfc;
		this.numstrings = pfc.numstrings;
		this.blocksize = pfc.blocksize;
		this.blocks = pfc.blocks;
		this.posFirst = pfc.posFirst;
		
		this.buffers = pfc.buffers;
		if(numstrings>0 && this.buffers!=null && this.buffers.length>0) {
			this.buffer = buffers[0].duplicate();
		} else {
			if(this.buffers==null) {
				System.err.println("Warning: Mapping a PFC section with null buffers. "+numstrings+" / "+blocksize+ " / "+pfc.dataSize+" / "+pfc.blocks.getNumberOfElements());
			} else if(this.buffers.length==0) {
				System.err.println("Warning: Mapping a PFC section with buffers but no entries. "+numstrings+" / "+blocksize+" / "+pfc.dataSize+" / "+pfc.blocks.getNumberOfElements()+" / "+buffers);
			}
			this.numstrings=0;
		}
	}

	public CharSequence extract(long target) {
		if(target<1 || target>numstrings) {
			throw new IndexOutOfBoundsException("Trying to access position "+target+ " but PFC has "+numstrings+" elements.");
		}

		if(target>id && target<( (id%blocksize)+blocksize) ) {
			// If the searched string is in the current block, just continue

			while(id<target) {
				if(!buffer.hasRemaining()) {
					buffer = buffers[(int) ++bytebufferIndex].duplicate();
					buffer.rewind();
				}
				try {
					if((id%blocksize)==0) {
						tempString.replace(buffer, 0);
					} else {				
						long delta = VByte.decode(buffer);
						tempString.replace(buffer, (int) delta);
					}
					id++;

					if(id==target) {
						return new CompactString(tempString).getDelayed();
						//							return tempString.toString();
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			// Should not reach here.
			System.err.println("Not found: "+target+" out of "+this.getNumStrings());
			return null;

		} else {
			// The searched string is in another block, seek directly to that one.

			id = target;

			long block = (target-1)/blocksize;
			bytebufferIndex = block/PFCDictionarySectionMap.BLOCKS_PER_BYTEBUFFER;
			buffer = buffers[(int) bytebufferIndex++].duplicate();
			buffer.position((int)(blocks.get(block)-posFirst[(int) (block/PFCDictionarySectionMap.BLOCKS_PER_BYTEBUFFER)]));

			try {
				tempString = new ReplazableString();
				tempString.replace(buffer,0);

				long stringid = (target-1)%blocksize;
				for(long i=0;i<stringid;i++) {
					long delta = VByte.decode(buffer);
					tempString.replace(buffer, (int) delta);
				}
				return new CompactString(tempString).getDelayed();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
	}
	
	public long getNumStrings() {
		return numstrings;
	}

}

