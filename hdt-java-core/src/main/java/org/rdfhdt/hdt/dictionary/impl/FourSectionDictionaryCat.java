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
import org.rdfhdt.hdt.dictionary.DictionarySection;
import org.rdfhdt.hdt.dictionary.impl.utilCat.CatCommon;
import org.rdfhdt.hdt.dictionary.impl.utilCat.CatIterator;
import org.rdfhdt.hdt.dictionary.impl.utilCat.CatMapping;
import org.rdfhdt.hdt.dictionary.impl.utilCat.CatMappingBack;
import org.rdfhdt.hdt.dictionary.impl.utilCat.IteratorPlusString;
import org.rdfhdt.hdt.dictionary.impl.utilCat.IteratorPlusStringComparator;
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
import org.rdfhdt.hdt.util.string.CharSequenceComparator;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.apache.commons.math3.util.Pair;

public class FourSectionDictionaryCat {

    private String location;
    private int DEFAULT_BLOCK_SIZE = 16;
    private int BLOCK_PER_BUFFER = 1000000;
    private long numShared;
    private CatMapping mappingSh1;
    private CatMapping mappingSh2;
    private CatMapping mappingS1;
    private CatMapping mappingS2;
    private CatMapping mappingO1;
    private CatMapping mappingO2;
    private CatMapping mappingP1;
    private CatMapping mappingP2;
    private CatMappingBack mappingS;

    public FourSectionDictionaryCat(String location)  {
        this.location = location;
    }

