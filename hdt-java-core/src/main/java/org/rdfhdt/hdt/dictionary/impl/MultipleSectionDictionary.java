package org.rdfhdt.hdt.dictionary.impl;

import org.rdfhdt.hdt.dictionary.DictionarySection;
import org.rdfhdt.hdt.dictionary.DictionarySectionPrivate;
import org.rdfhdt.hdt.dictionary.TempDictionary;
import org.rdfhdt.hdt.dictionary.impl.section.DictionarySectionFactory;
import org.rdfhdt.hdt.dictionary.impl.section.HashDictionarySection;
import org.rdfhdt.hdt.dictionary.impl.section.PFCDictionarySection;
import org.rdfhdt.hdt.exceptions.IllegalFormatException;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.header.Header;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInfo;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.util.CustomIterator;
import org.rdfhdt.hdt.util.LiteralsUtils;
import org.rdfhdt.hdt.util.io.CountInputStream;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.listener.IntermediateListener;
import org.rdfhdt.hdt.util.string.CompactString;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class
MultipleSectionDictionary extends MultipleBaseDictionary {


    public MultipleSectionDictionary(HDTOptions spec) {
        super(spec);
        // FIXME: Read type from spec.
        subjects = new PFCDictionarySection(spec);
        predicates = new PFCDictionarySection(spec);
        objects = new TreeMap<String, DictionarySectionPrivate>();
        shared = new PFCDictionarySection(spec);
    }

    /* (non-Javadoc)
     * @see hdt.dictionary.Dictionary#load(hdt.dictionary.Dictionary)
     */
    @Override
    public void load(TempDictionary other, ProgressListener listener) {
        IntermediateListener iListener = new IntermediateListener(listener);
        subjects.load(other.getSubjects(), iListener);
        predicates.load(other.getPredicates(), iListener);
        Iterator iter = other.getObjects().getEntries();

        // TODO: allow the usage of OneReadDictionarySection
        HashMap<String,Long> literalsCounts = ((HashDictionarySection)other.getObjects()).getLiteralsCounts();
        if(literalsCounts.containsKey("NO_DATATYPE"))
            literalsCounts.put("NO_DATATYPE",literalsCounts.get("NO_DATATYPE") - other.getShared().getNumberOfElements());
        CustomIterator customIterator = new CustomIterator(iter,literalsCounts);
        long startTime = System.currentTimeMillis();
        while (customIterator.hasNext()){
            PFCDictionarySection section = new PFCDictionarySection(spec);
            String type = LiteralsUtils.getType(customIterator.prev);
            long numEntries = literalsCounts.get(type);

            section.load(customIterator,numEntries,listener);
            long locate = section.locate(new CompactString("\"\uD83C\uDDEB\uD83C\uDDF7\"@ro"));
            objects.put(type,section);
        }
        long endTime = System.currentTimeMillis();
        //System.out.println("Loaded objects subsections in: "+(endTime - startTime)+" ms");
        shared.load(other.getShared(), iListener);
    }

    /* (non-Javadoc)
     * @see hdt.dictionary.Dictionary#save(java.io.OutputStream, hdt.ControlInformation, hdt.ProgressListener)
     */
    @Override
    public void save(OutputStream output, ControlInfo ci, ProgressListener listener) throws IOException {
        ci.setType(ControlInfo.Type.DICTIONARY);
        ci.setFormat(getType());
        ci.setInt("elements", this.getNumberOfElements());
        ci.save(output);

        IntermediateListener iListener = new IntermediateListener(listener);
        shared.save(output, iListener);
        subjects.save(output, iListener);
        predicates.save(output, iListener);

        writeLiteralsMap(output, iListener);

    }
    /*
    ------------------
    |len| Literal URI|
    ------------------
     */
    private void writeLiteralsMap(OutputStream output,ProgressListener listener) throws IOException {
        Iterator hmIterator = objects.entrySet().iterator();
        int numberOfTypes = objects.size();
        output.write(numberOfTypes);

        ArrayList<String> types = new ArrayList<>();

        while (hmIterator.hasNext()){
            Map.Entry entry = (Map.Entry)hmIterator.next();
            String uri = (String)entry.getKey();
            output.write(uri.length());
            IOUtil.writeBuffer(output, uri.getBytes(), 0, uri.getBytes().length, listener);
            types.add(uri);
        }
        for(String type:types){
            this.objects.get(type).save(output,listener);
        }
    }
    private void readLiteralsMap(InputStream input,ProgressListener listener) throws IOException {
        int numberOfTypes = input.read();
        ArrayList<String> types = new ArrayList<>();
        for (int i = 0; i < numberOfTypes; i++) {
            int length = input.read();
            byte[] type = IOUtil.readBuffer(input, length, listener);
            types.add(new String(type));
        }
        for(String type:types){
            this.objects.put(type,DictionarySectionFactory.loadFrom(input,listener));
        }
    }
    private void mapLiteralsMap(CountInputStream input,File f,ProgressListener listener) throws IOException {
        int numberOfTypes = input.read();
        ArrayList<String> types = new ArrayList<>();
        for (int i = 0; i < numberOfTypes; i++) {
            int length = input.read();
            byte[] type = IOUtil.readBuffer(input, length, listener);
            String typeStr = new String(type);
            types.add(typeStr);
        }
        for(String type:types){
            this.objects.put(type,DictionarySectionFactory.loadFrom(input,f,listener));
        }

    }


    /* (non-Javadoc)
     * @see hdt.dictionary.Dictionary#load(java.io.InputStream)
     */
    @Override
    public void load(InputStream input, ControlInfo ci, ProgressListener listener) throws IOException {
        if(ci.getType()!=ControlInfo.Type.DICTIONARY) {
            throw new IllegalFormatException("Trying to read a dictionary section, but was not dictionary.");
        }

        IntermediateListener iListener = new IntermediateListener(listener);

        shared = DictionarySectionFactory.loadFrom(input, iListener);
        subjects = DictionarySectionFactory.loadFrom(input, iListener);
        predicates = DictionarySectionFactory.loadFrom(input, iListener);

        readLiteralsMap(input,listener);
    }

    @Override
    public void mapFromFile(CountInputStream in, File f, ProgressListener listener) throws IOException {
        ControlInformation ci = new ControlInformation();
        ci.load(in);
        if(ci.getType()!=ControlInfo.Type.DICTIONARY) {
            throw new IllegalFormatException("Trying to read a dictionary section, but was not dictionary.");
        }

        IntermediateListener iListener = new IntermediateListener(listener);
        shared = DictionarySectionFactory.loadFrom(in, f, iListener);
        subjects = DictionarySectionFactory.loadFrom(in, f, iListener);
        predicates = DictionarySectionFactory.loadFrom(in, f, iListener);

        mapLiteralsMap(in,f,listener);

        // Use cache only for predicates. Preload only up to 100K predicates.
        // FIXME: DISABLED
//		predicates = new DictionarySectionCacheAll(predicates, predicates.getNumberOfElements()<100000);
    }

    @Override
    public long getNAllObjects() {
        Iterator hmIterator = objects.entrySet().iterator();
        long count = 0;
        while (hmIterator.hasNext()){
            Map.Entry entry = (Map.Entry)hmIterator.next();
            count += ((DictionarySectionPrivate)entry.getValue()).getNumberOfElements();
        }
        return count;
    }

    @Override
    public TreeMap<String, DictionarySection> getAllObjects() {
        return new TreeMap<>(objects);
    }

    /* (non-Javadoc)
     * @see hdt.dictionary.Dictionary#populateHeader(hdt.header.Header, java.lang.String)
     */
    @Override
    public void populateHeader(Header header, String rootNode) {
        header.insert(rootNode, HDTVocabulary.DICTIONARY_TYPE, getType());
//		header.insert(rootNode, HDTVocabulary.DICTIONARY_NUMSUBJECTS, getNsubjects());
//		header.insert(rootNode, HDTVocabulary.DICTIONARY_NUMPREDICATES, getNpredicates());
//		header.insert(rootNode, HDTVocabulary.DICTIONARY_NUMOBJECTS, getNobjects());
        header.insert(rootNode, HDTVocabulary.DICTIONARY_NUMSHARED, getNshared());
//		header.insert(rootNode, HDTVocabulary.DICTIONARY_MAXSUBJECTID, getMaxSubjectID());
//		header.insert(rootNode, HDTVocabulary.DICTIONARY_MAXPREDICATEID, getMaxPredicateID());
//		header.insert(rootNode, HDTVocabulary.DICTIONARY_MAXOBJECTTID, getMaxObjectID());
        header.insert(rootNode, HDTVocabulary.DICTIONARY_SIZE_STRINGS, size());
    }

    /* (non-Javadoc)
     * @see hdt.dictionary.Dictionary#getType()
     */
    @Override
    public String getType() {
        return HDTVocabulary.DICTIONARY_TYPE_MULT_SECTION;
    }

    @Override
    public void close() throws IOException {
        shared.close();
        subjects.close();
        predicates.close();

        // close all subsections
        Iterator hmIterator = objects.entrySet().iterator();
        while (hmIterator.hasNext()){
            Map.Entry entry = (Map.Entry)hmIterator.next();
            ((DictionarySectionPrivate)entry.getValue()).close();
        }

    }

    @Override
    public void loadAsync(TempDictionary other, ProgressListener listener) {
        throw new NotImplementedException();
    }
}
