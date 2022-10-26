package org.rdfhdt.hdt.util;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.iterator.utils.PipedCopyIterator;
import org.rdfhdt.hdt.rdf.RDFParserFactory;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.triples.impl.utils.HDTTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LargeFakeDataSetStreamSupplierTest {
    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();
    @Test
    public void streamTest() throws IOException {
        LargeFakeDataSetStreamSupplier triples = LargeFakeDataSetStreamSupplier.createSupplierWithMaxTriples(10, 10);
        Path f = tempDir.newFolder().toPath();
        Path testNt = f.resolve("test.nt");
        triples.createNTFile(testNt.toAbsolutePath().toString());
        triples.reset();

        Iterator<TripleString> it2 = triples.createTripleStringStream();
        try (InputStream is = Files.newInputStream(testNt)) {
            try (PipedCopyIterator<TripleString> it = RDFParserFactory.readAsIterator(
                    RDFParserFactory.getParserCallback(RDFNotation.NTRIPLES),
                    is,
                    HDTTestUtils.BASE_URI,
                    true,
                    RDFNotation.NTRIPLES
            )) {
                it.forEachRemaining(s -> {
                    assertTrue(it2.hasNext());
                    assertEquals(it2.next(), s);
                });
                assertFalse(it.hasNext());
            }
        }
    }
}
