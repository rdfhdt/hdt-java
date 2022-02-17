package org.rdfhdt.hdt.triples.impl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.triples.impl.utils.HDTTestUtils;

import java.io.IOException;

public class BitmapTriplesIteratorPositionTest {
    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();
    HDTSpecification spec;

    @Before
    public void setUp() throws Exception {
        spec = new HDTSpecification();
    }

    @Test
    public void searchAllTest() throws IOException, NotFoundException {
        HDTTestUtils data = new HDTTestUtils(tempDir.newFile(), 100, 50, 75, 30, spec);

        IteratorTripleString it = data.searchForSPO(0,0,0);

        long index = 0L;
        while (it.hasNext()) {
            TripleString triple = it.next();
            long tripleIndex = it.getLastTriplePosition();

            // test if the search is returning index from 0, 1, ...count
            Assert.assertEquals("nextTriplePosition order", index++, tripleIndex);

            // test if the triple is at the right index
            HDTTestUtils.SpoId spoId = data.TripleToSpo(triple);
            long testIndex = spoId.getIndex();
            Assert.assertEquals("getIndex hdt value", testIndex, tripleIndex);
        }
    }
    private void searchTest(int s, int p, int o) throws IOException, NotFoundException {
        HDTTestUtils data = new HDTTestUtils(tempDir.newFile(), 100, 50, 75, 30, spec);

        IteratorTripleString it = data.searchForSPO(s, p, o);

        while (it.hasNext()) {
            TripleString triple = it.next();
            long tripleIndex = it.getLastTriplePosition();
            // test if the triple is at the right index
            HDTTestUtils.SpoId spoId = data.TripleToSpo(triple);
            long testIndex = spoId.getIndex();
            Assert.assertEquals("getIndex hdt value", testIndex, tripleIndex);
        }
    }
    @Test
    public void ___SearchTest() throws IOException, NotFoundException {
        searchTest(0,0,0); // Tested pattern: ???
    }
    @Test
    public void s__SearchTest() throws IOException, NotFoundException {
        searchTest(30,0,0); // Tested pattern: S??
    }
    @Test
    public void _p_SearchTest() throws IOException, NotFoundException {
        searchTest(0,40,0); // Tested pattern: ?P?
    }
    @Test
    public void sp_SearchTest() throws IOException, NotFoundException {
        searchTest(30,40,0); // Tested pattern: SP?
    }
    @Test
    public void __oSearchTest() throws IOException, NotFoundException {
        searchTest(0,0,60); // Tested pattern: ??O
    }
    @Test
    public void s_oSearchTest() throws IOException, NotFoundException {
        searchTest(30,0,60); // Tested pattern: S?O
    }
    @Test
    public void _poSearchTest() throws IOException, NotFoundException {
        searchTest(0,40,60); // Tested pattern: ?PO
    }
    @Test
    public void spoSearchTest() throws IOException, NotFoundException {
        searchTest(30,40,60); // Tested pattern: SPO
    }
}
