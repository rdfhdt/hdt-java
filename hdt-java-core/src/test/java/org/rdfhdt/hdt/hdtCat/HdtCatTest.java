package org.rdfhdt.hdt.hdtCat;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.hdtCat.utils.UtilDictionary;
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
                UtilDictionary.printDictionary(hdtCatNew.getDictionary());
                UtilDictionary.compareDictionary(hdtCatOld.getDictionary(), hdtCatNew.getDictionary());

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

//    @Test
//    public void cat13() {
//        ClassLoader classLoader = getClass().getClassLoader();
//        try {
//            HDT hdt1 = HDTManager.mapHDT("/Users/Dennis/IdeaProjects/hdt-java/hdt-java-cli/lubm.1-4000Cat.hdt");
//            System.out.println(hdt1.getDictionary().getNshared());
//
//            IteratorTripleString it = hdt1.search("","","");
//            int count = 0;
//            while (it.hasNext()){
//                it.next();
//                count ++;
//                if (count%1000==0){
//                    System.out.println(count);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (NotFoundException e) {
//            e.printStackTrace();
//        }
//    }


//    @Test
//    public void cat14() {
//
//        ClassLoader classLoader = getClass().getClassLoader();
//        File file1 = new File(classLoader.getResource("wikidata.nt").getFile());
//        File file2 = new File(classLoader.getResource("example17.nt").getFile());
//        File concat = new File(classLoader.getResource("example16+17.nt").getFile());
//        HDT hdt1 = null;
//        HDT hdt2 = null;
//        try {
//            hdt1 = HDTManager.generateHDT(file1.getAbsolutePath(), "uri", RDFNotation.NTRIPLES, new HDTSpecification(), this);
//            hdt2 = HDTManager.generateHDT(file2.getAbsolutePath(), "uri", RDFNotation.NTRIPLES, new HDTSpecification(), this);
//            HDT hdtCatOld = HDTManager.generateHDT(concat.getAbsolutePath(), "uri", RDFNotation.NTRIPLES, new HDTSpecification(), this);
//            HDTCat hdtCatNew = new HDTCat(new HDTSpecification(),hdt1,hdt2,this);
//
//            File concatSave = new File(classLoader.getResource("example4+5Save.nt").getFile());
//            hdtCatNew.saveToHDT(concatSave.getAbsolutePath(),this);
//
//            IteratorTripleString it = hdt1.search("","","");
//            int count = 0;
//            while (it.hasNext()){
//                it.next();
//                count ++;
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (NotFoundException e) {
//            e.printStackTrace();
//        } catch (ParserException e) {
//            e.printStackTrace();
//        }
//    }

    @Override
    public void notifyProgress(float level, String message) {

    }

}
