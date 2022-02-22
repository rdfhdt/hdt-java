package org.rdfhdt.hdt.triples.impl.utils;

import org.junit.rules.TemporaryFolder;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.rdf.RDFParserCallback;
import org.rdfhdt.hdt.rdf.RDFParserFactory;
import org.rdfhdt.hdt.triples.TripleString;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HDTSortedUtils {
    private final HDT hdt;
    private final List<TripleString> triples = new ArrayList<>();

    public HDTSortedUtils(TemporaryFolder temp, InputStream ntFile) throws IOException, ParserException {
        File f = new File(temp.newFolder(), "test.nt");

        Objects.requireNonNull(ntFile, "ntFile can't be null");

        Files.copy(ntFile, f.toPath());
        hdt = HDTManager.generateHDT(f.getAbsolutePath(), HDTTestUtils.BASE_URI, RDFNotation.NTRIPLES, new HDTSpecification(), null);
        RDFParserCallback parser = RDFParserFactory.getParserCallback(RDFNotation.NTRIPLES);

        triples.clear();
        try {
            hdt.search("", "", "").forEachRemaining(triples::add);
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
    }

    public long getIndex(TripleString str) {
        for (int i = 0; i < triples.size(); i++) {
            if (equalsTriple(str, triples.get(i)))
                return i;
        }
        throw new IllegalArgumentException("not a triple or our hdt: " + str);
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
        // quick fix, might do a pr/issue later to remove it
        // s1.equals(s1) -> false
        return equalsCharSequence(s1.getSubject(), s2.getSubject())
                && equalsCharSequence(s1.getPredicate(), s2.getPredicate())
                && equalsCharSequence(s1.getObject(), s2.getObject());

    }

    public HDT getHdt() {
        return hdt;
    }

    public List<TripleString> getTriples() {
        return triples;
    }
}
