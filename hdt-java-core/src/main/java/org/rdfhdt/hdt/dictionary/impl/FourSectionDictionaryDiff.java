package org.rdfhdt.hdt.dictionary.impl;

import org.rdfhdt.hdt.compact.bitmap.Bitmap;
import org.rdfhdt.hdt.compact.bitmap.ModifiableBitmap;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.dictionary.DictionaryDiff;
import org.rdfhdt.hdt.dictionary.impl.utilCat.CatElement;
import org.rdfhdt.hdt.dictionary.impl.utilCat.CatIntersection;
import org.rdfhdt.hdt.dictionary.impl.utilCat.CatMapping;
import org.rdfhdt.hdt.dictionary.impl.utilCat.CatUnion;
import org.rdfhdt.hdt.dictionary.impl.utilCat.SectionUtil;
import org.rdfhdt.hdt.dictionary.impl.utilDiff.DiffWrapper;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInfo;
import org.rdfhdt.hdt.options.ControlInformation;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FourSectionDictionaryDiff implements DictionaryDiff {


    private final String location;
    private final HashMap<String, CatMapping> allMappings = new HashMap<>();
    private CatMapping mappingBack;
    public long numShared;

    public FourSectionDictionaryDiff(String location) {
        this.location = location;
    }

    @Override
    public void diff(Dictionary dictionary, Map<String, ModifiableBitmap> bitmaps, ProgressListener listener) throws IOException {
        allMappings.put("predicate", new CatMapping(location, "predicate", dictionary.getPredicates().getNumberOfElements()));
        allMappings.put("subject", new CatMapping(location, "subject", dictionary.getSubjects().getNumberOfElements()));
        allMappings.put("object", new CatMapping(location, "object", dictionary.getObjects().getNumberOfElements()));
        allMappings.put("shared", new CatMapping(location, "shared", dictionary.getShared().getNumberOfElements()));
//        allMappings.put("shared_o",new CatMapping(location,"shared_o",dictionary.getShared().getNumberOfElements()));

        // Predicates
        Bitmap predicatesBitMap = bitmaps.get("P");

        Iterator<? extends CharSequence> predicates = dictionary.getPredicates().getSortedEntries();
        DiffWrapper itSkipPreds = new DiffWrapper(predicates, predicatesBitMap, "predicate");

        ArrayList<Iterator<CatElement>> listSkipPred = new ArrayList<>();
        listSkipPred.add(itSkipPreds);

        long numPreds = predicatesBitMap.countOnes();

        SectionUtil.createSection(location, numPreds, 4, new CatUnion(listSkipPred), new CatUnion(new ArrayList<>()), allMappings, 0, listener);
        //diffSection(numPreds,4,predicates,predicatesBitMap,allMappings,listener);

        // Subjects
        Bitmap subjectsBitMap = bitmaps.get("S");
        Iterator<? extends CharSequence> subjects = dictionary.getSubjects().getSortedEntries();

        DiffWrapper itSkipSubs = new DiffWrapper(subjects, subjectsBitMap, "subject");

        List<Iterator<CatElement>> listSkipSubj = new ArrayList<>();
        listSkipSubj.add(itSkipSubs);

        SharedWrapper sharedWrapper = new SharedWrapper(0, bitmaps.get("SH_S"), bitmaps.get("SH_O"), dictionary.getShared().getSortedEntries());
        long numNewSubj = sharedWrapper.count();
        sharedWrapper = new SharedWrapper(0, bitmaps.get("SH_S"), bitmaps.get("SH_O"), dictionary.getShared().getSortedEntries());
        listSkipSubj.add(sharedWrapper);

        long numSubj = subjectsBitMap.countOnes() + numNewSubj;
        SectionUtil.createSection(location, numSubj, 2, new CatUnion(listSkipSubj), new CatUnion(new ArrayList<>()), allMappings, 0, listener);


        // Objects ----------------------------+++++++++++++++++++++++++++++++++----------------------------------------

        Bitmap objectsBitMap = bitmaps.get("O");
        Iterator<? extends CharSequence> objects = dictionary.getObjects().getSortedEntries();

        DiffWrapper itSkipObjs = new DiffWrapper(objects, objectsBitMap, "object");

        ArrayList<Iterator<CatElement>> listSkipObjs = new ArrayList<>();
        listSkipObjs.add(itSkipObjs);

        // flag = 1 for objects
        sharedWrapper = new SharedWrapper(1, bitmaps.get("SH_S"), bitmaps.get("SH_O"), dictionary.getShared().getSortedEntries());
        long numNewObj = sharedWrapper.count();
        sharedWrapper = new SharedWrapper(1, bitmaps.get("SH_S"), bitmaps.get("SH_O"), dictionary.getShared().getSortedEntries());
        listSkipObjs.add(sharedWrapper);

        long numObject = objectsBitMap.countOnes() + numNewObj;

        SectionUtil.createSection(location, numObject, 3, new CatUnion(listSkipObjs), new CatUnion(new ArrayList<>()), allMappings, 0, listener);
        // Shared
        Bitmap sharedSubjBitMap = bitmaps.get("SH_S");
        Bitmap sharedObjBitMap = bitmaps.get("SH_O");

        Iterator<? extends CharSequence> shared = dictionary.getShared().getSortedEntries();

        DiffWrapper sharedSubj = new DiffWrapper(shared, sharedSubjBitMap, "shared");

        shared = dictionary.getShared().getSortedEntries();

        DiffWrapper sharedObj = new DiffWrapper(shared, sharedObjBitMap, "shared");

        ArrayList<Iterator<CatElement>> listShared = new ArrayList<>();
        listShared.add(new CatIntersection(sharedSubj, sharedObj));

        CatUnion union = new CatUnion(listShared);
        while (union.hasNext()) {
            union.next();
            numShared++;
        }

        listShared = new ArrayList<>();
        sharedSubj = new DiffWrapper(dictionary.getShared().getSortedEntries(), sharedSubjBitMap, "shared");

        sharedObj = new DiffWrapper(dictionary.getShared().getSortedEntries(), sharedObjBitMap, "shared");
        listShared.add(new CatIntersection(sharedSubj, sharedObj));

        SectionUtil.createSection(location, numShared, 1, new CatUnion(listShared), new CatUnion(new ArrayList<>()), allMappings, 0, listener);
        //diffSection(numShared,1,shared,sharedBitMap,allMappings,listener);

        //Putting the sections together
        ControlInfo ci = new ControlInformation();
        ci.setType(ControlInfo.Type.DICTIONARY);
        ci.setFormat(dictionary.getType());

        ci.setInt("elements", numSubj + numPreds + numObject + numShared);

        try (OutputStream outFinal = new FileOutputStream(location + "dictionary")) {
            ci.save(outFinal);

            byte[] buf = new byte[100000];
            for (int i = 1; i <= 4; i++) {
                int j = i;
                if (i == 4) {
                    j = 3;
                } else if (j == 3) {
                    j = 4;
                }
                try (InputStream in = new FileInputStream(location + "section" + j)) {
                    int b;
                    while ((b = in.read(buf)) >= 0) {
                        outFinal.write(buf, 0, b);
                        outFinal.flush();
                    }
                }
                Files.delete(Paths.get(location + "section" + j));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mappingBack = new CatMapping(location, "back", numSubj + numShared);

        if (mappingBack.getSize() > 0) {
            for (int i = 0; i < allMappings.get("shared").getSize(); i++) {
                long type = allMappings.get("shared").getType(i);
                if (type == 1) {
                    mappingBack.set(allMappings.get("shared").getMapping(i) - 1, i + 1, 1);
                } else if (type == 2) {
                    mappingBack.set(allMappings.get("shared").getMapping(i) + numShared - 1, i + 1, 2);
                }
            }

            for (int i = 0; i < allMappings.get("subject").getSize(); i++) {
                long type = allMappings.get("subject").getType(i);
                if (type == 1) {
                    mappingBack.set(allMappings.get("subject").getMapping(i) - 1, (i + 1 + (int) dictionary.getNshared()), 1);
                } else if (type == 2) {
                    mappingBack.set(allMappings.get("subject").getMapping(i) + numShared - 1, (i + 1 + (int) dictionary.getNshared()), 2);
                }
            }
        }
    }

    private static class SharedWrapper implements Iterator<CatElement> {
        private final Bitmap bitmapSub;
        private final Bitmap bitmapObj;
        private final Iterator<? extends CharSequence> sectionIter;
        private final int flag; // 0 = subject ; 1 = object
        private CatElement next;
        private long count = 0;

        public SharedWrapper(int flag, Bitmap bitmapSub, Bitmap bitmapObj, Iterator<? extends CharSequence> sectionIter) {
            this.bitmapSub = bitmapSub;
            this.bitmapObj = bitmapObj;
            this.sectionIter = sectionIter;
            this.flag = flag;
        }

        @Override
        public boolean hasNext() {

            while (sectionIter.hasNext()) {
                CharSequence element = sectionIter.next();
                if ((flag == 0 && bitmapSub.access(count) && !bitmapObj.access(count)) || (flag == 1 && bitmapObj.access(count) && !bitmapSub.access(count))) {
                    ArrayList<CatElement.IteratorPlusPosition> IDs = new ArrayList<>();
                    IDs.add(new CatElement.IteratorPlusPosition("shared", count + 1));
                    next = new CatElement(element, IDs);
                    count++;
                    return true;
                }

                count++;
            }
            return false;
        }

        @Override
        public CatElement next() {
            return next;
        }
        
        public int count() {
            int i = 0;
            while (hasNext()) {
                // next();
                i++;
            }
            return i;
        }
    }

    @Override
    public HashMap<String, CatMapping> getAllMappings() {
        return allMappings;
    }

    @Override
    public CatMapping getMappingBack() {
        return mappingBack;
    }

    @Override
    public long getNumShared() {
        return numShared;
    }
}
