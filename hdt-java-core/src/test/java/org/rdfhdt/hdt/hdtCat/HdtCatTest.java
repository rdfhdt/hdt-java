package org.rdfhdt.hdt.hdtCat;

import org.junit.Test;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.hdtCat.utils.Utility;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.util.io.AbstractMapMemoryTest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

public class HdtCatTest extends AbstractMapMemoryTest implements ProgressListener {

    private void help(String filename1, String filename2, String concatFilename) throws ParserException, IOException {

        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource(filename1);
        if (resource == null) {
            throw new FileNotFoundException(filename1);
        }
        String file1 = resource.getFile();
        URL resource1 = classLoader.getResource(filename2);
        if (resource1 == null) {
            throw new FileNotFoundException(filename2);
        }
        String file2 = resource1.getFile();
        URL resource2 = classLoader.getResource(concatFilename);
        if (resource2 == null) {
            throw new FileNotFoundException(concatFilename);
        }
        String concat = resource2.getFile();

        String hdt1Location = file1
            .replace(".nt", ".hdt")
            .replace(".nq", ".hdt");
        String hdt2Location = file2
            .replace(".nt", ".hdt")
            .replace(".nq", ".hdt");
        RDFNotation notation = RDFNotation.NTRIPLES;
        if (file1.endsWith(".nq")) {
            notation = RDFNotation.NQUAD;
        }
        try (HDT hdt = HDTManager.generateHDT(new File(file1).getAbsolutePath(), "uri", notation, new HDTSpecification(), this)) {
            hdt.saveToHDT(hdt1Location, null);
        }
        try (HDT hdt = HDTManager.generateHDT(new File(file2).getAbsolutePath(), "uri", notation, new HDTSpecification(), this)) {
            hdt.saveToHDT(hdt2Location, null);
        }
        try (HDT hdtCatOld = HDTManager.generateHDT(new File(concat).getAbsolutePath(), "uri", notation, new HDTSpecification(), this)) {

            File file = new File(file1);
            File theDir = new File(file.getAbsolutePath() + "_tmp");
            Files.createDirectories(theDir.toPath());
            try (HDT hdtCatNew = HDTManager.catHDT(theDir.getAbsolutePath(), hdt1Location, hdt2Location, new HDTSpecification(), null)) {

                //HDTCat hdtCatNew = new HDTCat(new HDTSpecification(),hdt1,hdt2,this);
                Utility.compareDictionary(hdtCatOld.getDictionary(), hdtCatNew.getDictionary());
                Utility.compareTriples(hdtCatOld, hdtCatNew);
            }
            Files.delete(theDir.toPath());
        }
    }

    @Test
    public void cat1() throws ParserException, IOException {
        help("example1.nt", "example2.nt", "example1+2.nt");
    }

    @Test
    public void cat2() throws ParserException, IOException {
        help("example2.nt", "example3.nt", "example2+3.nt");
    }

    @Test
    public void cat3() throws ParserException, IOException {
        help("example4.nt", "example5.nt", "example4+5.nt");
    }

    @Test
    public void cat4() throws ParserException, IOException {
        help("example6.nt", "example7.nt", "example6+7.nt");
    }

    @Test
    public void cat5() throws ParserException, IOException {
        help("example8.nt", "example9.nt", "example8+9.nt");
    }

    @Test
    public void cat6() throws ParserException, IOException {
        help("example10.nt", "example11.nt", "example10+11.nt");
    }

    @Test
    public void cat7() throws ParserException, IOException {
        help("example12.nt", "example13.nt", "example12+13.nt");
    }

    @Test
    public void cat9() throws ParserException, IOException {
        help("example14.nt", "example15.nt", "example14+15.nt");
    }

    @Test
    public void cat10() throws ParserException, IOException {
        help("example16.nt", "example17.nt", "example16+17.nt");
    }

    @Test
    public void cat11() throws ParserException, IOException {
        help("example1.nt", "example1.nt", "example1.nt");
    }

    @Test
    public void cat12() throws ParserException, IOException {
        help("example18.nt", "example19.nt", "example18+19.nt");
    }

    @Test
    public void cat13() throws ParserException, IOException {
        help("example20.nt", "example21.nt", "example20+21.nt");
    }

    @Test
    public void cat14() throws ParserException, IOException {
        help("example22.nt", "example23.nt", "example22+23.nt");
    }

    // QUADS
    @Test
    public void catQ1() throws ParserException, IOException {
        help("example1.nq", "example2.nq", "example1+2.nq");
    }

    @Test
    public void catQ2() throws ParserException, IOException {
        help("example2.nq", "example3.nq", "example2+3.nq");
    }

    @Test
    public void catQ3() throws ParserException, IOException {
        help("example4.nq", "example5.nq", "example4+5.nq");
    }

    @Test
    public void catQ4() throws ParserException, IOException {
        help("example6.nq", "example7.nq", "example6+7.nq");
    }

    @Test
    public void catQ5() throws ParserException, IOException {
        help("example8.nq", "example9.nq", "example8+9.nq");
    }

    @Test
    public void catQ6() throws ParserException, IOException {
        help("example10.nq", "example11.nq", "example10+11.nq");
    }

    @Test
    public void catQ7() throws ParserException, IOException {
        help("example12.nq", "example13.nq", "example12+13.nq");
    }

    @Test
    public void catQ9() throws ParserException, IOException {
        help("example14.nq", "example15.nq", "example14+15.nq");
    }

    @Test
    public void catQ10() throws ParserException, IOException {
        help("example16.nq", "example17.nq", "example16+17.nq");
    }

    @Test
    public void catQ11() throws ParserException, IOException {
        help("example1.nq", "example1.nq", "example1.nq");
    }

    @Test
    public void catQ12() throws ParserException, IOException {
        help("example18.nq", "example19.nq", "example18+19.nq");
    }

    @Test
    public void catQ13() throws ParserException, IOException {
        help("example20.nq", "example21.nq", "example20+21.nq");
    }

    @Test
    public void catQ14() throws ParserException, IOException {
        help("example22.nq", "example23.nq", "example22+23.nq");
    }

    //

    @Override
    public void notifyProgress(float level, String message) {

    }
}
