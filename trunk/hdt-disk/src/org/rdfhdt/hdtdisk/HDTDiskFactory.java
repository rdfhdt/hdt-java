package org.rdfhdt.hdtdisk;

import java.util.HashSet;
import java.util.Set;

import org.rdfhdt.hdt.dictionary.TempDictionary;
import org.rdfhdt.hdt.dictionary.impl.BerkeleyDictionary;
import org.rdfhdt.hdt.dictionary.impl.JDBMDictionary;
import org.rdfhdt.hdt.dictionary.impl.KyotoDictionary;
import org.rdfhdt.hdt.hdt.TempDictTriplesFactory;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.triples.TempTriples;
import org.rdfhdt.hdt.triples.impl.TriplesBerkeley;
import org.rdfhdt.hdt.triples.impl.TriplesJDBM;

public class HDTDiskFactory implements TempDictTriplesFactory {

	public static final String TEMP_TRIPLES_IMPL_JDBM = "jdbm";
	public static final String TEMP_TRIPLES_IMPL_BERKELEY = "berkeley";
	public static final String TEMP_TRIPLES_IMPL_BERKELEY_NATIVE = "berkeleyNative";
	public static final String TEMP_TRIPLES_IMPL_KYOTO = "kyoto";
	
	public static final String TEMP_DICT_IMPL_JDBM = "jdbm";
	public static final String TEMP_DICT_IMPL_BERKELEY = "berkeley";
	public static final String TEMP_DICT_IMPL_BERKELEY_NATIVE = "berkeleyNative";
	public static final String TEMP_DICT_IMPL_KYOTO = "kyoto";
	
	private static Set<String> implementations = new HashSet<String>();
	
	static {
		implementations.add(TEMP_TRIPLES_IMPL_JDBM);
		implementations.add(TEMP_TRIPLES_IMPL_BERKELEY);
		implementations.add(TEMP_TRIPLES_IMPL_BERKELEY_NATIVE);
		implementations.add(TEMP_TRIPLES_IMPL_KYOTO);
		
		implementations.add(TEMP_DICT_IMPL_JDBM);
		implementations.add(TEMP_DICT_IMPL_BERKELEY);
		implementations.add(TEMP_DICT_IMPL_BERKELEY_NATIVE);
		implementations.add(TEMP_DICT_IMPL_KYOTO);
	}
	
	@Override
	public TempDictionary getDictionary(HDTOptions options) {
		String dictType = options.get("tempDictionary.impl");
		if(TEMP_DICT_IMPL_JDBM.equals(dictType)) {
			return new JDBMDictionary(options);
		} else if(TEMP_DICT_IMPL_BERKELEY.equals(dictType)) {
			return new BerkeleyDictionary(options);
//		} else if(TEMP_DICT_IMPL_BERKELEY_NATIVE.equals(dictType)) {
//			return new BerkeleyNative(options);
		} else if(TEMP_DICT_IMPL_KYOTO.equals(dictType)) {
			return new KyotoDictionary(options);
		}
		throw new IllegalArgumentException("Implementation of dictionary for "+dictType+" not found.");
	}

	@Override
	public TempTriples getTriples(HDTOptions options) {
		String triplesType = options.get("tempTriples.impl");
		if(TEMP_TRIPLES_IMPL_JDBM.equals(triplesType)) {
			return new TriplesJDBM(options);
		} else if(TEMP_TRIPLES_IMPL_BERKELEY.equals(triplesType)) {
			return new TriplesBerkeley(options);
//		} else if(TEMP_TRIPLES_IMPL_KYOTO.equals(triplesType)) {
//			return new TriplesKyoto(options);
		}
		throw new IllegalArgumentException("Implementation of triples for "+triplesType+" not found.");
	}
	
	public void checkTwoPass(HDTOptions spec) {
		if(implementations.contains(spec.get("tempTriples.impl")) || 
			implementations.contains(spec.get("tempDictionary.impl"))) {
			spec.set("loader.type","two-pass");
		}
	}
	
}
