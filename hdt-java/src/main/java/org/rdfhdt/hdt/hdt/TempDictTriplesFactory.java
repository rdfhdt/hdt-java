package org.rdfhdt.hdt.hdt;

import org.rdfhdt.hdt.dictionary.TempDictionary;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.triples.TempTriples;

public interface TempDictTriplesFactory {
	void checkTwoPass(HDTOptions spec);
	TempDictionary getDictionary(HDTOptions options);
	TempTriples getTriples(HDTOptions options);
}
