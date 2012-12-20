/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/util/BitUtil.java $
 * Revision: $Rev: 51 $
 * Last modified: $Date: 2012-08-16 12:20:52 +0100 (Thu, 16 Aug 2012) $
 * Last modified by: $Author: eugen.rozic $
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Contacting the authors:
 *   Mario Arias:               mario.arias@deri.org
 *   Javier D. Fernandez:       jfergar@infor.uva.es
 *   Miguel A. Martinez-Prieto: migumar2@infor.uva.es
 *   Alejandro Andres:          fuzzy.alej@gmail.com
 */
package org.rdfhdt.hdtdisk.util;

import org.rdfhdt.hdt.dictionary.DictionaryFactory;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TriplesFactory;
import org.rdfhdt.hdt.util.RDFInfo;

/**
 * This class contains methods for calculating the sizes of caches
 * for on-disk implementations of TempTriples and ModifiableDictionaries
 * 
 * @author Eugen Rozic
 */
public class CacheCalculator {
	
	/**
	 * This method gets the "tempTriples.cache" property from the specifications. If the property is
	 * unset or set to "auto" then this method will calculate the amount of cache for a
	 * disk-based implementation of TempTriples.
	 * 
	 * The calculation is done with the following formula:
	 * Xmx - (2*expectedCompression*sizeOfRDF) - tempDictionary.Cache.
	 * 
	 * if the type of the dictionary is "on-disk"(much more likely) then if the dictionary cache
	 * is already set it is just subtracted (as seen in the formula), and if it's not already
	 * set then the amount of cache given to triples is 1/4 of the total available memory for
	 * all caches (calculated by the Xmx - (2*expectedCompression*sizeOfRDF) formula)
	 * 
	 * If the Xmx of sizeOfRDF properties are not set a runtime exception is throws because
	 * the calculation cannot be made. The compression parameter will always be set due to the
	 * implementation of the RDFInfro.getcompression method.
	 * 
	 * If the type of the dictionary is "in-memory" then the memory given to the triples cache is
	 * 5% of the total memory (Xmx).
	 * 
	 * @throws NumberFormatException if the calculated cache size is negative
	 * @return the size of the cache for modTriples in bytes
	 */
	public static long getTriplesCache(HDTSpecification specs){
		long bytes = specs.getBytesProperty("tempTriples.cache");
		if (bytes!=-1){
			return bytes;
		}
		
		System.out.println("No value set for size of triples cache -> calculating...");
		
		String dictionaryType = specs.get("tempDictionary.type");
		long dictionaryCache = specs.getBytesProperty("tempDictionary.cache");
		
		long xmx = Runtime.getRuntime().maxMemory();
		if (xmx==Long.MAX_VALUE) {
			throw new RuntimeException("Unable to calculate cache because max heap size not specified. Please set Xmx parameter.");
		}
		
		xmx = (long)(0.95*xmx); //for safety, logically based on the size of the survivor area of new generation (0.1*0.5Xmx=0.05Xmx)
		Long sizeOfRDF = RDFInfo.getSizeInBytes(specs);
		if (sizeOfRDF==null) {
			throw new RuntimeException("Cannot calculate cache if size of input RDF file in bytes is unknown");
		}
		
		double compression = RDFInfo.getCompression(specs);
		
		if (DictionaryFactory.MOD_DICT_TYPE_ON_DISK.equals(dictionaryType)){
			bytes = xmx - (long)(2*compression*sizeOfRDF);
			if (dictionaryCache!=-1){
				//if dictionary cache already calculated/set then just subtract
				bytes -= dictionaryCache;
			} else {
				//else if not yet calculated then just split the cache in 1:3 ratio, 1/4 to triples, 3/4 to dictionary
				bytes = (long)(bytes*0.25); //FIXME hard-coded ratio!!
			}
		} else if (DictionaryFactory.MOD_DICT_TYPE_IN_MEM.equals(dictionaryType)){
			//TODO what is the formula if the dictionary is in_mem and triples on_disk (little likely combination)
			bytes = (long)(xmx*0.05); //FIXME 5% of memory total guesstimation (assumption, lots of memory for in-mem dictionary)
		} else {
			throw new RuntimeException("Cannot calculate triples cache if type of modDictionary is unknown.");
		}
		
		if (bytes<0)
			throw new NumberFormatException("Triples cache caluclated to a negative " +
					"value, meaning not enough memory is reserved with -Xmx. If you think " +
					"there is enough memory or want to force operation lower the " +
					"\"rdf.expectedCompression\" parameter in the hdtcfg file.");
		System.out.println("Triples cache calculated to: "+bytes+" bytes");
		specs.set("tempTriples.cache", Long.toString(bytes));
		return bytes;
	}
	
