/**
 * File: $HeadURL$
 * Revision: $Rev$
 * Last modified: $Date$
 * Last modified by: $Author$
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

package org.rdfhdt.hdt.hdt;

/**
 * @author mario.arias
 *
 */
public class HDTVocabulary {
	public static final String HDT_BASE = "<http://purl.org/HDT/hdt#";
	public static final String HDT_HEADER = HDT_BASE+"header";
	public static final String HDT_DICTIONARY_BASE = HDT_BASE+"dictionary";
	public static final String HDT_TRIPLES_BASE = HDT_BASE+"triples";
	public static final String HDT_ARRAY_BASE = HDT_BASE+"array";
	public static final String HDT_BITMAP_BASE = HDT_BASE+"bitmap";

	public static final String HDT_DATASET = HDT_BASE+"Dataset>";
	public static final String HDT_FORMAT_INFORMATION = HDT_BASE+"formatInformation>";
	public static final String HDT_STATISTICAL_INFORMATION = HDT_BASE+"statisticalInformation>";
	public static final String HDT_PUBLICATION_INFORMATION = HDT_BASE+"publicationInformation>";
	public static final String HDT_DICTIONARY = HDT_DICTIONARY_BASE+">";
	public static final String HDT_TRIPLES = HDT_TRIPLES_BASE+">";

	// External Vocabularies
	public static final String RDF = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static final String RDF_TYPE = RDF+"type>";
	public static final String DUBLIN_CORE = "<http://purl.org/dc/terms/";
	public static final String DUBLIN_CORE_ISSUED = DUBLIN_CORE+"issued>";

	// Header
	public static final String HEADER_PLAIN = HDT_HEADER+"plain>";

	// Dictionary
	public static final String DICTIONARY_TYPE = DUBLIN_CORE+"format>";
	public static final String DICTIONARY_NUMSUBJECTS = HDT_DICTIONARY_BASE+"numSubjects>";
	public static final String DICTIONARY_NUMPREDICATES = HDT_DICTIONARY_BASE+"numPredicates>";
	public static final String DICTIONARY_NUMOBJECTS = HDT_DICTIONARY_BASE+"numObjects>";
	public static final String DICTIONARY_NUMSHARED = HDT_DICTIONARY_BASE+"numSharedSubjectObject>";
	public static final String DICTIONARY_MAXSUBJECTID = HDT_DICTIONARY_BASE+"maxSubjectID>";
	public static final String DICTIONARY_MAXPREDICATEID = HDT_DICTIONARY_BASE+"maxPredicateID>";
	public static final String DICTIONARY_MAXOBJECTTID = HDT_DICTIONARY_BASE+"maxObjectID>";
	public static final String DICTIONARY_MAPPING = HDT_DICTIONARY_BASE+"mapping>";
	public static final String DICTIONARY_SIZE_STRINGS = HDT_DICTIONARY_BASE+"sizeStrings>";
	public static final String DICTIONARY_BLOCK_SIZE = HDT_DICTIONARY_BASE+"numBlocks>";

	// Dictionary Types
	public static final String DICTIONARY_TYPE_PLAIN = HDT_DICTIONARY_BASE+"Plain>";
	public static final String DICTIONARY_TYPE_PFC = HDT_DICTIONARY_BASE+"FrontCoding>";

	// Triples
	public static final String TRIPLES_TYPE = DUBLIN_CORE+"format>";
	public static final String TRIPLES_NUM_TRIPLES = HDT_TRIPLES_BASE+"numTriples>";
	public static final String TRIPLES_ORDER = HDT_TRIPLES_BASE+"Order>";
	public static final String TRIPLES_ARRAYX_TYPE = HDT_TRIPLES_BASE+"arrayX>";
	public static final String TRIPLES_ARRAYY_TYPE = HDT_TRIPLES_BASE+"arrayY>";
	public static final String TRIPLES_ARRAYZ_TYPE = HDT_TRIPLES_BASE+"arrayZ>";
	public static final String TRIPLES_ARRAYX_SIZE = HDT_TRIPLES_BASE+"arrayXsize>";
	public static final String TRIPLES_ARRAYY_SIZE = HDT_TRIPLES_BASE+"arrayYsize>";
	public static final String TRIPLES_ARRAYZ_SIZE = HDT_TRIPLES_BASE+"arrayZsize>";
	public static final String TRIPLES_BITMAPX_SIZE = HDT_TRIPLES_BASE+"bitmapXsize>";
	public static final String TRIPLES_BITMAPY_SIZE = HDT_TRIPLES_BASE+"bitmapYsize>";
	public static final String TRIPLES_BITMAPZ_SIZE = HDT_TRIPLES_BASE+"bitmapZsize>";

	// Triples types
	public static final String TRIPLES_TYPE_TRIPLESLIST = HDT_TRIPLES_BASE+"List>";
	public static final String TRIPLES_TYPE_TRIPLESLISTDISK = HDT_TRIPLES_BASE+"ListDisk>";
	public static final String TRIPLES_TYPE_PLAIN = HDT_TRIPLES_BASE+"Plain>";
	public static final String TRIPLES_TYPE_COMPACT = HDT_TRIPLES_BASE+"Compact>";
	public static final String TRIPLES_TYPE_BITMAP = HDT_TRIPLES_BASE+"Bitmap>";
	public static final String TRIPLES_TYPE_FOQ = HDT_TRIPLES_BASE+"FOQ>";

	// Streams
	public static final String ARRAY_TYPE_INTEGER = HDT_ARRAY_BASE+"Integer>";
	public static final String ARRAY_TYPE_LOG = HDT_ARRAY_BASE+"Log>";
	public static final String ARRAY_TYPE_LOG32 = HDT_ARRAY_BASE+"Log32>";
	public static final String ARRAY_TYPE_HUFFMAN = HDT_ARRAY_BASE+"Huffman>";
	public static final String ARRAY_TYPE_WAVELET = HDT_ARRAY_BASE+"Wavelet>";

	// Bitmaps
	public static final String BITMAP_TYPE_375 = HDT_BITMAP_BASE+"375>";
	
    // OTHER
	public static final String ORIGINAL_SIZE = HDT_BASE+"originalSize>";
	public static final String HDT_SIZE = HDT_BASE+"hdtSize>";
}
