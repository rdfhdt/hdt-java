package org.rdfhdt.hdt.hdtCat;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.hdtCat.utils.Utility;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTSpecification;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class HdtCatTest implements ProgressListener {

    public void help(String file1, String file2, String concat){
        if (SystemUtils.IS_OS_UNIX) {
            try {
                ClassLoader classLoader = getClass().getClassLoader();
                HDT hdt1 = null;
                HDT hdt2 = null;
                String hdt1_location = file1.replace(".nt", ".hdt");
                String hdt2_location = file2.replace(".nt", ".hdt");
                hdt1 = HDTManager.generateHDT(new File(file1).getAbsolutePath(), "uri", RDFNotation.NTRIPLES, new HDTSpecification(), this);
                hdt1.saveToHDT(hdt1_location, null);
                hdt2 = HDTManager.generateHDT(new File(file2).getAbsolutePath(), "uri", RDFNotation.NTRIPLES, new HDTSpecification(), this);
                hdt2.saveToHDT(hdt2_location, null);
                HDT hdtCatOld = HDTManager.generateHDT(new File(concat).getAbsolutePath(), "uri", RDFNotation.NTRIPLES, new HDTSpecification(), this);

                File file = new File(file1);
                File theDir = new File(file.getAbsolutePath() + "_tmp");
                theDir.mkdirs();
                HDT hdtCatNew = HDTManager.catHDT(theDir.getAbsolutePath(), hdt1_location, hdt2_location, new HDTSpecification(), null);

                //HDTCat hdtCatNew = new HDTCat(new HDTSpecification(),hdt1,hdt2,this);
                Utility.printDictionary(hdtCatNew.getDictionary());
                Utility.compareDictionary(hdtCatOld.getDictionary(), hdtCatNew.getDictionary());
                Utility.printTriples(hdtCatNew);
                Utility.compareTriples(hdtCatOld,hdtCatNew);

                try {
                    Iterator it = hdtCatOld.search("", "", "");
                    while (it.hasNext()) {
                        System.out.println(it.next().toString());
                    }

                } catch (NotFoundException e) {
                    e.printStackTrace();
                }

                try {
                    Iterator it = hdtCatNew.search("", "", "");
                    while (it.hasNext()) {
                        System.out.println(it.next().toString());
                    }

                } catch (NotFoundException e) {
                    e.printStackTrace();
                }
                theDir.delete();
            } catch (ParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void cat1() {
        ClassLoader classLoader = getClass().getClassLoader();
        String file1 = classLoader.getResource("example1.nt").getFile();
        String file2 = classLoader.getResource("example2.nt").getFile();
        String concat = classLoader.getResource("example1+2.nt").getFile();
        help(file1,file2,concat);
    }

    @Test
    public void cat2() {
        ClassLoader classLoader = getClass().getClassLoader();
        String file1 = classLoader.getResource("example2.nt").getFile();
        String file2 = classLoader.getResource("example3.nt").getFile();
        String concat = classLoader.getResource("example2+3.nt").getFile();
        help(file1,file2,concat);
    }

    @Test
    public void cat3() {
        ClassLoader classLoader = getClass().getClassLoader();
        String file1 = classLoader.getResource("example4.nt").getFile();
        String file2 = classLoader.getResource("example5.nt").getFile();
        String concat = classLoader.getResource("example4+5.nt").getFile();
        help(file1,file2,concat);
    }

    @Test
    public void cat4() {
        ClassLoader classLoader = getClass().getClassLoader();
        String file1 = classLoader.getResource("example6.nt").getFile();
        String file2 = classLoader.getResource("example7.nt").getFile();
        String concat = classLoader.getResource("example6+7.nt").getFile();
        help(file1,file2,concat);
    }

    @Test
    public void cat5() {
        ClassLoader classLoader = getClass().getClassLoader();
        String file1 = classLoader.getResource("example8.nt").getFile();
        String file2 = classLoader.getResource("example9.nt").getFile();
        String concat = classLoader.getResource("example8+9.nt").getFile();
        help(file1,file2,concat);
    }

    @Test
    public void cat6() {
        ClassLoader classLoader = getClass().getClassLoader();
        String file1 = classLoader.getResource("example10.nt").getFile();
        String file2 = classLoader.getResource("example11.nt").getFile();
        String concat = classLoader.getResource("example10+11.nt").getFile();
        help(file1,file2,concat);
    }

    @Test
    public void cat7() {
        ClassLoader classLoader = getClass().getClassLoader();
        String file1 = classLoader.getResource("example12.nt").getFile();
        String file2 = classLoader.getResource("example13.nt").getFile();
        String concat = classLoader.getResource("example12+13.nt").getFile();
        help(file1,file2,concat);
    }

    @Test
    public void cat9() {
        ClassLoader classLoader = getClass().getClassLoader();
        String file1 = classLoader.getResource("example14.nt").getFile();
        String file2 = classLoader.getResource("example15.nt").getFile();
        String concat = classLoader.getResource("example14+15.nt").getFile();
        help(file1,file2,concat);
    }

    @Test
    public void cat10() {
        ClassLoader classLoader = getClass().getClassLoader();
        String file1 = classLoader.getResource("example16.nt").getFile();
        String file2 = classLoader.getResource("example17.nt").getFile();
        String concat = classLoader.getResource("example16+17.nt").getFile();
        help(file1,file2,concat);
    }

    @Test
    public void cat11() {
        ClassLoader classLoader = getClass().getClassLoader();
        String file1 = classLoader.getResource("example1.nt").getFile();
        String file2 = classLoader.getResource("example1.nt").getFile();
        String concat = classLoader.getResource("example1.nt").getFile();
        help(file1,file2,concat);
    }

    @Override
    public void notifyProgress(float level, String message) {

    }

}