	/**
	 * This method gets the "tempDictionary.cache" property form the specifications. If the property is
	 * unset or set to "auto" then this method will calculate the amount of cache for a
	 * disk-based implementation of TempDictionary.
	 * 
	 * The calculation is done with the following formula:
	 * Xmx - (2*expectedCompression*sizeOfRDF) - tempTriples.Cache.
	 * 
	 * If the type of the triples is "on-disk" then if the triples cache
	 * is already set it is just subtracted (as seen in the formula), and if it's not already
	 * set then the amount of cache given to the dictionary cache is 3/4 of the total available memory for
	 * all caches (calculated by the Xmx - (2*expectedCompression*sizeOfRDF) formula)
	 * 
	 * If the Xmx of sizeOfRDF properties are not set a runtime exception is thrown because
	 * the calculation cannot be made. The compression parameter will always be set due to the
	 * implementation of the RDFInfro.getcompression method.
	 * 
	 * If the type of the triples is "in-memory" then:
	 * 		if the implementation is TriplesList the formula is:
	 *			Xmx - sizeOfTriplesInMem - max(sizeOfTriplesInMem, 2*expectedCompression*sizeOfRDF)
	 *		if the implementation is TriplesSet the formula is:
	 *			Xmx - sizeOfTriplesInMem - 2*expectedCompression*sizeOfRDF
	 *
	 *		where sizeOfTriplesInMem is equal to numLines*sizeof(TripleID) if the "rdf.lines" property
	 *		is set, or guessNumLines*sizeOf(TriplesID) where the guessed number of lines is equal to
	 *		sizeOfRDF/100 meaning that a guess of 100 bytes per triple is made.
	 * 
	 * 
	 * 
	 * @throws NumberFormatException if the calculated cache size is negative
	 * @return the size of the cache for modDictionary in bytes
	 */
	public static long getDictionaryCache(HDTSpecification specs){
		long bytes = specs.getBytesProperty("tempDictionary.cache");
		if (bytes!=-1){
			return bytes;
		}
		
		System.out.println("No value set for size of dictionary cache -> calculating...");
		
		String triplesType = specs.get("tempTriples.type");
		String triplesImpl = specs.get("tempTriples.impl");
		long triplesCache = specs.getBytesProperty("tempTriples.cache");
		
		long Xmx = Runtime.getRuntime().maxMemory();
		if (Xmx==Long.MAX_VALUE)
			throw new RuntimeException("Unable to calculate cache because max heap size not specified. Please set Xmx parameter.");
		Xmx = (long)(0.95*Xmx); //for safety, logically based on the size of the survivor area of new generation (0.1*0.5Xmx=0.05Xmx)
		Long sizeOfRDF = RDFInfo.getSizeInBytes(specs);
		if (sizeOfRDF==null)
			throw new RuntimeException("Cannot calculate cache if size of input RDF file in bytes is unknown");
		long sizeOfTriplesInMem = 0;
		long numLines = RDFInfo.getLines(specs);
		if (numLines<=0){
			sizeOfTriplesInMem = (long)(sizeOfRDF*TripleID.size()/100.0); //estimating 100 bytes per line of RDF
		} else {
			sizeOfTriplesInMem = numLines*TripleID.size();
		}
		double compression = RDFInfo.getCompression(specs);
		
		if (TriplesFactory.MOD_TRIPLES_TYPE_ON_DISK.equals(triplesType)){
			bytes = Xmx - (long)(2*compression*sizeOfRDF);
			if (triplesCache!=-1){
				//if dictionary cache already calculated/set then just subtract
				bytes -= triplesCache;
			} else {
				//else if not yet calculated then just split the cache in 1:3 ratio, 1/4 to triples, 3/4 to dictionary
				bytes = (long)(bytes*0.75); //FIXME hard-coded ratio!!
			}
		} else if (TriplesFactory.MOD_TRIPLES_TYPE_IN_MEM.equals(triplesType)){
			if (TriplesFactory.MOD_TRIPLES_IMPL_LIST.equals(triplesImpl)){
				if ((sizeOfTriplesInMem)>((long)(2*compression*sizeOfRDF))) {
					bytes = Xmx - 2*sizeOfTriplesInMem;
				} else {
					bytes = Xmx - (sizeOfTriplesInMem + (long)(2*compression*sizeOfRDF));
				}
			} else if (TriplesFactory.MOD_TRIPLES_IMPL_SET.equals(triplesImpl)){
				//FIXME - this is bad...
				// TriplesSet takes more memory than just numTriples*sizeOf(TripleID)
				// because of the tree structure (treeNode is maybe even bigger than TripleID)
				System.out.println("Use of TriplesSet with on-disk dictionary DEPRECATED!! Use TriplesList");
				bytes = Xmx - (sizeOfTriplesInMem + (long)(2*compression*sizeOfRDF));
			} else
				throw new RuntimeException("Unknown in-memory triples implementation...");
		} else {
			throw new RuntimeException("Cannot calculate triples cache if type of modDictionary is unknown.");
		}
		
		if (bytes<0)
			throw new NumberFormatException("Dictionary cache caluclated to a negative " +
					"value, meaning not enough memory is reserved with -Xmx. If you think " +
					"there is enough memory or want to force operation lower the " +
					"\"rdf.expectedCompression\" parameter in the hdtcfg file.");
		System.out.println("Dictionary cache calculated to: "+bytes+" bytes");
		specs.set("tempDictionary.cache", Long.toString(bytes));
		return bytes;
	}
	
