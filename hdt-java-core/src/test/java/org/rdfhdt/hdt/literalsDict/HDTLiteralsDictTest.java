package org.rdfhdt.hdt.literalsDict;

import org.junit.Test;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.dictionary.impl.MultipleSectionDictionary;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.hdtCat.utils.Utility;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class HDTLiteralsDictTest {

    @Test
    public void testIdConversion() throws ParserException, IOException, NotFoundException {
        ClassLoader classLoader = getClass().getClassLoader();
        String file1 = Objects.requireNonNull(classLoader.getResource("example4+5.nt")).getFile();
        HDTSpecification spec = new HDTSpecification();
        spec.setOptions("tempDictionary.impl=multHash;dictionary.type=dictionaryMultiObj;");
        try (HDT hdt1 = HDTManager.generateHDT(new File(file1).getAbsolutePath(), "uri", RDFNotation.NTRIPLES, spec, null)) {
            IteratorTripleString iterator = hdt1.search("","","");
            while (iterator.hasNext()){
                TripleString next = iterator.next();
                System.out.println(next);

                long subId = hdt1.getDictionary().stringToId(next.getSubject().toString(), TripleComponentRole.SUBJECT);
                String subj = hdt1.getDictionary().idToString(subId,TripleComponentRole.SUBJECT).toString();
                assertEquals(next.getSubject(), subj);

                long predId = hdt1.getDictionary().stringToId(next.getPredicate().toString(), TripleComponentRole.PREDICATE);
                String pred = hdt1.getDictionary().idToString(predId,TripleComponentRole.PREDICATE).toString();
                assertEquals(next.getPredicate(), pred);

                long objId = hdt1.getDictionary().stringToId(next.getObject().toString(), TripleComponentRole.OBJECT);
                String obj = hdt1.getDictionary().idToString(objId,TripleComponentRole.OBJECT).toString();
                assertEquals(next.getObject(), obj);
            }
        }
    }
    @Test
    public void testGetDataTypeRange() throws IOException, ParserException{
        ClassLoader classLoader = getClass().getClassLoader();
        String file1 = Objects.requireNonNull(classLoader.getResource("example22.nt")).getFile();
        HDTSpecification spec = new HDTSpecification();
        spec.setOptions("tempDictionary.impl=multHash;dictionary.type=dictionaryMultiObj;");
        try (HDT hdt = HDTManager.generateHDT(new File(file1).getAbsolutePath(), "uri", RDFNotation.NTRIPLES, spec, null)) {
            Dictionary dictionary = hdt.getDictionary();
            AbstractMap.SimpleEntry<Long, Long> dataTypeRange = ((MultipleSectionDictionary) dictionary).getDataTypeRange("http://www.w3.org/2001/XMLSchema#float");
            long lower = dataTypeRange.getKey();
            long upper = dataTypeRange.getValue();
            Utility.printTriples(hdt);
            assertEquals(5,lower);
            assertEquals(7,upper);
        }
    }
    @Test
    public void testGetDataTypeOfId() throws IOException, ParserException{
        ClassLoader classLoader = getClass().getClassLoader();
        String file1 = Objects.requireNonNull(classLoader.getResource("example22.nt")).getFile();
        HDTSpecification spec = new HDTSpecification();
        spec.setOptions("tempDictionary.impl=multHash;dictionary.type=dictionaryMultiObj;");
        try (HDT hdt = HDTManager.generateHDT(new File(file1).getAbsolutePath(), "uri", RDFNotation.NTRIPLES, spec, null)) {
            Dictionary dictionary = hdt.getDictionary();

            // first get the id of a given string
            long id = dictionary.stringToId("\"Ali Haidar\"@en", TripleComponentRole.OBJECT);
            // by default of there is no string datatype in the rdf file, the dictionary will create a section for the
            //  strings
            assertEquals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#langString>", dictionary.dataTypeOfId(id).toString());
        }
    }
}
