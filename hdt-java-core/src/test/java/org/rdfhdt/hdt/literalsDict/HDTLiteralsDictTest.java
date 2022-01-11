package org.rdfhdt.hdt.literalsDict;

import org.junit.Test;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class HDTLiteralsDictTest {

    @Test
    public void testIdConversion(){
        ClassLoader classLoader = getClass().getClassLoader();
        String file1 = classLoader.getResource("example4+5.nt").getFile();
        HDTSpecification spec = new HDTSpecification();
        spec.setOptions("tempDictionary.impl=multHash;dictionary.type=dictionaryMultiObj;");
        try {
            HDT hdt1 = HDTManager.generateHDT(new File(file1).getAbsolutePath(), "uri", RDFNotation.NTRIPLES, spec, null);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
