package org.rdfhdt.hdt.hdtDiff;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.rdfhdt.hdt.dictionary.DictionaryFactory;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.hdt.writer.TripleWriterHDT;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.triples.impl.utils.HDTTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

@RunWith(Parameterized.class)
public class HdtDiffTest {
    /**
     * Return type for {@link #createTestHDT(File, File, File, HDTOptions, int, int, int, int, int)}
     */
    public static class HDTDiffData {
        public int tupleCountA;
        public int tupleCountB;
        public int tupleCountC;
        public int shared;
    }

    /**
     * create 2 test hdts and a result hdt
     *
     * @param fileName   hdt file name
     * @param fileName2  hdt 2 file name
     * @param fileName3  result hdt file name
     * @param subjects   number of subjects
     * @param predicates number of predicates
     * @param objects    number of objects
     * @param shared     number of shared subjects/objects
     * @return tuple count
     */
    public static HDTDiffData createTestHDT(File fileName, File fileName2, File fileName3, HDTOptions spec, int subjects, int predicates, int objects, int shared, int shift) throws IOException {
        String baseURI = "http://ex.org/";
        HDTDiffData count = new HDTDiffData();
        int shiftCount = 0;
        try (final TripleWriterHDT wr = new TripleWriterHDT(baseURI, spec, fileName.getAbsolutePath(), false);
             final TripleWriterHDT wr2 = new TripleWriterHDT(baseURI, spec, fileName2.getAbsolutePath(), false);
             final TripleWriterHDT wr3 = new TripleWriterHDT(baseURI, spec, fileName3.getAbsolutePath(), false)) {
            for (int i = subjects; i > 0; i--) {
                String subject;
                if (i <= shared) {
                    subject = baseURI + "Shared" + i;
                } else {
                    subject = baseURI + "Subject" + (i - shared);
                }

                for (int j = predicates; j > 0; j--) {
                    String predicate = baseURI + "Predicate" + j;
                    for (int k = objects; k > 0; k--) {
                        String object;
                        if (k <= shared) {
                            object = baseURI + "Shared" + k;
                        } else {
                            object = baseURI + "Object" + (k - shared);
                        }
                        shiftCount = (shiftCount + 1) % shift;
                        int newtriple = 0;

                        if (shiftCount != 0) {
                            wr.addTriple(new TripleString(
                                    subject,
                                    predicate,
                                    object
                            ));
                            count.tupleCountA++;
                            newtriple++;
                        }
                        if (shiftCount == 0 || shiftCount == 1) {
                            wr2.addTriple(new TripleString(
                                    subject,
                                    predicate,
                                    object
                            ));
                            count.tupleCountB++;
                            newtriple++;
                        } else {
                            if (newtriple == 1) {
                                wr3.addTriple(new TripleString(
                                        subject,
                                        predicate,
                                        object
                                ));
                                count.tupleCountC++;
                            }
                        }

                        if (newtriple == 2) {

                            count.shared++;
                        }
                    }
                }
            }
        }

        Assert.assertEquals("|A-B| != |C|", count.tupleCountA - count.shared, count.tupleCountC);

        return count;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> genParam() {
        return Arrays.asList(
                new Object[]{HDTVocabulary.DICTIONARY_TYPE_FOUR_SECTION, DictionaryFactory.MOD_DICT_IMPL_HASH},
                new Object[]{DictionaryFactory.DICTIONARY_TYPE_FOUR_SECTION_BIG, DictionaryFactory.MOD_DICT_IMPL_HASH},
                new Object[]{DictionaryFactory.DICTIONARY_TYPE_MULTI_OBJECTS, DictionaryFactory.MOD_DICT_IMPL_MULT_HASH},
                new Object[]{HDTVocabulary.DICTIONARY_TYPE_FOUR_PSFC_SECTION, DictionaryFactory.MOD_DICT_IMPL_HASH_PSFC}
        );
    }

    public static void assertHdtEquals(HDT hdt1, HDT hdt2) {
        Assert.assertEquals(
                "HDT Same triple count",
                hdt1.getTriples().getNumberOfElements(),
                hdt2.getTriples().getNumberOfElements()
        );

        IteratorTripleString its1;
        IteratorTripleString its2;

        try {
            its1 = hdt1.search("", "", "");
            its2 = hdt1.search("", "", "");
        } catch (NotFoundException e) {
            throw new AssertionError(e);
        }

        while (true) {
            if (!its1.hasNext()) {
                Assert.assertFalse("its1 is empty, but not its2", its2.hasNext());
                return;
            }
            Assert.assertTrue("its2 is empty, but not its1", its2.hasNext());

            TripleString ts1 = its1.next();
            TripleString ts2 = its2.next();
            Assert.assertEquals("ts1=ts2", ts1, ts2);
        }
    }

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();
    final HDTSpecification spec;

    public HdtDiffTest(String dictionaryType, String tempDictionaryImpl) {
        spec = new HDTSpecification();
        spec.set("dictionary.type", dictionaryType);
        spec.set("tempDictionary.impl", tempDictionaryImpl);
    }

    @Test
    public void hdtDiffTest() throws IOException {
        File hdtFile1 = tempDir.newFile();
        File hdtFile2 = tempDir.newFile();
        File hdtDiffExceptedFile = tempDir.newFile();
        createTestHDT(hdtFile1, hdtFile2, hdtDiffExceptedFile, spec,40, 50, 60, 5, 4);

        HDT hdtDiffExcepted = HDTManager.mapHDT(hdtDiffExceptedFile.getAbsolutePath(), null, spec);

        HDT hdtDiffActual = HDTManager.diffHDT(hdtFile1.getAbsolutePath(), hdtFile2.getAbsolutePath(), spec, null);

        Assert.assertEquals(
                "Dictionaries aren't the same",
                hdtDiffExcepted.getDictionary().getType(),
                hdtDiffActual.getDictionary().getType()
        );

        assertHdtEquals(hdtDiffExcepted, hdtDiffActual);
    }

    private void ntFilesDiffTest(String a, String b, String amb) throws IOException, ParserException {
        ClassLoader classLoader = getClass().getClassLoader();
        String file1 = Objects.requireNonNull(classLoader.getResource(a), "Can't find " + a).getFile();
        String file2 = Objects.requireNonNull(classLoader.getResource(b), "Can't find " + b).getFile();
        String diff = Objects.requireNonNull(classLoader.getResource(amb), "Can't find " + amb).getFile();

        String hdtFile1 = tempDir.newFile().getAbsolutePath();
        String hdtFile2 = tempDir.newFile().getAbsolutePath();

        HDT hdt1 = HDTManager.generateHDT(file1, HDTTestUtils.BASE_URI, RDFNotation.NTRIPLES, spec, null);
        hdt1.saveToHDT(hdtFile1, null);

        HDT hdt2 = HDTManager.generateHDT(file2, HDTTestUtils.BASE_URI, RDFNotation.NTRIPLES, spec, null);
        hdt2.saveToHDT(hdtFile2, null);

        HDT hdtDiffExcepted = HDTManager.generateHDT(diff, HDTTestUtils.BASE_URI, RDFNotation.NTRIPLES, spec, null);

        HDT hdtDiffActual = HDTManager.diffHDT(hdtFile1, hdtFile2, spec, null);


        Assert.assertEquals(
                "Dictionaries aren't the same",
                hdtDiffExcepted.getDictionary().getType(),
                hdtDiffActual.getDictionary().getType()
        );

        assertHdtEquals(hdtDiffExcepted, hdtDiffActual);
    }

    @Test
    public void ntFilesDiffTest1M2() throws IOException, ParserException {
        ntFilesDiffTest(
                "hdtDiff/example1.nt",
                "hdtDiff/example2.nt",
                "hdtDiff/example1-2.nt"
        );
    }

    @Test
    public void ntFilesDiffTest3M4() throws IOException, ParserException {
        ntFilesDiffTest(
                "hdtDiff/example3.nt",
                "hdtDiff/example4.nt",
                "hdtDiff/example3-4.nt"
        );
    }

    @Test
    public void ntFilesDiffTest5M6() throws IOException, ParserException {
        ntFilesDiffTest(
                "hdtDiff/example5.nt",
                "hdtDiff/example6.nt",
                "hdtDiff/example5-6.nt"
        );
    }

    @Test
    public void ntFilesDiffTest7M8() throws IOException, ParserException {
        ntFilesDiffTest(
                "hdtDiff/example7.nt",
                "hdtDiff/example8.nt",
                "hdtDiff/example7-8.nt"
        );
    }

    @Test
    public void ntFilesDiffTest9M10() throws IOException, ParserException {
        ntFilesDiffTest(
                "hdtDiff/example9.nt",
                "hdtDiff/example10.nt",
                "hdtDiff/example9-10.nt"
        );
    }

    @Test
    public void ntFilesDiffTest11M12() throws IOException, ParserException {
        ntFilesDiffTest(
                "hdtDiff/example11.nt",
                "hdtDiff/example12.nt",
                "hdtDiff/example11-12.nt"
        );
    }

    @Test
    public void ntFilesDiffTest13M14() throws IOException, ParserException {
        ntFilesDiffTest(
                "hdtDiff/example13.nt",
                "hdtDiff/example14.nt",
                "hdtDiff/example13-14.nt"
        );
    }

    @Test
    public void ntFilesDiffTest15M16() throws IOException, ParserException {
        ntFilesDiffTest(
                "hdtDiff/example15.nt",
                "hdtDiff/example16.nt",
                "hdtDiff/example15-16.nt"
        );
    }

    @Test
    public void ntFilesDiffTest17M18() throws IOException, ParserException {
        ntFilesDiffTest(
                "hdtDiff/example17.nt",
                "hdtDiff/example18.nt",
                "hdtDiff/example17-18.nt"
        );
    }

    @Test
    public void ntFilesDiffTest19M20() throws IOException, ParserException {
        ntFilesDiffTest(
                "hdtDiff/example19.nt",
                "hdtDiff/example20.nt",
                "hdtDiff/example19-20.nt"
        );
    }

    @Test
    public void ntFilesDiffTest21M22() throws IOException, ParserException {
        ntFilesDiffTest(
                "hdtDiff/example21.nt",
                "hdtDiff/example22.nt",
                "hdtDiff/example21-22.nt"
        );
    }

    @Test
    public void ntFilesDiffTest23M24() throws IOException, ParserException {
        ntFilesDiffTest(
                "hdtDiff/example23.nt",
                "hdtDiff/example24.nt",
                "hdtDiff/example23-24.nt"
        );
    }
}
