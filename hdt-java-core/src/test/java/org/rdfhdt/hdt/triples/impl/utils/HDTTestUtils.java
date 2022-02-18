package org.rdfhdt.hdt.triples.impl.utils;

import org.junit.Assert;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.hdt.writer.TripleWriterHDT;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleString;

import java.io.File;
import java.io.IOException;

public class HDTTestUtils {

    /**
     * base URI
     */
    public static final String BASE_URI = "http://ex.org/";

    public class SpoId {
        public final int s, p, o;

        public SpoId(int s, int p, int o) {
            this.s = s;
            this.p = p;
            this.o = o;
        }

        /**
         * Compute the index in the HDT assuming the order
         * @return index
         */
        public long getIndex() {
            long indexS = s == 0 ? 0 : (s - 1);
            long indexP = p == 0 ? 0 : (p - 1);
            long indexO = o == 0 ? 0 : (o - 1);
            return indexO + objects * (indexP + predicates * indexS);
        }
    }

    /**
     * memory hdt
     */
    public final HDT hdt;
    /**
     * the hdt file
     */
    public final File hdtFile;
    /**
     * triples count
     */
    public final int triples;
    /**
     * subject/object shared iris count
     */
    public final int shared;
    /**
     * subjects count
     */
    public final int subjects;
    /**
     * predicates count
     */
    public final int predicates;
    /**
     * objects count
     */
    public final int objects;

    /**
     * create a test hdt with information
     *
     * @param subjects   number of subjects
     * @param predicates number of predicates
     * @param objects    number of objects
     * @param shared     number of shared subjects/objects
     * @param spec       hdt spec
     */
    public HDTTestUtils(File f, int subjects, int predicates, int objects, int shared, HDTOptions spec, boolean buffer) throws IOException {
        this.hdtFile = f;
        this.shared = shared;
        this.subjects = subjects;
        this.predicates = predicates;
        this.objects = objects;

        int triples = 0;
        try (final TripleWriterHDT wr = new TripleWriterHDT(BASE_URI, new HDTSpecification(), hdtFile.getAbsolutePath(), false)) {
            for (int i = subjects; i > 0; i--) {
                for (int j = predicates; j > 0; j--) {
                    for (int k = objects; k > 0; k--) {
                        wr.addTriple(spoToTriple(i, j, k));
                        triples++;
                    }
                }
            }
        }
        if (buffer) this.hdt = HDTManager.mapHDT(hdtFile.getAbsolutePath(), null, spec);
        else this.hdt = HDTManager.loadHDT(hdtFile.getAbsolutePath(), null, spec);
        Assert.assertEquals("HDT count", triples, hdt.getTriples().getNumberOfElements());
        this.triples = triples;
    }

    /**
     * convert SpoId into {@link TripleString}
     *
     * @param id spoid
     * @return triplestring
     */
    public TripleString spoToTriple(SpoId id) {
        return spoToTriple(id.s, id.p, id.o);
    }

    /**
     * convert SpoId into {@link TripleString}
     *
     * @param s subject
     * @param p predicate
     * @param o object
     * @return triplestring
     */
    public TripleString spoToTriple(int s, int p, int o) {
        String ss, sp, so;
        if (s == 0) {
            ss = "";
        } else if (s <= shared) {
            ss = BASE_URI + "Sh" + String.format("%05d", s);
        } else {
            ss = BASE_URI + "Su" + String.format("%05d", s - shared);
        }
        if (p == 0) {
            sp = "";
        } else {
            sp = BASE_URI + "Pr" + String.format("%05d", p);
        }
        if (o == 0) {
            so = "";
        } else if (o <= shared) {
            so = BASE_URI + "Sh" + String.format("%05d", o);
        } else {
            so = BASE_URI + "Ob" + String.format("%05d", o - shared);
        }
        return new TripleString(ss, sp, so);
    }

    /**
     * convert a {@link TripleID} to a {@link SpoId}
     *
     * @param triple hdt triple
     * @return spoid
     */
    public SpoId tripleToSpo(TripleID triple) {
        return tripleToSpo(new TripleString(
                hdt.getDictionary().idToString(triple.getSubject(), TripleComponentRole.SUBJECT),
                hdt.getDictionary().idToString(triple.getPredicate(), TripleComponentRole.PREDICATE),
                hdt.getDictionary().idToString(triple.getObject(), TripleComponentRole.OBJECT)
        ));
    }
    /**
     * convert a {@link TripleString} to a {@link SpoId}
     *
     * @param triple hdt triple
     * @return spoid
     */
    public SpoId tripleToSpo(TripleString triple) {

        int shift = BASE_URI.length();

        String s = triple.getSubject().toString();
        String p = triple.getPredicate().toString();
        String o = triple.getObject().toString();

        int sid = s.isEmpty() ? 0 : Integer.parseInt(s.substring(shift + 2));
        int pid = p.isEmpty() ? 0 : Integer.parseInt(p.substring(shift + 2));
        int oid = o.isEmpty() ? 0 : Integer.parseInt(o.substring(shift + 2));

        if (!s.startsWith("Sh", shift)) sid += shared;

        if (!o.startsWith("Sh", shift)) oid += shared;

        return new SpoId(sid, pid, oid);
    }

    /**
     * search in the hdt triples, 0 for wildcard
     *
     * @param s subject
     * @param p predicate
     * @param o object
     * @return the iterator
     * @throws NotFoundException if no triples can be found
     */
    public IteratorTripleString searchForSPO(int s, int p, int o) throws NotFoundException {
        TripleString tr = spoToTriple(s, p, o);

        System.out.println("Search with pattern:" + (s == 0 ? "?" : "S") + (p == 0 ? "?" : "P") + (o == 0 ? "?" : "O"));
        return hdt.search(tr.getSubject(), tr.getPredicate(), tr.getObject());
    }

}