    public void cat(Dictionary dictionary1, Dictionary dictionary2, ProgressListener listener){
        System.out.println("PREDICATES-------------------");
        mappingP1 = new CatMapping(location,"P1",(int)dictionary1.getPredicates().getNumberOfElements());
        mappingP2 = new CatMapping(location,"P2",(int)dictionary2.getPredicates().getNumberOfElements());
        int numCommonPredicates = 0;
        Iterator<Pair<Integer,Integer>> commonP1P2 = new CatCommon(dictionary1.getPredicates().getSortedEntries(),dictionary2.getPredicates().getSortedEntries());
        long maxPredicates = dictionary1.getPredicates().getNumberOfElements()+dictionary2.getPredicates().getNumberOfElements();
        while (commonP1P2.hasNext()){
            commonP1P2.next();
            numCommonPredicates++;
            //ListenerUtil.notifyCond(listener, "Analyze common predicates", numCommonPredicates, numCommonPredicates, maxPredicates);
        }
        long numPredicates = dictionary1.getPredicates().getNumberOfElements()+dictionary2.getPredicates().getNumberOfElements()-numCommonPredicates;
        catSection(numPredicates, 4,dictionary1.getPredicates(),dictionary2.getPredicates(), Collections.<Integer>emptyList().iterator(), Collections.<Integer>emptyList().iterator(), mappingP1, mappingP2, listener);

        System.out.println("SUBJECTS-------------------");
        CatIterator commonSubject1 = new CatIterator(new CatCommon(dictionary1.getSubjects().getSortedEntries(),dictionary2.getShared().getSortedEntries()),new CatCommon(dictionary1.getSubjects().getSortedEntries(),dictionary2.getObjects().getSortedEntries()));
        int numCommonSubject1Hdt2 = 0;
        while (commonSubject1.hasNext()){
            commonSubject1.next();
            numCommonSubject1Hdt2++;
        }
        CatIterator commonSubject2 = new CatIterator(new CatCommon(dictionary2.getSubjects().getSortedEntries(),dictionary1.getShared().getSortedEntries()),new CatCommon(dictionary2.getSubjects().getSortedEntries(),dictionary1.getObjects().getSortedEntries()));
        int numCommonSubject2Hdt1 = 0;
        while (commonSubject2.hasNext()){
            commonSubject2.next();
            numCommonSubject2Hdt1++;
        }
        int numCommonSubjects = 0;
        Iterator<Pair<Integer,Integer>> commonS1S2 = new CatCommon(dictionary1.getSubjects().getSortedEntries(),dictionary2.getSubjects().getSortedEntries());
        while (commonS1S2.hasNext()){
            commonS1S2.next();
            numCommonSubjects++;
        }
        long numSubjects = dictionary1.getSubjects().getNumberOfElements()+dictionary2.getSubjects().getNumberOfElements()-numCommonSubjects-numCommonSubject1Hdt2-numCommonSubject2Hdt1;
        mappingS1 = new CatMapping(location,"S1",(int)dictionary1.getSubjects().getNumberOfElements());
        mappingS2 = new CatMapping(location,"S2",(int)dictionary2.getSubjects().getNumberOfElements());
        commonSubject1 = new CatIterator(new CatCommon(dictionary1.getSubjects().getSortedEntries(),dictionary2.getShared().getSortedEntries()),new CatCommon(dictionary1.getSubjects().getSortedEntries(),dictionary2.getObjects().getSortedEntries()));
        commonSubject2 = new CatIterator(new CatCommon(dictionary2.getSubjects().getSortedEntries(),dictionary1.getShared().getSortedEntries()),new CatCommon(dictionary2.getSubjects().getSortedEntries(),dictionary1.getObjects().getSortedEntries()));
        catSection(numSubjects, 2,dictionary1.getSubjects(),dictionary2.getSubjects(), commonSubject1, commonSubject2, mappingS1, mappingS2, listener);

        System.out.println("OBJECTS-------------------");
        Iterator<Integer> commonObject1 = new CatIterator(new CatCommon(dictionary1.getObjects().getSortedEntries(),dictionary2.getShared().getSortedEntries()),new CatCommon(dictionary1.getObjects().getSortedEntries(),dictionary2.getSubjects().getSortedEntries()));
        int numCommonObject1Hdt2 = 0;
        while (commonObject1.hasNext()){
            commonObject1.next();
            numCommonObject1Hdt2++;
        }
        Iterator<Integer> commonObject2 = new CatIterator(new CatCommon(dictionary2.getObjects().getSortedEntries(),dictionary1.getShared().getSortedEntries()),new CatCommon(dictionary2.getObjects().getSortedEntries(),dictionary1.getSubjects().getSortedEntries()));
        int numCommonObject2Hdt1 = 0;
        while (commonObject2.hasNext()){
            commonObject2.next();
            numCommonObject2Hdt1++;
        }
        int numCommonObjects = 0;
        Iterator<Pair<Integer,Integer>> commonO1O2 = new CatCommon(dictionary1.getObjects().getSortedEntries(),dictionary2.getObjects().getSortedEntries());
        while (commonO1O2.hasNext()){
            commonO1O2.next();
            numCommonObjects++;
        }
        commonObject1 = new CatIterator(new CatCommon(dictionary1.getObjects().getSortedEntries(),dictionary2.getShared().getSortedEntries()),new CatCommon(dictionary1.getObjects().getSortedEntries(),dictionary2.getSubjects().getSortedEntries()));
        commonObject2 = new CatIterator(new CatCommon(dictionary2.getObjects().getSortedEntries(),dictionary1.getShared().getSortedEntries()),new CatCommon(dictionary2.getObjects().getSortedEntries(),dictionary1.getSubjects().getSortedEntries()));
        mappingO1 = new CatMapping(location, "O1",(int)dictionary1.getObjects().getNumberOfElements());
        mappingO2 = new CatMapping(location, "O2",(int)dictionary2.getObjects().getNumberOfElements());
        long numObject = dictionary1.getObjects().getNumberOfElements()+dictionary2.getObjects().getNumberOfElements()-numCommonObjects-numCommonObject1Hdt2-numCommonObject2Hdt1;
        catSection(numObject,3,dictionary1.getObjects(),dictionary2.getObjects(), commonObject1, commonObject2, mappingO1, mappingO2, listener);

        System.out.println("SHARED-------------------");
        Iterator<Pair<Integer,Integer>> i2 = new CatCommon(dictionary1.getSubjects().getSortedEntries(), dictionary2.getObjects().getSortedEntries());
        int numCommonS1O2=0;
        while (i2.hasNext()){
            i2.next();
            numCommonS1O2++;
        }
        Iterator<? extends CharSequence> it = dictionary2.getSubjects().getSortedEntries();
        i2 = new CatCommon(dictionary1.getObjects().getSortedEntries(), dictionary2.getSubjects().getSortedEntries());
        int numCommonO1S2=0;
        while (i2.hasNext()){
            i2.next();
            numCommonO1S2++;
        }

        i2 = new CatCommon(dictionary1.getShared().getSortedEntries(), dictionary2.getShared().getSortedEntries());
        int numCommonSh1Sh2=0;
        while (i2.hasNext()){
            i2.next();
            numCommonSh1Sh2++;
        }
        numShared = dictionary1.getShared().getNumberOfElements()+dictionary2.getShared().getNumberOfElements()-numCommonSh1Sh2+numCommonS1O2+numCommonO1S2;
        catShared(numShared, dictionary1, dictionary2, listener);

        //Putting the sections together
        ControlInfo ci = new ControlInformation();
        ci.setType(ControlInfo.Type.DICTIONARY);
        ci.setFormat(HDTVocabulary.DICTIONARY_TYPE_FOUR_SECTION);
        ci.setInt("elements", numSubjects+numPredicates+numObject+numShared);

        try {
            ci.save(new FileOutputStream(location + "dictionary"));
            FileOutputStream outFinal = new FileOutputStream(location + "dictionary",true);
            byte[] buf = new byte[100000];
            for (int i = 1; i <= 4; i++) {
                int j = i;
                if (i == 4){
                    j = 3;
                } else if (j == 3){
                    j = 4;
                }
                try {
                    InputStream in = new FileInputStream(location + "section" + j);
                    int b = 0;
                    while ((b = in.read(buf)) >= 0) {
                        outFinal.write(buf, 0, b);
                        outFinal.flush();
                    }
                    in.close();
                    Files.delete(Paths.get(location + "section" + j));
                } catch (FileNotFoundException e ){
                    e.printStackTrace();
                }
            }
            outFinal.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //calculate the inverse mapping for the subjects, i.e. from the new dictionary subject section to the old ones
        mappingS = new CatMappingBack(location,numSubjects+numShared);

        for (int i=0; i<mappingSh1.getSize(); i++){
            mappingS.set(mappingSh1.getMapping(i),i+1,1);
        }

        for (int i=0; i<mappingSh2.getSize(); i++){
            mappingS.set(mappingSh2.getMapping(i),i+1,2);
        }

        for (int i=0; i<mappingS1.getSize(); i++){
            if (mappingS1.getType(i)==1){
                mappingS.set(mappingS1.getMapping(i),(i+1+(int)dictionary1.getNshared()),1);
            } else {
                mappingS.set(mappingS1.getMapping(i)+(int)numShared,(i+1+(int)dictionary1.getNshared()),1);
            }
        }

        for (int i=0; i<mappingS2.getSize(); i++){
            if (mappingS2.getType(i)==1){
                mappingS.set(mappingS2.getMapping(i), (i + 1 + (int) dictionary2.getNshared()), 2);
            } else {
                mappingS.set(mappingS2.getMapping(i) + (int)numShared, (i + 1 + (int) dictionary2.getNshared()), 2);
            }
        }
    }

    public void catSection(long numentries, int type, DictionarySection dictionarySectionHdt1, DictionarySection dictionarySectionHdt2, Iterator<Integer> it1common, Iterator<Integer> it2common, CatMapping mappingHdt1, CatMapping mappingHdt2, ProgressListener listener) {
        CRCOutputStream out_buffer = null;
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
                case 3:
                    name = "object";
                case 4:
                    name = "predicate";
            }
            int count1 = 0;
            int count2 = 0;
            int skipSection1 = -1;
            int skipSection2 = -1;
            long storedBuffersSize = 0;
            long numBlocks = 0;
            long numberElements = 0;
            SequenceLog64BigDisk blocks = new SequenceLog64BigDisk(location+"SequenceLog64BigDisk"+type,64, numentries/16);
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream(16*1024);
            if (numentries > 0) {
                CharSequence previousStr=null;
                Iterator<? extends CharSequence> it1 = dictionarySectionHdt1.getSortedEntries();
                Iterator<? extends CharSequence> it2 = dictionarySectionHdt2.getSortedEntries();

                ArrayList<IteratorPlusString> list = new ArrayList<IteratorPlusString>();
                CharSequenceComparator comparator = new CharSequenceComparator();
                if (it1.hasNext()){
                    list.add(new IteratorPlusString(1,it1.next()));
                }
                if (it2.hasNext()){
                    list.add(new IteratorPlusString(2,it2.next()));
                }
                if (it1common.hasNext()){
                    skipSection1 = it1common.next();
                }
                if (it2common.hasNext()){
                    skipSection2 = it2common.next();
                }
                while(list.size()!=0) {
                    ListenerUtil.notifyCond(listener, "Analyze section "+name+" ", numberElements, numberElements, numentries);
                    Collections.sort(list, new IteratorPlusStringComparator());
                    boolean skip = false;
                    if (list.get(0).iterator==1){
                        if (count1==skipSection1){
                            skip = true;
                            if (it1.hasNext()) {
                                list.set(0, new IteratorPlusString(1, it1.next()));
                                count1++;
                            }  else {
                                list.remove(0);
                            }
                            if (it1common.hasNext()){
                                skipSection1 = it1common.next();
                            }
                        }
                    } else { //there are only two cases
                        if (count2==skipSection2){
                            skip = true;
                            if (it2.hasNext()) {
                                list.set(0, new IteratorPlusString(2, it2.next()));
                                count2++;
                            }  else {
                                list.remove(0);
                            }
                            if (it2common.hasNext()){
                                skipSection2 = it2common.next();
                            }
                        }
                    }
                    if (skip == false) {
                        String str = list.get(0).value.toString();
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
                        if (list.size() >= 2 && list.get(0).value.toString().equals(list.get(1).value.toString())) {
                            boolean removed = false;
                            mappingHdt1.set(count1, numberElements + 1, type);
                            count1++;
                            mappingHdt2.set(count2, numberElements + 1, type);
                            count2++;
                            if (it1.hasNext()) {
                                list.set(0, new IteratorPlusString(1, it1.next()));
                            } else {
                                list.remove(0);
                                removed = true;
                            }
                            if (it2.hasNext()) {
                                if (removed == true){
                                    list.set(0, new IteratorPlusString(2, it2.next()));
                                } else {
                                    list.set(1, new IteratorPlusString(2, it2.next()));
                                }
                            } else {
                                if (removed == true) {
                                    list.remove(0);
                                } else {
                                    list.remove(1);
                                }
                            }
                        } else if (list.get(0).iterator == 1) {
                            mappingHdt1.set(count1, numberElements + 1, type);
                            count1++;
                            if (it1.hasNext()) {
                                list.set(0, new IteratorPlusString(1, it1.next()));
                            } else {
                                list.remove(0);
                            }
                        } else if (list.get(0).iterator == 2) {
                            mappingHdt2.set(count2, numberElements + 1, type);
                            count2++;
                            if (it2.hasNext()) {
                                list.set(0, new IteratorPlusString(2, it2.next()));
                            } else {
                                list.remove(0);
                            }
                        }
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
    }

    public void catShared(long numentries, Dictionary dictionaryHdt1, Dictionary dictionaryHdt2, ProgressListener listener) {
        CRCOutputStream out_buffer = null;
        try {
            out_buffer = new CRCOutputStream(new FileOutputStream(location+"section_buffer_"+1), new CRC32());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        mappingSh1 = new CatMapping(location, "SH1",(int)dictionaryHdt1.getShared().getNumberOfElements());
        mappingSh2 = new CatMapping(location, "SH2",(int)dictionaryHdt2.getShared().getNumberOfElements());

        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream(16*1024);
            SequenceLog64BigDisk blocks = new SequenceLog64BigDisk(location+"SequenceLog64BigDisk"+1,64, numentries/16);
            CharSequence previousStr=null;
            int count1 = 0;
            int count2 = 0;
            Pair<Integer,Integer> idS1O2 = null;
            Pair<Integer,Integer> idO1S2 = null;
            long storedBuffersSize = 0;
            long numberElements = 0;
            long numBlocks = 0;
            if (numentries >0 ) {
                Iterator<? extends CharSequence> it1 = dictionaryHdt1.getShared().getSortedEntries();
                Iterator<? extends CharSequence> it2 = dictionaryHdt2.getShared().getSortedEntries();
                Iterator<Pair<Integer,Integer>> iteratorCommonSubjects1Objects2 = new CatCommon(dictionaryHdt1.getSubjects().getSortedEntries(), dictionaryHdt2.getObjects().getSortedEntries());
                Iterator<Pair<Integer,Integer>> iteratorCommonObjects1Subjects2 = new CatCommon(dictionaryHdt1.getObjects().getSortedEntries(), dictionaryHdt2.getSubjects().getSortedEntries());
                Iterator<Pair<Integer,Integer>> iteratorCommonShared1Subjects2 = new CatCommon(dictionaryHdt1.getShared().getSortedEntries(), dictionaryHdt2.getSubjects().getSortedEntries());
                Iterator<Pair<Integer,Integer>> iteratorCommonShared1Objects2 = new CatCommon(dictionaryHdt1.getShared().getSortedEntries(), dictionaryHdt2.getObjects().getSortedEntries());
                Iterator<Pair<Integer,Integer>> iteratorCommonShared2Subjects1 = new CatCommon(dictionaryHdt2.getShared().getSortedEntries(), dictionaryHdt1.getSubjects().getSortedEntries());
                Iterator<Pair<Integer,Integer>> iteratorCommonShared2Objects1 = new CatCommon(dictionaryHdt2.getShared().getSortedEntries(), dictionaryHdt1.getObjects().getSortedEntries());

                Pair<Integer,Integer> commonShared1Subjects2 = new Pair(-1,-1);
                Pair<Integer,Integer> commonShared1Objects2 = new Pair(-1,-1);
                Pair<Integer,Integer> commonShared2Subjects1 = new Pair(-1,-1);
                Pair<Integer,Integer> commonShared2Objects1 = new Pair(-1,-1);
                if (iteratorCommonShared1Subjects2.hasNext()){
                    commonShared1Subjects2 = iteratorCommonShared1Subjects2.next();
                }
                if (iteratorCommonShared1Objects2.hasNext()){
                    commonShared1Objects2 = iteratorCommonShared1Objects2.next();
                }
                if (iteratorCommonShared2Subjects1.hasNext()){
                    commonShared2Subjects1 = iteratorCommonShared2Subjects1.next();
                }
                if (iteratorCommonShared2Objects1.hasNext()){
                    commonShared2Objects1 = iteratorCommonShared2Objects1.next();
                }
                ArrayList<IteratorPlusString> list = new ArrayList<IteratorPlusString>();
                CharSequenceComparator comparator = new CharSequenceComparator();
                if (it1.hasNext()){
                    list.add(new IteratorPlusString(1,it1.next()));
                }
                if (it2.hasNext()){
                    list.add(new IteratorPlusString(2,it2.next()));
                }
                if (iteratorCommonSubjects1Objects2.hasNext()){
                    idS1O2 = iteratorCommonSubjects1Objects2.next();
                    list.add(new IteratorPlusString(3,dictionaryHdt1.getSubjects().extract(idS1O2.getKey()+1)));
                }
                if (iteratorCommonObjects1Subjects2.hasNext()){
                    idO1S2 = iteratorCommonObjects1Subjects2.next();
                    list.add(new IteratorPlusString(4,dictionaryHdt1.getObjects().extract(idO1S2.getKey()+1)));
                }


                while(list.size()!=0){
                    ListenerUtil.notifyCond(listener, "Analyze section shared ", numberElements, numberElements, numentries);
                    Collections.sort(list,new IteratorPlusStringComparator());
                    CharSequence str = list.get(0).value.toString();

                    if(numberElements%DEFAULT_BLOCK_SIZE==0) {
                        // Add new block pointer
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
                    if (list.size()>=2 && list.get(0).value.toString().equals(list.get(1).value.toString())){ //this case can only happen if the iterators are from the shared section
                        mappingSh1.set(count1,numberElements+1,1);
                        mappingSh2.set(count2,numberElements+1,1);


                        if (count1==commonShared1Subjects2.getKey()){
                            mappingS2.set(commonShared1Subjects2.getValue(),numberElements+1,1);
                        }
                        if (count1==commonShared1Objects2.getKey()){
                            mappingO2.set(commonShared1Objects2.getValue(),numberElements+1,1);
                        }
                        if (count2==commonShared2Subjects1.getKey()){
                            mappingS1.set(commonShared2Subjects1.getValue(),numberElements+1,1);
                        }
                        if (count2==commonShared2Objects1.getKey()){
                            mappingO1.set(commonShared2Objects1.getValue(),numberElements+1,1);
                        }
                        boolean removed = false;
                        if (it1.hasNext()) {
                            count1++;
                            list.set(0, new IteratorPlusString(1, it1.next()));
                            if (count1>commonShared1Subjects2.getKey() && iteratorCommonShared1Subjects2.hasNext()){
                                commonShared1Subjects2 = iteratorCommonShared1Subjects2.next();
                            }

                            if (count1>commonShared1Objects2.getKey() && iteratorCommonShared1Objects2.hasNext()){
                                commonShared1Objects2 = iteratorCommonShared1Objects2.next();
                            }
                        } else {
                            removed = true;
                        }
                        if (it2.hasNext()) {
                            list.set(1, new IteratorPlusString(2, it2.next()));
                            count2++;
                            if (count2>commonShared2Subjects1.getKey() && iteratorCommonShared2Subjects1.hasNext()){
                                commonShared2Subjects1 = iteratorCommonShared2Subjects1.next();
                            }

                            if (count2>commonShared2Objects1.getKey() && iteratorCommonShared2Objects1.hasNext()){
                                commonShared2Objects1 = iteratorCommonShared2Objects1.next();
                            }
                        } else {
                            list.remove(1);
                        }
                        if (removed==true){
                            list.remove(0);
                        }
                    } else if (list.get(0).iterator==1){
                        mappingSh1.set(count1,numberElements+1,1);

                        //Check if this is in common with Subjects2 or Objects2 for the mapping
                        if (count1==commonShared1Subjects2.getKey()){
                            mappingS2.set(commonShared1Subjects2.getValue(),numberElements+1,1);
                        }
                        if (count1==commonShared1Objects2.getKey()){
                            mappingO2.set(commonShared1Objects2.getValue(),numberElements+1,1);
                        }
                        if (it1.hasNext()) {
                            count1++;
                            list.set(0, new IteratorPlusString(1, it1.next()));
                            if (count1>commonShared1Subjects2.getKey() && iteratorCommonShared1Subjects2.hasNext()){
                                commonShared1Subjects2 = iteratorCommonShared1Subjects2.next();
                            }

                            if (count1>commonShared1Objects2.getKey() && iteratorCommonShared1Objects2.hasNext()){
                                commonShared1Objects2 = iteratorCommonShared1Objects2.next();
                            }


                        } else {
                            list.remove(0);
                        }
                    } else if (list.get(0).iterator==2){
                        mappingSh2.set(count2,numberElements+1,1);

                        //Check if this is in common with Subjects2 or Objects2 for the mapping
                        if (count2==commonShared2Subjects1.getKey()){
                            mappingS1.set(commonShared2Subjects1.getValue(),numberElements+1,1);
                        }
                        if (count2==commonShared2Objects1.getKey()){
                            mappingO1.set(commonShared2Objects1.getValue(),numberElements+1,1);
                        }
                        if (it2.hasNext()) {
                            list.set(0, new IteratorPlusString(2, it2.next()));
                            count2++;
                            if (count2>commonShared2Subjects1.getKey() && iteratorCommonShared2Subjects1.hasNext()){
                                commonShared2Subjects1 = iteratorCommonShared2Subjects1.next();
                            }

                            if (count2>commonShared2Objects1.getKey() && iteratorCommonShared2Objects1.hasNext()){
                                commonShared2Objects1 = iteratorCommonShared2Objects1.next();
                            }
                        } else {
                            list.remove(0);
                        }
                    } else if (list.get(0).iterator==3){
                        mappingS1.set(idS1O2.getKey(),numberElements+1,1);
                        mappingO2.set(idS1O2.getValue(),numberElements+1,1);
                        if (iteratorCommonSubjects1Objects2.hasNext()) {
                            idS1O2 = iteratorCommonSubjects1Objects2.next();
                            list.set(0,new IteratorPlusString(3,dictionaryHdt1.getSubjects().extract(idS1O2.getKey()+1)));
                        } else {
                            list.remove(0);
                        }
                    } else {
                        mappingO1.set(idO1S2.getKey(),numberElements+1,1);
                        mappingS2.set(idO1S2.getValue(),numberElements+1,1);
                        if (iteratorCommonObjects1Subjects2.hasNext()) {
                            idO1S2 = iteratorCommonObjects1Subjects2.next();
                            list.set(0,new IteratorPlusString(4,dictionaryHdt2.getSubjects().extract(idO1S2.getValue()+1)));
                        } else {
                            list.remove(0);
                        }
                    }
                    numberElements++;
                }

                // Ending block pointer.
                blocks.append(storedBuffersSize+byteOut.size());

                // Trim text/blocks
                blocks.aggressiveTrimToSize();

                byteOut.flush();
                IOUtil.writeBuffer(out_buffer, byteOut.toByteArray(), 0, byteOut.toByteArray().length, null);
            }
            out_buffer.writeCRC();
            out_buffer.close();
            //Save the section conforming to the HDT format
            CRCOutputStream out = new CRCOutputStream(new FileOutputStream(location+"section"+1), new CRC8());
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
            InputStream in = new FileInputStream(location+"section_buffer_"+1);
            int b = 0;
            while ( (b = in.read(buf)) >= 0) {
                out.write(buf, 0, b);
                out.flush();
            }
            out.close();
            Files.delete(Paths.get(location+"section_buffer_"+1));
            Files.delete(Paths.get(location+"SequenceLog64BigDisk"+1));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CatMapping getMappingSh1() {
        return mappingSh1;
    }

    public CatMapping getMappingSh2() {
        return mappingSh2;
    }

    public CatMapping getMappingS1() {
        return mappingS1;
    }

    public CatMapping getMappingS2() {
        return mappingS2;
    }

    public CatMapping getMappingO1() {
        return mappingO1;
    }

    public CatMapping getMappingO2() {
        return mappingO2;
    }

    public CatMapping getMappingP1() {
        return mappingP1;
    }

    public CatMapping getMappingP2() {
        return mappingP2;
    }

    public CatMappingBack getMappingS() {
        return mappingS;
    }

    public long getNumEntriesShared(){
        return numShared;
    }

}