	/**
	 * Calculates (estimates) the number of records for a JDBM triples cache based on the amount of
	 * available bytes for the triples cache.
	 * 
	 * Empirically based formula is: at most 1500B for a record when 2048 records.
	 *  - If the cache is bigger a single record takes less bytes and that is ok (goes down to 1K, maybe if huge 0.5K...
	 *  cache not that important to adjust this perfectly)
	 *  - If the cache is smaller a single record takes more bytes, but here number of records is important
	 *  because it is small enough as it is. If records<2048 when bytesPerRecord = 1500 drop linearly from
	 *  (2048,1500) to (1024,3000) and then keep at maximum bytesPerRecord of 3000.
	 */
	public static int getJDBMTriplesCache(HDTSpecification specs){
		//TODO some smarter, better way of doing this... extensive testing for more precise empirical conclusion at least
		long bytes = getTriplesCache(specs);
		int bytesPerRecord = 1500;
		
		int records = (int)(bytes/bytesPerRecord);
		if (records<2048) {
			bytesPerRecord = (int)(4500 - 1.4648*records); //linear drop from (2048,1500) to (1024,3000)
			bytesPerRecord = (bytesPerRecord>3000)?3000:bytesPerRecord; //limit to 3000B per record
			records = (int)(bytes/bytesPerRecord);
		}
		
		System.out.println("JDBM triples cache calculated to: "+records+" records");
		return records;
	}
	
	/**
	 * Calculates (estimates) the number of records for a JDBM dictionary cache based on the amount of
	 * available bytes for the dictionary cache.
	 * 
	 * Empirically based formula is: at most 5500B for a record when 2048 records.
	 *  - If the cache is bigger a single record takes less bytes and that is ok(goes down to 3K, maybe if huge 2K...
	 *  cache not that important to adjust this perfectly)
	 *  - If the cache is smaller a single record takes more bytes, but here number of records is important
	 *  because it is small enough as it is. If records<2048 when bytesPerRecord = 5500 drop linearly from
	 *  (2048,5500) to (1024,11000) and then keep at maximum bytesPerRecord of 11000.
	 */
	public static int getJDBMDictionaryCache(HDTSpecification specs){
		//TODO some smarter, better way of doing this... extensive testing for more precise empirical conclusion at least
		long bytes = getDictionaryCache(specs);
		int bytesPerRecord = 5500;
		
		int records = (int)(bytes/bytesPerRecord);
		if (records<2048) {
			bytesPerRecord = (int)(16500 - 5.3711*records); //linear drop from (2048,5500) to (1024,11000)
			bytesPerRecord = (bytesPerRecord>11000)?11000:bytesPerRecord; //limit to 11000B per record
			records = (int)(bytes/bytesPerRecord);
		}
		
		System.out.println("JDBM dictionary cache calculated to: "+records+" records");
		return records;
	}

}
