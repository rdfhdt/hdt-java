/**
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 3.0 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Contacting the authors:
 *   Dennis Diefenbach:         dennis.diefenbach@univ-st-etienne.fr
 *   Jose Gimenez Garcia:       jose.gimenez.garcia@univ-st-etienne.fr
 */

package org.rdfhdt.hdt.dictionary.impl;

import org.rdfhdt.hdt.compact.integer.VByte;
import org.rdfhdt.hdt.compact.sequence.SequenceLog64BigDisk;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.dictionary.DictionaryCat;
import org.rdfhdt.hdt.dictionary.DictionarySection;
import org.rdfhdt.hdt.dictionary.impl.utilCat.*;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInfo;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.util.crc.CRC32;
import org.rdfhdt.hdt.util.crc.CRC8;
import org.rdfhdt.hdt.util.crc.CRCOutputStream;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.listener.ListenerUtil;
import org.rdfhdt.hdt.util.string.ByteStringUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MultipleSectionDictionaryCat implements DictionaryCat {

    private String location;
    private int DEFAULT_BLOCK_SIZE = 16;
    private int BLOCK_PER_BUFFER = 1000000;
    private long numShared;

    private HashMap<String,CatMapping> allMappings = new HashMap<>();

    private CatMappingBack mappingS;
    private String NO_DT_OBJECTS = "NO_DATATYPE";
    public MultipleSectionDictionaryCat(String location)  {
        this.location = location;
    }

    public void cat(Dictionary dictionary1, Dictionary dictionary2, ProgressListener listener){


        // Initialize all mappings ......

        allMappings.put("P1",new CatMapping(location,"P1",dictionary1.getPredicates().getNumberOfElements()));
        allMappings.put("P2",new CatMapping(location,"P2",dictionary2.getPredicates().getNumberOfElements()));
        allMappings.put("S1",new CatMapping(location,"S1",dictionary1.getSubjects().getNumberOfElements()));
        allMappings.put("S2",new CatMapping(location,"S2",dictionary2.getSubjects().getNumberOfElements()));
        allMappings.put("O1",new CatMapping(location, "O1",dictionary1.getNAllObjects()));
        allMappings.put("O2",new CatMapping(location, "O2",dictionary2.getNAllObjects()));
        allMappings.put("SH1",new CatMapping(location,"SH1",dictionary1.getShared().getNumberOfElements()));
        allMappings.put("SH2",new CatMapping(location,"SH2",dictionary2.getShared().getNumberOfElements()));
        Iterator hmIterator1 = dictionary1.getAllObjects().entrySet().iterator();
        int countSubSections1 = 0;
        int countSubSections2 = 0;

        while (hmIterator1.hasNext()){
            Map.Entry entry = (Map.Entry)hmIterator1.next();
            String prefix = "sub"+countSubSections1;
            if((entry.getKey()).equals(NO_DT_OBJECTS))
                prefix = (String)entry.getKey();
            allMappings.put(prefix+"1",new CatMapping(location,prefix+"1",
                    ((DictionarySection)entry.getValue()).getNumberOfElements()));
            countSubSections1++;
        }
        Iterator hmIterator2 = dictionary2.getAllObjects().entrySet().iterator();
        while (hmIterator2.hasNext()){
            Map.Entry entry = (Map.Entry)hmIterator2.next();
            String prefix = "sub"+countSubSections2;
            if((entry.getKey()).equals(NO_DT_OBJECTS))
                prefix = (String)entry.getKey();
            allMappings.put(prefix+"2",new CatMapping(location,prefix+"2",
                    ((DictionarySection)entry.getValue()).getNumberOfElements()));
            countSubSections2++;
        }


        System.out.println("PREDICATES-------------------");


        int numCommonPredicates = 0;
        CatIntersection commonP1P2 = new CatIntersection(new CatWrapper(dictionary1.getPredicates().getSortedEntries(),"P1"),
                new CatWrapper(dictionary2.getPredicates().getSortedEntries(),"P2"));
        long maxPredicates = dictionary1.getPredicates().getNumberOfElements()+dictionary2.getPredicates().getNumberOfElements();
        while (commonP1P2.hasNext()){
            commonP1P2.next();
            numCommonPredicates++;
        }
        long numPredicates = dictionary1.getPredicates().getNumberOfElements()+dictionary2.getPredicates().getNumberOfElements()-numCommonPredicates;

        ArrayList<Iterator<CatElement>> addPredicatesList = new ArrayList<>();
        addPredicatesList.add(new CatWrapper(dictionary1.getPredicates().getSortedEntries(),"P1"));
        addPredicatesList.add(new CatWrapper(dictionary2.getPredicates().getSortedEntries(),"P2"));
        CatUnion itAddPredicates = new CatUnion(addPredicatesList);
        catSection(numPredicates, 3,itAddPredicates, new CatUnion(new ArrayList<>()),allMappings, listener);


        System.out.println("SUBJECTS-------------------");
        ArrayList<Iterator<CatElement>> skipSubjectList = new ArrayList<>();

        skipSubjectList.add(new CatIntersection(new CatWrapper(dictionary1.getSubjects().getSortedEntries(),"S1"),
                new CatWrapper(dictionary2.getShared().getSortedEntries(),"SH2")));
        if(dictionary2.getAllObjects().containsKey(NO_DT_OBJECTS))
            skipSubjectList.add(new CatIntersection(new CatWrapper(dictionary1.getSubjects().getSortedEntries(),"S1"),
                    new CatWrapper(dictionary2.getAllObjects().get(NO_DT_OBJECTS).getSortedEntries(),NO_DT_OBJECTS+"2")));
        skipSubjectList.add(new CatIntersection(new CatWrapper(dictionary2.getSubjects().getSortedEntries(),"S2"),
                new CatWrapper(dictionary1.getShared().getSortedEntries(),"SH1")));
        if(dictionary1.getAllObjects().containsKey(NO_DT_OBJECTS))
            skipSubjectList.add(new CatIntersection(new CatWrapper(dictionary2.getSubjects().getSortedEntries(),"S2"),
                    new CatWrapper(dictionary1.getAllObjects().get(NO_DT_OBJECTS).getSortedEntries(),NO_DT_OBJECTS+"1")));
        CatUnion skipSubject = new CatUnion(skipSubjectList);
        int numSkipSubjects = 0;
        while (skipSubject.hasNext()){
            skipSubject.next();
            numSkipSubjects++;
        }
        int numCommonSubjects = 0;
        CatIntersection commonS1S2 = new CatIntersection(new CatWrapper(dictionary1.getSubjects().getSortedEntries(),"S1"),
                new CatWrapper(dictionary2.getSubjects().getSortedEntries(),"S2"));
        while (commonS1S2.hasNext()){
            commonS1S2.next();
            numCommonSubjects++;
        }
        long numSubjects = dictionary1.getSubjects().getNumberOfElements()+dictionary2.getSubjects().getNumberOfElements()-numCommonSubjects-numSkipSubjects;

        skipSubjectList = new ArrayList<>();

        skipSubjectList.add(new CatIntersection(new CatWrapper(dictionary1.getSubjects().getSortedEntries(),"S1"),
                new CatWrapper(dictionary2.getShared().getSortedEntries(),"SH2")));
        if(dictionary2.getAllObjects().containsKey(NO_DT_OBJECTS))
            skipSubjectList.add(new CatIntersection(new CatWrapper(dictionary1.getSubjects().getSortedEntries(),"S1"),
                    new CatWrapper(dictionary2.getAllObjects().get(NO_DT_OBJECTS).getSortedEntries(),NO_DT_OBJECTS+"2")));
        skipSubjectList.add(new CatIntersection(new CatWrapper(dictionary2.getSubjects().getSortedEntries(),"S2"),
                new CatWrapper(dictionary1.getShared().getSortedEntries(),"SH1")));
        if(dictionary1.getAllObjects().containsKey(NO_DT_OBJECTS))
            skipSubjectList.add(new CatIntersection(new CatWrapper(dictionary2.getSubjects().getSortedEntries(),"S2"),
                    new CatWrapper(dictionary1.getAllObjects().get(NO_DT_OBJECTS).getSortedEntries(),NO_DT_OBJECTS+"1")));
        skipSubject = new CatUnion(skipSubjectList);

        ArrayList<Iterator<CatElement>> addSubjectsList = new ArrayList<>();
        addSubjectsList.add(new CatWrapper(dictionary1.getSubjects().getSortedEntries(),"S1"));
        addSubjectsList.add(new CatWrapper(dictionary2.getSubjects().getSortedEntries(),"S2"));
        CatUnion itAddSubjects = new CatUnion(addSubjectsList);

        catSection(numSubjects, 2,itAddSubjects,skipSubject ,allMappings, listener);

        System.out.println("OBJECTS-------------------");
        ArrayList<Iterator<CatElement>> skipObjectsList = new ArrayList<>();
        if(dictionary1.getAllObjects().containsKey(NO_DT_OBJECTS)) {
            skipObjectsList.add(new CatIntersection(
                    new CatWrapper(dictionary1.getAllObjects().get(NO_DT_OBJECTS).getSortedEntries(), NO_DT_OBJECTS + "1"),
                    new CatWrapper(dictionary2.getShared().getSortedEntries(), "SH2"))
            );
            skipObjectsList.add(new CatIntersection(
                    new CatWrapper(dictionary1.getAllObjects().get(NO_DT_OBJECTS).getSortedEntries(), NO_DT_OBJECTS + "1"),
                    new CatWrapper(dictionary2.getSubjects().getSortedEntries(), "S2"))
            );
        }
        if(dictionary2.getAllObjects().containsKey(NO_DT_OBJECTS)) {
            skipObjectsList.add(new CatIntersection(
                    new CatWrapper(dictionary2.getAllObjects().get(NO_DT_OBJECTS).getSortedEntries(), NO_DT_OBJECTS + "2"),
                    new CatWrapper(dictionary1.getShared().getSortedEntries(), "SH1"))
            );
            skipObjectsList.add(new CatIntersection(
                    new CatWrapper(dictionary2.getAllObjects().get(NO_DT_OBJECTS).getSortedEntries(), NO_DT_OBJECTS + "2"),
                    new CatWrapper(dictionary1.getSubjects().getSortedEntries(), "S1"))
            );
        }
        CatUnion skipObject = new CatUnion(skipObjectsList);
        int numSkipObjects = 0;
        while (skipObject.hasNext()){
            skipObject.next();
            numSkipObjects++;
        }

        int numCommonObjects = 0;
        ArrayList<Iterator<CatElement>> commonObjectsList = new ArrayList<>();
        hmIterator1 = dictionary1.getAllObjects().entrySet().iterator();
        hmIterator2 = dictionary2.getAllObjects().entrySet().iterator();
        boolean skip1 = false;
        boolean skip2 = false;
        String dataType1 = "";
        String dataType2 = "";
        DictionarySection section1 = null;
        DictionarySection section2 = null;
        while (hmIterator1.hasNext() || hmIterator2.hasNext()){

            if(hmIterator1.hasNext()){
                if(!skip1) {
                    Map.Entry entry1 = (Map.Entry) hmIterator1.next();
                    section1 = (DictionarySection)entry1.getValue();
                    dataType1 = entry1.getKey().toString();
                }
            }
            if(hmIterator2.hasNext()){
                if(!skip2){
                    Map.Entry entry2 = (Map.Entry)hmIterator2.next();
                    section2 = (DictionarySection)entry2.getValue();
                    dataType2 = entry2.getKey().toString();
                }
            }
            if(section1 != null && section2 != null && dataType1.equals(dataType2)) {
                commonObjectsList.add(new CatIntersection(
                        new CatWrapper(section1.getSortedEntries(), dataType1 + "_1"),
                        new CatWrapper(section2.getSortedEntries(), dataType2 + "_2")
                ));
            }else{
                if(dataType1.compareTo(dataType2) > 0){
                    skip1 = true;
                    skip2 = false;
                }else if(dataType1.compareTo(dataType2) < 0){
                    skip1 = false;
                    skip2 = true;
                }
                if(!hmIterator2.hasNext()){
                    skip1 = false;
                }
                if(!hmIterator1.hasNext()){
                    skip2 = false;
                }
            }
        }
        CatUnion commonO1O2 = new CatUnion(commonObjectsList);
        while (commonO1O2.hasNext()){
            commonO1O2.next();
            numCommonObjects++;
        }


        skipObjectsList = new ArrayList<>();
        if(dictionary1.getAllObjects().containsKey(NO_DT_OBJECTS)) {
            skipObjectsList.add(new CatIntersection(
                    new CatWrapper(dictionary1.getAllObjects().get(NO_DT_OBJECTS).getSortedEntries(), NO_DT_OBJECTS + "1"),
                    new CatWrapper(dictionary2.getShared().getSortedEntries(), "SH2"))
            );
            skipObjectsList.add(new CatIntersection(
                    new CatWrapper(dictionary1.getAllObjects().get(NO_DT_OBJECTS).getSortedEntries(), NO_DT_OBJECTS + "1"),
                    new CatWrapper(dictionary2.getSubjects().getSortedEntries(), "S2")));
        }
        if(dictionary2.getAllObjects().containsKey(NO_DT_OBJECTS)) {
            skipObjectsList.add(new CatIntersection(
                    new CatWrapper(dictionary2.getAllObjects().get(NO_DT_OBJECTS).getSortedEntries(), NO_DT_OBJECTS + "2"),
                    new CatWrapper(dictionary1.getShared().getSortedEntries(), "SH1"))
            );
            skipObjectsList.add(new CatIntersection(
                    new CatWrapper(dictionary2.getAllObjects().get(NO_DT_OBJECTS).getSortedEntries(), NO_DT_OBJECTS + "2"),
                    new CatWrapper(dictionary1.getSubjects().getSortedEntries(), "S1"))
            );
        }
        skipObject = new CatUnion(skipObjectsList);

        long numObject = dictionary1.getNAllObjects()+dictionary2.getNAllObjects()-numCommonObjects-numSkipObjects;

        hmIterator1 = dictionary1.getAllObjects().entrySet().iterator();
        hmIterator2 = dictionary2.getAllObjects().entrySet().iterator();
        int type = 4;
        ArrayList<String> dataTypes = new ArrayList<>();
        // iterate over objects subsections and cat them together
        countSubSections1 = 0;
        countSubSections2 = 0;

        HashMap<String,Long> offsets = new HashMap<>();
        long total = 0;
        skip1 = false;
        skip2 = false;
        dataType1 = "";
        dataType2 = "";
        section1 = null;
        section2 = null;
        String prefix1 = "";
        String prefix2= "";

        while (hmIterator1.hasNext() || hmIterator2.hasNext()){
            ArrayList<Iterator<CatElement>> addObjectsList = new ArrayList<>();
            ArrayList<Iterator<CatElement>> countObjectsList = new ArrayList<>();
            if(hmIterator1.hasNext()){
                if(!skip1) {
                    Map.Entry entry = (Map.Entry) hmIterator1.next();
                    dataType1 = (String) entry.getKey();
                    section1 = ((DictionarySection) entry.getValue());
                    prefix1 = "sub" + countSubSections1;
                    if (dataType1.equals(NO_DT_OBJECTS))
                        prefix1 = dataType1;
                    countSubSections1++;
                }
            }
            if(hmIterator2.hasNext()){
                if(!skip2) {
                    Map.Entry entry = (Map.Entry) hmIterator2.next();
                    dataType2 = (String) entry.getKey();
                    section2 = ((DictionarySection) entry.getValue());
                    prefix2 = "sub" + countSubSections2;
                    if (dataType2.equals(NO_DT_OBJECTS))
                        prefix2 = dataType2;
                    countSubSections2++;
                }
            }
            String dataType = "";
            if(section1 != null && section2 != null && dataType1.equals(dataType2)){
                dataType = dataType1;
                addObjectsList.add(new CatWrapper(
                        section1.getSortedEntries(),
                        prefix1+"1")
                );
                countObjectsList.add(new CatWrapper(
                        section1.getSortedEntries(),
                        prefix1+"1")
                );
                addObjectsList.add(new CatWrapper(
                        section2.getSortedEntries(),
                        prefix2+"2")
                );
                countObjectsList.add(new CatWrapper(
                        section2.getSortedEntries(),
                        prefix2+"2")
                );
                skip1 = false;
                skip2 = false;
                if(!hmIterator1.hasNext()){
                    section1 = null;
                    dataType1 = "";
                }else if(!hmIterator2.hasNext()){
                    section2 = null;
                    dataType2 = "";
                }
            }else{
                boolean fromOne = false;
                boolean fromTwo = false;
                if(dataType1.equals("")){
                    fromTwo = true;
                }else if(dataType2.equals("")){
                    fromOne = true;
                }
                if(dataType1.compareTo(dataType2) < 0)
                    fromOne = true;
                if(dataType1.compareTo(dataType2) > 0)
                    fromTwo = true;
                if(section1!= null && fromOne){ // section 1 before section 2
                    dataType = dataType1;
                    addObjectsList.add(new CatWrapper(
                            section1.getSortedEntries(),
                            prefix1+"1")
                    );
                    countObjectsList.add(new CatWrapper(
                            section1.getSortedEntries(),
                            prefix1+"1")
                    );
                    if(!hmIterator1.hasNext()){
                        section1 = null;
                        dataType1 = "";
                        skip2 = false;
                    }else {
                        skip1 = false;
                        skip2 = true;
                    }
                }else if(section2!= null && fromTwo){
                    dataType = dataType2;
                    addObjectsList.add(new CatWrapper(
                            section2.getSortedEntries(),
                            prefix2+"2")
                    );
                    countObjectsList.add(new CatWrapper(
                            section2.getSortedEntries(),
                            prefix2+"2")
                    );
                    if(!hmIterator2.hasNext()){
                        section2 = null;
                        dataType2 = "";
                        skip1 = false;
                    }else {
                        skip1 = true;
                        skip2 = false;
                    }
                }
            }
            long numberElts = 0;
            CatUnion itCountObjects = new CatUnion(countObjectsList);
            while (itCountObjects.hasNext()){
                numberElts++;
                itCountObjects.next();
            }

            CatUnion itAddObjects = new CatUnion(addObjectsList);
            // subtract the number of objects to be skipped - if creating do data type section
            if(dataType.equals(NO_DT_OBJECTS)) {
                numberElts -= numSkipObjects;
                catSection(numberElts, type, itAddObjects, skipObject, allMappings, listener);
            }
            else // if catting literals sections .. nothing will move (nothing to be skipped)
                catSection(numberElts,type,itAddObjects,new CatUnion(new ArrayList<>()),allMappings,listener);
            if(numberElts > 0 ) {
                dataTypes.add(dataType);
                offsets.put(dataType, total);
            }
            total+=numberElts;
            type++;
        }
        System.out.println("SHARED-------------------");
        int numCommonS1O2 = 0;
        if(dictionary2.getAllObjects().containsKey(NO_DT_OBJECTS)) {
            CatIntersection i2 = new CatIntersection(new CatWrapper(dictionary1.getSubjects().getSortedEntries(), "S1"), new CatWrapper(dictionary2.getAllObjects().get(NO_DT_OBJECTS).getSortedEntries(), NO_DT_OBJECTS + "2"));
            while (i2.hasNext()) {
                i2.next();
                numCommonS1O2++;
            }
        }
        Iterator<? extends CharSequence> it = dictionary2.getSubjects().getSortedEntries();
        int numCommonO1S2 = 0;
        if(dictionary1.getAllObjects().containsKey(NO_DT_OBJECTS)) {
            CatIntersection i2 = new CatIntersection(new CatWrapper(dictionary1.getAllObjects().get(NO_DT_OBJECTS).getSortedEntries(), NO_DT_OBJECTS + "1"), new CatWrapper(dictionary2.getSubjects().getSortedEntries(), "S2"));
            while (i2.hasNext()) {
                i2.next();
                numCommonO1S2++;
            }
        }

        CatIntersection i2 = new CatIntersection(new CatWrapper(dictionary1.getShared().getSortedEntries(),"SH1"),new CatWrapper( dictionary2.getShared().getSortedEntries(),"SH2"));
        int numCommonSh1Sh2=0;
        while (i2.hasNext()){
            i2.next();
            numCommonSh1Sh2++;
        }
        numShared = dictionary1.getShared().getNumberOfElements()+dictionary2.getShared().getNumberOfElements()-numCommonSh1Sh2+numCommonS1O2+numCommonO1S2;

        ArrayList<Iterator<CatElement>> addSharedList = new ArrayList<>();
        addSharedList.add(new CatWrapper(dictionary1.getShared().getSortedEntries(),"SH1"));
        addSharedList.add(new CatWrapper(dictionary2.getShared().getSortedEntries(),"SH2"));

        if(dictionary1.getAllObjects().containsKey(NO_DT_OBJECTS)) {
            addSharedList.add(new CatIntersection(new CatWrapper(dictionary1.getAllObjects().get(NO_DT_OBJECTS).getSortedEntries(), NO_DT_OBJECTS + "1"), new CatWrapper(dictionary2.getShared().getSortedEntries(), "SH2")));
            addSharedList.add(new CatIntersection(new CatWrapper(dictionary2.getSubjects().getSortedEntries(), "S2"), new CatWrapper(dictionary1.getAllObjects().get(NO_DT_OBJECTS).getSortedEntries(), NO_DT_OBJECTS + "1")));
        }
        if(dictionary2.getAllObjects().containsKey(NO_DT_OBJECTS)) {
            addSharedList.add(new CatIntersection(new CatWrapper(dictionary1.getSubjects().getSortedEntries(), "S1"), new CatWrapper(dictionary2.getAllObjects().get(NO_DT_OBJECTS).getSortedEntries(), NO_DT_OBJECTS + "2")));
            addSharedList.add(new CatIntersection(new CatWrapper(dictionary2.getAllObjects().get(NO_DT_OBJECTS).getSortedEntries(), NO_DT_OBJECTS + "2"), new CatWrapper(dictionary1.getShared().getSortedEntries(), "SH1")));
        }
        addSharedList.add(new CatIntersection(new CatWrapper(dictionary1.getSubjects().getSortedEntries(),"S1"),new CatWrapper(dictionary2.getShared().getSortedEntries(),"SH2")));
        addSharedList.add(new CatIntersection(new CatWrapper(dictionary2.getSubjects().getSortedEntries(),"S2"),new CatWrapper(dictionary1.getShared().getSortedEntries(),"SH1")));

        CatUnion itAddShared = new CatUnion(addSharedList);
        catSection(numShared, 1,itAddShared,new CatUnion(new ArrayList<>()) ,allMappings, listener);


        //Putting the sections together
        ControlInfo ci = new ControlInformation();
        ci.setType(ControlInfo.Type.DICTIONARY);
        ci.setFormat(HDTVocabulary.DICTIONARY_TYPE_FOUR_SECTION);
        ci.setInt("elements", numSubjects+numPredicates+numObject+numShared);

        try {
            ci.save(new FileOutputStream(location + "dictionary"));
            FileOutputStream outFinal = new FileOutputStream(location + "dictionary",true);
            byte[] buf = new byte[100000];
            for (int i = 1; i <= 3 + dataTypes.size(); i++) {
                try {
                    if(i == 4){ // write literals map before writing the objects sections
                        outFinal.write(dataTypes.size());
                        for(String datatype:dataTypes){
                            outFinal.write(datatype.length());
                            IOUtil.writeBuffer(outFinal, datatype.getBytes(), 0, datatype.getBytes().length, listener);
                        }
                    }
                    InputStream in = new FileInputStream(location + "section" + i);
                    int b = 0;
                    while ((b = in.read(buf)) >= 0) {
                        outFinal.write(buf, 0, b);
                        outFinal.flush();
                    }
                    in.close();
                    Files.delete(Paths.get(location + "section" + i));
                } catch (FileNotFoundException e ){
                    e.printStackTrace();
                }
            }
            outFinal.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // create the objects mappings
        long oldId = 0;
        hmIterator1 = dictionary1.getAllObjects().entrySet().iterator();
        countSubSections1 = 0;
        countSubSections2 = 0;
        while (hmIterator1.hasNext()){
            Map.Entry entry = (Map.Entry)hmIterator1.next();
            String dataType = (String)entry.getKey();
            String prefix = "sub"+countSubSections1;
            if(dataType.equals(NO_DT_OBJECTS))
                prefix = dataType+"1";
            else
                prefix +="1";
            if(allMappings.containsKey(prefix)) {
                CatMapping mapping = allMappings.get(prefix);
                for (int i = 0; i < mapping.getSize(); i++) {
                    long newId = mapping.getMapping(i);
                    if (mapping.getType(i) != 1 && offsets.containsKey(dataType)) {
                        newId = newId + offsets.get(dataType);
                    }
                    allMappings.get("O1").set(oldId, newId, (int) mapping.getType(i));
                    oldId++;
                }
            }
            countSubSections1++;
        }

        oldId = 0;
        hmIterator2 = dictionary2.getAllObjects().entrySet().iterator();
        while (hmIterator2.hasNext()){
            Map.Entry entry = (Map.Entry)hmIterator2.next();
            String dataType = (String)entry.getKey();
            String prefix = "sub"+countSubSections2;
            if(dataType.equals(NO_DT_OBJECTS))
                prefix = dataType+"2";
            else
                prefix +="2";
            if(allMappings.containsKey(prefix)) {
                CatMapping mapping = allMappings.get(prefix);
                countSubSections2++;
                for (int i = 0; i < mapping.getSize(); i++) {
                    long newId = mapping.getMapping(i);
                    if (mapping.getType(i) != 1 && offsets.containsKey(dataType))
                        newId = newId + offsets.get(dataType);
                    allMappings.get("O2").set(oldId, newId, (int) mapping.getType(i));
                    oldId++;
                }
            }
        }
        //printMappings();
        //System.out.println("Num shared: "+numShared);
        //calculate the inverse mapping for the subjects, i.e. from the new dictionary subject section to the old ones
        mappingS = new CatMappingBack(location,numSubjects+numShared);

        for (int i=0; i<allMappings.get("SH1").getSize(); i++){
            mappingS.set(allMappings.get("SH1").getMapping(i),i+1,1);
        }

        for (int i=0; i<allMappings.get("SH2").getSize(); i++){
            mappingS.set(allMappings.get("SH2").getMapping(i),i+1,2);
        }

        for (int i=0; i<allMappings.get("S1").getSize(); i++){
            if (allMappings.get("S1").getType(i)==1){
                mappingS.set(allMappings.get("S1").getMapping(i),(i+1+(int)dictionary1.getNshared()),1);
            } else {
                mappingS.set(allMappings.get("S1").getMapping(i)+(int)numShared,(i+1+(int)dictionary1.getNshared()),1);
            }
        }

        for (int i=0; i<allMappings.get("S2").getSize(); i++){
            if (allMappings.get("S2").getType(i)==1){
                mappingS.set(allMappings.get("S2").getMapping(i), (i + 1 + (int) dictionary2.getNshared()), 2);
            } else {
                mappingS.set(allMappings.get("S2").getMapping(i) + (int)numShared, (i + 1 + (int) dictionary2.getNshared()), 2);
            }
        }
    }
    public void printMappings(){
        Iterator iterMap = allMappings.entrySet().iterator();
        while (iterMap.hasNext()){
            Map.Entry entry = (Map.Entry)iterMap.next();
            CatMapping mapping = (CatMapping)entry.getValue();
            System.out.println(entry.getKey());
            for(int i=0;i<mapping.getSize();i++){
                System.out.println(mapping.getMapping(i)+" type:"+mapping.getType(i));
            }
            System.out.println("----------------------");
        }
    }
    public long catSection(long numEntries, int type, CatUnion itAdd , CatUnion itSkip , HashMap<String,CatMapping> mappings, ProgressListener listener) {
        CRCOutputStream out_buffer = null;
        long numberElements = 0;
        try {
            out_buffer = new CRCOutputStream(new FileOutputStream(location+"section_buffer_"+type), new CRC32());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            String name = "";
            switch (type) {
                case 2:
                    name = "subject";
                    break;
                case 3:
                    name = "object";
                     break;
                case 4:
                    name = "predicate";
            }
            long storedBuffersSize = 0;
            long numBlocks = 0;
            SequenceLog64BigDisk blocks = new SequenceLog64BigDisk(location+"SequenceLog64BigDisk"+type,64, numEntries/16);
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream(16*1024);
            if (numEntries > 0) {
                CharSequence previousStr=null;

                CatElement skipElement = null;
                if(itSkip.hasNext()){
                    skipElement = itSkip.next();
                }
                while (itAdd.hasNext()){
                    ListenerUtil.notifyCond(listener, "Analyze section "+name+" ", numberElements, numberElements, numEntries);
                    CatElement nextElement = itAdd.next();
                    Boolean skip = false;
                    if(skipElement!= null && nextElement.entity.toString().equals(skipElement.entity.toString()))
                        skip = true;
                    else {
                        for (int i = 0; i < nextElement.IDs.size(); i++) {
                            long id = nextElement.IDs.get(i).pos;
                            String iter = nextElement.IDs.get(i).iter.toString();
                            mappings.get(iter).set(id - 1, numberElements + 1, type);
                        }
                    }
                    if(skip){
                        if(itSkip.hasNext())
                            skipElement = itSkip.next();
                        else
                            skipElement = null;
                    }else{
                        String str = nextElement.entity.toString();
                        if (numberElements % DEFAULT_BLOCK_SIZE == 0) {
                            blocks.append(storedBuffersSize + byteOut.size());
                            numBlocks++;

                            // if a buffer is filled, flush the byteOut and store it
                            if (((numBlocks - 1) % BLOCK_PER_BUFFER == 0) && ((numBlocks - 1) / BLOCK_PER_BUFFER != 0)) {
                                storedBuffersSize += byteOut.size();
                                byteOut.flush();
                                IOUtil.writeBuffer(out_buffer, byteOut.toByteArray(), 0, byteOut.toByteArray().length, null);
                                byteOut.close();
                                byteOut = new ByteArrayOutputStream(16 * 1024);
                            }

                            // Copy full string
                            ByteStringUtil.append(byteOut, str, 0);
                        } else {
                            // Find common part.
                            int delta = ByteStringUtil.longestCommonPrefix(previousStr, str);
                            // Write Delta in VByte
                            VByte.encode(byteOut, delta);
                            // Write remaining
                            ByteStringUtil.append(byteOut, str, delta);
                        }
                        byteOut.write(0); // End of string
                        previousStr = str;
                        numberElements += 1;
                    }
                }
            }
            // Ending block pointer.
            blocks.append(storedBuffersSize+byteOut.size());
            // Trim text/blocks
            blocks.aggressiveTrimToSize();
            byteOut.flush();
            //section.addBuffer(buffer, byteOut.toByteArray());
            IOUtil.writeBuffer(out_buffer, byteOut.toByteArray(), 0, byteOut.toByteArray().length, null);
            out_buffer.writeCRC();
            out_buffer.close();
            //Save the section conforming to the HDT format
            CRCOutputStream out = new CRCOutputStream(new FileOutputStream(location+"section"+type), new CRC8());
            //write the index type
            out.write(2);
            //write the number of strings
            VByte.encode(out, numberElements);
            //write the datasize
            VByte.encode(out, storedBuffersSize+byteOut.size());
            //wirte the blocksize
            VByte.encode(out, DEFAULT_BLOCK_SIZE);
            //write CRC
            out.writeCRC();
            //write the blocks
            blocks.save(out, null);	// Write blocks directly to output, they have their own CRC check.
            blocks.close();
            //write out_buffer
            byte[] buf = new byte[100000];
            InputStream in = new FileInputStream(location+"section_buffer_"+type);
            int b = 0;
            while ( (b = in.read(buf)) >= 0) {
                out.write(buf, 0, b);
                out.flush();
            }
            out.close();
            Files.delete(Paths.get(location+"section_buffer_"+type));
            Files.delete(Paths.get(location+"SequenceLog64BigDisk"+type));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return numberElements;
    }

    public CatMappingBack getMappingS() {
        return mappingS;
    }

    public HashMap<String, CatMapping> getAllMappings() {
        return allMappings;
    }

    public long getNumShared() {
        return numShared;
    }
}