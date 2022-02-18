package org.rdfhdt.hdt.triples.impl;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.rdfhdt.hdt.dictionary.DictionaryFactory;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.iterator.DictionaryTranslateIterator;
import org.rdfhdt.hdt.iterator.DictionaryTranslateIteratorBuffer;
import org.rdfhdt.hdt.iterator.SequentialSearchIteratorTripleID;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.triples.impl.utils.HDTTestUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class BitmapTriplesIteratorPositionTest {

    private static final Field ITERATOR_SUB;
    private static final Field ITERATOR_SUB_BUFFER;
    private static final Field ITERATOR_SUB_SEQ;

    static {
        try {
            ITERATOR_SUB = DictionaryTranslateIterator.class.getDeclaredField("iterator");
            ITERATOR_SUB_BUFFER = DictionaryTranslateIteratorBuffer.class.getDeclaredField("iterator");
            ITERATOR_SUB_SEQ = SequentialSearchIteratorTripleID.class.getDeclaredField("iterator");

            ITERATOR_SUB.setAccessible(true);
            ITERATOR_SUB_BUFFER.setAccessible(true);
            ITERATOR_SUB_SEQ.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new Error(e);
        }
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object> genParam() {
        return Arrays.asList(
                HDTVocabulary.DICTIONARY_TYPE_FOUR_SECTION,
                DictionaryFactory.DICTIONARY_TYPE_FOUR_SECTION_BIG,
                DictionaryFactory.DICTIONARY_TYPE_MULTI_OBJECTS,
                HDTVocabulary.DICTIONARY_TYPE_FOUR_PSFC_SECTION
        );
    }

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();
    final HDTSpecification spec;
    final int shared;
    final int subjects;
    final int predicates;
    final int objects;

    public BitmapTriplesIteratorPositionTest(String dictionaryType) {
        spec = new HDTSpecification();
        spec.set("dictionary.type", dictionaryType);
        subjects = 25;
        predicates = 50;
        objects = 37;
        shared = 12;
    }

    /**
     * Print an iterator and (if found) sub iterators
     * @param it Iterator
     */
    private void printIterator(Object it) {
        for (int depth = 0; ; depth++) {
            System.out.println("[" + depth + "] Used iterator: " + it.getClass());
            try {
                if (it instanceof DictionaryTranslateIterator) {
                    it = ITERATOR_SUB.get(it);
                } else if (it instanceof DictionaryTranslateIteratorBuffer) {
                    it = ITERATOR_SUB_BUFFER.get(it);
                } else if (it instanceof SequentialSearchIteratorTripleID) {
                    it = ITERATOR_SUB_SEQ.get(it);
                } else
                    break;
            } catch (IllegalAccessException e) {
                break;
            }
        }
    }

    @Test
    public void searchAllTest() throws IOException, NotFoundException {
        HDTTestUtils data = new HDTTestUtils(tempDir.newFile(), subjects, predicates, objects, shared, spec, false);

        IteratorTripleString it = data.searchForSPO(0, 0, 0);

        printIterator(it);

        long index = 0L;
        while (it.hasNext()) {
            TripleString triple = it.next();
            long tripleIndex = it.getLastTriplePosition();

            // test if the search is returning index from 0, 1, ...count
            Assert.assertEquals("nextTriplePosition order", index++, tripleIndex);

            // test if the triple is at the right index
            HDTTestUtils.SpoId spoId = data.tripleToSpo(triple);
            long testIndex = spoId.getIndex();
            Assert.assertEquals("getIndex hdt value", testIndex, tripleIndex);

            TripleID findTriple = data.hdt.getTriples().findTriple(tripleIndex);
            long testIndex2 = data.tripleToSpo(findTriple).getIndex();
            Assert.assertEquals("getIndex findTriple hdt value", testIndex2, tripleIndex);
        }
    }

    @Test
    public void searchAllTestBuffer() throws IOException, NotFoundException {
        HDTTestUtils data = new HDTTestUtils(tempDir.newFile(), subjects, predicates, objects, shared, spec, true);

        IteratorTripleString it = data.searchForSPO(0, 0, 0);

        printIterator(it);

        long index = 0L;
        while (it.hasNext()) {
            TripleString triple = it.next();
            long tripleIndex = it.getLastTriplePosition();

            // test if the search is returning index from 0, 1, ...count
            Assert.assertEquals("nextTriplePosition order", index++, tripleIndex);

            // test if the triple is at the right index
            HDTTestUtils.SpoId spoId = data.tripleToSpo(triple);
            long testIndex = spoId.getIndex();
            Assert.assertEquals("getIndex hdt value", testIndex, tripleIndex);

            // test if we can find it back again
            TripleID findTriple = data.hdt.getTriples().findTriple(tripleIndex);
            long testIndex2 = data.tripleToSpo(findTriple).getIndex();
            Assert.assertEquals("getIndex findTriple hdt value", testIndex2, tripleIndex);
        }
    }

    /**
     * create a test for a particular spo pattern
     * @param s subject to search (or 0 for wildcard)
     * @param p predicate to search (or 0 for wildcard)
     * @param o object to search (or 0 for wildcard)
     * @throws IOException file
     * @throws NotFoundException search
     */
    private void searchTest(int s, int p, int o) throws IOException, NotFoundException {
        HDTTestUtils data = new HDTTestUtils(tempDir.newFile(), subjects, predicates, objects, shared, spec, true);

        IteratorTripleString it = data.searchForSPO(s, p, o);

        printIterator(it);

        while (it.hasNext()) {
            TripleString triple = it.next();
            long tripleIndex = it.getLastTriplePosition();
            // test if the triple is at the right index
            HDTTestUtils.SpoId spoId = data.tripleToSpo(triple);
            long testIndex = spoId.getIndex();
            Assert.assertEquals("getIndex hdt value", testIndex, tripleIndex);
        }
    }

    @Test
    public void ___SearchTest() throws IOException, NotFoundException {
        searchTest(0, 0, 0); // Tested pattern: ???
    }

    @Test
    public void s__SearchTest() throws IOException, NotFoundException {
        searchTest(subjects / 3, 0, 0); // Tested pattern: S??
    }

    @Test
    public void _p_SearchTest() throws IOException, NotFoundException {
        searchTest(0, predicates / 3, 0); // Tested pattern: ?P?
    }

    @Test
    public void sp_SearchTest() throws IOException, NotFoundException {
        searchTest(subjects / 3, predicates / 3, 0); // Tested pattern: SP?
    }

    @Test
    public void __oSearchTest() throws IOException, NotFoundException {
        searchTest(0, 0, objects / 3); // Tested pattern: ??O
    }

    @Test
    public void s_oSearchTest() throws IOException, NotFoundException {
        searchTest(subjects / 3, 0, objects / 3); // Tested pattern: S?O
    }

    @Test
    public void _poSearchTest() throws IOException, NotFoundException {
        searchTest(0, predicates / 3, objects / 3); // Tested pattern: ?PO
    }

    @Test
    public void spoSearchTest() throws IOException, NotFoundException {
        searchTest(subjects / 3, predicates / 3, objects / 3); // Tested pattern: SPO
    }

}
