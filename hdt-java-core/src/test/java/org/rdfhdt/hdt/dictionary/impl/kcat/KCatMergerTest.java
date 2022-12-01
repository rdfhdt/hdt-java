package org.rdfhdt.hdt.dictionary.impl.kcat;

import org.apache.commons.io.file.PathUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.dictionary.DictionaryPrivate;
import org.rdfhdt.hdt.dictionary.DictionarySection;
import org.rdfhdt.hdt.dictionary.DictionarySectionPrivate;
import org.rdfhdt.hdt.dictionary.impl.section.PFCDictionarySection;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.hdt.HDTManagerTest;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTOptionsBase;
import org.rdfhdt.hdt.options.HDTOptionsKeys;
import org.rdfhdt.hdt.util.LargeFakeDataSetStreamSupplier;
import org.rdfhdt.hdt.util.concurrent.SyncSeq;
import org.rdfhdt.hdt.util.io.AbstractMapMemoryTest;
import org.rdfhdt.hdt.util.io.Closer;
import org.rdfhdt.hdt.util.string.ByteString;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class KCatMergerTest extends AbstractMapMemoryTest {
    @Parameterized.Parameters(name = "multi: {0}, unicode: {1}, map: {2}, count: {3}")
    public static Collection<Object[]> params() {
        return Stream.of(false, true).flatMap(
                multi -> Stream.of(false, true).flatMap(
                        unicode -> Stream.of(false, true).flatMap(
                                map -> Stream.of(2, 10).map(
                                        kcat -> new Object[]{multi, unicode, map, kcat}
                                )
                        )
                )
        ).collect(Collectors.toList());
    }

    @Parameterized.Parameter
    public boolean multi;
    @Parameterized.Parameter(1)
    public boolean unicode;
    @Parameterized.Parameter(2)
    public boolean map;
    @Parameterized.Parameter(3)
    public int kcat;

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private void writeSection(DictionarySection sec, OutputStream stream) throws IOException {
        ((DictionarySectionPrivate) sec).save(stream, null);
    }

    private DictionarySection loadSection(InputStream stream) throws IOException {
        PFCDictionarySection section = new PFCDictionarySection(new HDTOptionsBase());
        section.load(stream, null);
        return section;
    }

    private Map<? extends CharSequence, DictionarySection> loadMultiSection(List<CharSequence> seq, InputStream stream) throws IOException {
        Map<ByteString, DictionarySection> sectionMap = new TreeMap<>();
        for (CharSequence key : seq) {
            PFCDictionarySection section = new PFCDictionarySection(new HDTOptionsBase());
            section.load(stream, null);
            sectionMap.put(ByteString.of(key), section);
        }
        return sectionMap;
    }

    @Test
    public void mergerTest() throws ParserException, IOException, InterruptedException {
        Path root = tempDir.getRoot().toPath();
        try {
            HDTOptions spec = new HDTOptionsBase();

            if (multi) {
                spec.set(HDTOptionsKeys.DICTIONARY_TYPE_KEY, HDTOptionsKeys.DICTIONARY_TYPE_VALUE_MULTI_OBJECTS);
                spec.set(HDTOptionsKeys.TEMP_DICTIONARY_IMPL_KEY, HDTOptionsKeys.TEMP_DICTIONARY_IMPL_VALUE_MULT_HASH);
            }

            // create "kcat" fake HDTs
            LargeFakeDataSetStreamSupplier s = LargeFakeDataSetStreamSupplier
                    .createSupplierWithMaxTriples(1000, 42)
                    .withMaxElementSplit(50)
                    .withUnicode(unicode);

            List<String> hdts = new ArrayList<>();
            for (int i = 0; i < kcat; i++) {
                String location = root.resolve("hdt" + i + ".hdt").toAbsolutePath().toString();
                hdts.add(location);
                s.createAndSaveFakeHDT(spec, location);
            }

            // create the excepted HDT from previous algorithm
            Path fatcathdt = root.resolve("fatcat.hdt");
            LargeFakeDataSetStreamSupplier
                    .createSupplierWithMaxTriples(1000L * kcat, 42)
                    .withMaxElementSplit(50)
                    .withUnicode(unicode)
                    .createAndSaveFakeHDT(spec, fatcathdt.toAbsolutePath().toString());

            // create dictionary and write sections
            Path dictFile = root.resolve("dict");
            List<CharSequence> sub = new ArrayList<>();
            try (KCatImpl impl = new KCatImpl(hdts, spec, null)) {
                try (KCatMerger merger = impl.createMerger(null)) {
                    assertEquals(multi, merger.typedHDT);
                    merger.startMerger();
                    // create
                    DictionaryPrivate dict = merger.buildDictionary();
                    try (OutputStream stream = new BufferedOutputStream(Files.newOutputStream(dictFile))) {
                        writeSection(dict.getShared(), stream);
                        writeSection(dict.getSubjects(), stream);
                        writeSection(dict.getPredicates(), stream);
                        if (multi) {
                            for (Map.Entry<? extends CharSequence, DictionarySection> e : dict.getAllObjects().entrySet()) {
                                CharSequence key = e.getKey();
                                sub.add(key);
                                DictionarySection sec = e.getValue();
                                writeSection(sec, stream);
                            }
                        } else {
                            writeSection(dict.getObjects(), stream);
                        }
                    }

                    // check if all the dynamic sequences are filled

                    SyncSeq[] sms = merger.subjectsMaps;
                    SyncSeq[] pms = merger.predicatesMaps;
                    SyncSeq[] oms = merger.objectsMaps;

                    AtomicLong[] objectCounts = merger.countObject;
                    AtomicLong[] subjectCounts = merger.countSubject;

                    for (int hdtId = 1; hdtId <= impl.hdts.length; hdtId++) {
                        HDT hdt = impl.hdts[hdtId - 1];
                        SyncSeq sm = sms[hdtId - 1];
                        SyncSeq pm = pms[hdtId - 1];
                        SyncSeq om = oms[hdtId - 1];

                        AtomicLong objectCount = objectCounts[hdtId - 1];
                        AtomicLong subjectCount = subjectCounts[hdtId - 1];

                        long shared = hdt.getDictionary().getShared().getNumberOfElements();
                        long subjects = hdt.getDictionary().getSubjects().getNumberOfElements();
                        long predicates = hdt.getDictionary().getPredicates().getNumberOfElements();
                        long objects =
                                multi ? hdt.getDictionary().getAllObjects().values().stream().mapToLong(DictionarySection::getNumberOfElements).sum()
                                        : hdt.getDictionary().getObjects().getNumberOfElements();

                        assertEquals(shared + objects, objectCount.get());
                        assertEquals(shared + subjects, subjectCount.get());

                        for (long i = 1; i <= shared; i++) {
                            long sv = sm.get(i);
                            long ov = om.get(i);
                            if (merger.removeHeader(sv) == 0) {
                                fail("HDT #" + hdtId + "/" + impl.hdts.length + " Missing shared subject #" + i + "/" + shared + " for node: " + hdt.getDictionary().idToString(i, TripleComponentRole.SUBJECT));
                            }
                            if (merger.removeHeader(ov) == 0) {
                                fail("HDT #" + hdtId + "/" + impl.hdts.length + " Missing shared object #" + i + "/" + shared + " for node: " + hdt.getDictionary().idToString(i, TripleComponentRole.OBJECT));
                            }

                            assertEquals("shared element not mapped to the same object", ov, sv);
                            assertTrue("shared mapped element isn't shared", merger.isShared(ov));
                        }

                        for (long i = 1; i <= subjects; i++) {
                            if (merger.removeHeader(sm.get(shared + i)) == 0) {
                                fail("HDT #" + hdtId + "/" + impl.hdts.length + " Missing subject #" + i + "/" + subjects + " for node: " + hdt.getDictionary().idToString(i + shared, TripleComponentRole.SUBJECT));
                            }
                        }

                        for (long i = 1; i <= objects; i++) {
                            if (merger.removeHeader(om.get(shared + i)) == 0) {
                                fail("HDT #" + hdtId + "/" + impl.hdts.length + " Missing object #" + i + "/" + subjects + " for node: " + hdt.getDictionary().idToString(i + shared, TripleComponentRole.OBJECT));
                            }
                        }

                        for (long i = 1; i <= predicates; i++) {
                            if (pm.get(i) == 0) {
                                fail("HDT #" + hdtId + "/" + impl.hdts.length + " Missing predicate #" + i + "/" + subjects + " for node: " + hdt.getDictionary().idToString(i, TripleComponentRole.PREDICATE));
                            }
                        }

                    }
                }
            }
            try (InputStream stream = new BufferedInputStream(Files.newInputStream(dictFile))) {
                // read the sections
                try (DictionarySection sh = loadSection(stream);
                     DictionarySection su = loadSection(stream);
                     DictionarySection pr = loadSection(stream)) {
                    Map<? extends CharSequence, DictionarySection> dictionarySectionMap;
                    DictionarySection ob;
                    if (multi) {
                        ob = null;
                        dictionarySectionMap = loadMultiSection(sub, stream);
                    } else {
                        dictionarySectionMap = Map.of();
                        ob = loadSection(stream);
                    }
                    try {
                        // map the excepted hdt
                        try (HDT exceptedHDT = HDTManager.mapHDT(fatcathdt.toAbsolutePath().toString())) {
                            Dictionary exceptedDict = exceptedHDT.getDictionary();
                            assertNotEquals("Invalid test, shared section empty", 0, exceptedHDT.getDictionary().getShared().getNumberOfElements());
                            // assert equals between the dictionaries
                            HDTManagerTest.HDTManagerTestBase.assertEqualsHDT("Shared", exceptedDict.getShared(), sh);
                            HDTManagerTest.HDTManagerTestBase.assertEqualsHDT("Subjects", exceptedDict.getSubjects(), su);
                            HDTManagerTest.HDTManagerTestBase.assertEqualsHDT("Predicates", exceptedDict.getPredicates(), pr);
                            if (multi) {
                                Map<? extends CharSequence, DictionarySection> exceptedDictSub = exceptedDict.getAllObjects();
                                dictionarySectionMap.forEach((key, sec) -> {
                                    DictionarySection subSec = exceptedDictSub.get(key);
                                    assertNotNull("sub#" + key + " wasn't found", subSec);
                                    HDTManagerTest.HDTManagerTestBase.assertEqualsHDT("Section#" + key, subSec, sec);
                                });
                            } else {
                                assert ob != null;
                                HDTManagerTest.HDTManagerTestBase.assertEqualsHDT("Objects", exceptedDict.getObjects(), ob);
                            }
                        }
                    } finally {
                        Closer
                                .of(ob)
                                .with(dictionarySectionMap.values())
                                .close();
                    }
                }
            }
        } finally {
            PathUtils.deleteDirectory(root);
        }
    }

    @Test
    public void catTest() throws ParserException, IOException, NotFoundException {
        Path root = tempDir.getRoot().toPath();
        try {
            // number of HDTs
            int countPerHDT = 1000;
            Random rnd = new Random(58);

            // create the config
            HDTOptions spec = new HDTOptionsBase();
            if (multi) {
                spec.set(HDTOptionsKeys.DICTIONARY_TYPE_KEY, HDTOptionsKeys.DICTIONARY_TYPE_VALUE_MULTI_OBJECTS);
                spec.set(HDTOptionsKeys.TEMP_DICTIONARY_IMPL_KEY, HDTOptionsKeys.TEMP_DICTIONARY_IMPL_VALUE_MULT_HASH);
            }

            if (map) {
                spec.set(HDTOptionsKeys.HDTCAT_FUTURE_LOCATION, root.resolve("futurehc.hdt").toAbsolutePath());
            }

            // create "kcat" fake HDTs
            LargeFakeDataSetStreamSupplier s = LargeFakeDataSetStreamSupplier
                    .createInfinite(42)
                    .withMaxElementSplit(50)
                    .withUnicode(unicode);

            long size = 0;
            List<String> hdts = new ArrayList<>();
            for (int i = 0; i < kcat; i++) {
                String location = root.resolve("hdt" + i + ".hdt").toAbsolutePath().toString();
                hdts.add(location);
                int hdtSize = countPerHDT / 2 + rnd.nextInt(countPerHDT);
                size += hdtSize;
                s.withMaxTriples(hdtSize)
                        .createAndSaveFakeHDT(spec, location);
            }

            // create the excepted HDT from previous algorithm
            Path fatcathdt = root.resolve("fatcat.hdt");
            s.reset();
            s.withMaxTriples(size)
                    .createAndSaveFakeHDT(spec, fatcathdt.toAbsolutePath().toString());

            // create dictionary and write sections
            // map the excepted hdt
            try (HDT actualHDT = HDTManager.catHDT(hdts, spec, null)) {
                try (HDT exceptedHDT = HDTManager.mapHDT(fatcathdt.toAbsolutePath().toString())) {
                    // assert equals between the dictionaries
                    assertNotEquals(0, actualHDT.getDictionary().getShared().getNumberOfElements());
                    HDTManagerTest.HDTManagerTestBase.assertEqualsHDT(exceptedHDT, actualHDT);
                }
            }
        } finally {
            PathUtils.deleteDirectory(root);
        }
    }

}
