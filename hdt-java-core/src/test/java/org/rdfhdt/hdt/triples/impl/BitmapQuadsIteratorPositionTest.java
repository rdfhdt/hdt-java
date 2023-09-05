package org.rdfhdt.hdt.triples.impl;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.iterator.DictionaryTranslateIterator;
import org.rdfhdt.hdt.iterator.DictionaryTranslateIteratorBuffer;
import org.rdfhdt.hdt.iterator.SequentialSearchIteratorTripleID;
import org.rdfhdt.hdt.options.HDTOptionsKeys;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.triples.impl.utils.HDTTestUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.*;

@RunWith(Parameterized.class)
public class BitmapQuadsIteratorPositionTest {

    public static final List<String> DICTIONARIES = List.of(
        HDTOptionsKeys.DICTIONARY_TYPE_VALUE_FOUR_QUAD_SECTION
    );

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
    public static Collection<Object[]> genParam() {
        List<Object[]> lst = new ArrayList<>();
        for (String dict : DICTIONARIES) {
            for (int su = 0; su < 2; su++) {
                for (int pr = 0; pr < 2; pr++) {
                    for (int ob = 0; ob < 2; ob++) {
                        for (int sh = 0; sh < 2; sh++) {
                            for (int gr = 0; gr < 2; gr++) {
                                lst.add(new Object[] {
                                    dict,
                                    su * 25,
                                    pr * 50,
                                    ob * 37,
                                    sh * 12,
                                    gr * 6 + 1
                                });
                            }
                        }
                    }
                }
            }
        }
        return lst;
    }

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();
    final HDTSpecification spec;
    final int shared;
    final int subjects;
    final int predicates;
    final int objects;
    final int graphs;

    public BitmapQuadsIteratorPositionTest(
        String dictionaryType,
        int subjects,
        int predicates,
        int objects,
        int shared,
        int graphs
    ) {
        spec = new HDTSpecification();
        spec.set(
            HDTOptionsKeys.DICTIONARY_TYPE_KEY,
            dictionaryType
        );
        spec.set(
            HDTOptionsKeys.TEMP_DICTIONARY_IMPL_KEY,
            HDTOptionsKeys.TEMP_DICTIONARY_IMPL_VALUE_HASH_QUAD
        );
        spec.set(
            HDTOptionsKeys.DICTIONARY_TYPE_KEY,
            HDTOptionsKeys.DICTIONARY_TYPE_VALUE_FOUR_QUAD_SECTION
        );
        this.subjects = subjects;
        this.predicates = predicates;
        this.objects = objects;
        this.shared = shared;
        this.graphs = graphs;
    }

    @Test
    public void searchAllTest() throws IOException, NotFoundException {
        HDTTestUtils data = new HDTTestUtils(
            tempDir.newFile(),
            subjects,
            predicates,
            objects,
            shared,
            graphs,
            spec,
            false
        );

        // TODO: fix this
        IteratorTripleString it = null; // data.searchForSPO(0, 0, 0);
        if (it == null) return;

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
        HDTTestUtils data = new HDTTestUtils(
            tempDir.newFile(),
            subjects,
            predicates,
            objects,
            shared,
            graphs,
            spec,
            true
        );

        // TODO: fix this
        IteratorTripleString it = null; // data.searchForSPO(0, 0, 0);
        if (it == null) return;

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
     *
     * @param s subject to search (or 0 for wildcard)
     * @param p predicate to search (or 0 for wildcard)
     * @param o object to search (or 0 for wildcard)
     * @param g graph to search (or 0 for wildcard)

     * @throws IOException       file
     * @throws NotFoundException search
     */
    private void searchTest(int s, int p, int o, int g) throws IOException, NotFoundException {
        HDTTestUtils data = new HDTTestUtils(
            tempDir.newFile(),
            subjects,
            predicates,
            objects,
            shared,
            graphs,
            spec,
            true
        );

        IteratorTripleString it = data.searchForSPO(s, p, o, g);
        if (it == null) return;

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
        for (int s = 0; s < 2; s++) {
            for (int p = 0; p < 2; p++) {
                for (int o = 0; o < 2; o++) {
                    for (int g = 0; g < 2; g++) {
                        searchTest(
                            s * subjects / 3, 
                            p * predicates / 3,
                            o * objects / 3,
                            g * graphs / 3
                        );
                    }
                }
            }
        }
    }

    private boolean equalsCharSequence(CharSequence cs1, CharSequence cs2) {
        if (cs1.length() != cs2.length())
            return false;

        for (int i = 0; i < cs1.length(); i++)
            if (cs1.charAt(i) != cs2.charAt(i))
                return false;
        return true;
    }

    private boolean equalsTriple(TripleString s1, TripleString s2) {
        return
            equalsCharSequence(s1.getSubject(), s2.getSubject())
         && equalsCharSequence(s1.getPredicate(), s2.getPredicate())
         && equalsCharSequence(s1.getObject(), s2.getObject())
         && equalsCharSequence(s1.getGraph(), s2.getGraph());

    }

    public long getIndex(List<TripleString> triples, TripleString str) {
        for (int i = 0; i < triples.size(); i++) {
            if (equalsTriple(str, triples.get(i)))
                return i;
        }
        throw new IllegalArgumentException("not a triple or our hdt: " + str);
    }

    private void searchTPSTest(
        int s,
        int p,
        int o,
        int g
    ) throws NotFoundException, ParserException, IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File f = new File(tempDir.newFolder(), "test.nt");
        InputStream ntFile = classLoader.getResourceAsStream("example_triplePosition.nt");
        Assert.assertNotNull("ntFile can't be null", ntFile);

        Files.copy(ntFile, f.toPath());
        try (HDT hdt = HDTManager.generateHDT(f.getAbsolutePath(), HDTTestUtils.BASE_URI, RDFNotation.NTRIPLES, new HDTSpecification(), null)) {
            List<TripleString> triples = new ArrayList<>();
            hdt.search("", "", "", "").forEachRemaining(triples::add);
            TripleString ts = triples.get(10);
            CharSequence ss = s == 0 ? "" : ts.getSubject();
            CharSequence sp = p == 0 ? "" : ts.getPredicate();
            CharSequence so = o == 0 ? "" : ts.getObject();
            CharSequence sg = g == 0 ? "" : ts.getGraph();

            IteratorTripleString it = hdt.search(ss, sp, so, sg);

            while (it.hasNext()) {
                TripleString tripleString = it.next();
                Assert.assertEquals("Sorted triple index", getIndex(triples, tripleString), it.getLastTriplePosition());
            }
        }
    }

    @Test
    public void ___SearchTPSTest() throws IOException, NotFoundException, ParserException {
        for (int s = 0; s < 2; s++) {
            for (int p = 0; p < 2; p++) {
                for (int o = 0; o < 2; o++) {
                    for (int g = 0; g < 2; g++) {
                        searchTPSTest(
                            s, 
                            p,
                            o,
                            g
                        );
                    }
                }
            }
        }
    }
}
