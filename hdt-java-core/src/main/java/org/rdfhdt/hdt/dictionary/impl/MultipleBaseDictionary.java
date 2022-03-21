package org.rdfhdt.hdt.dictionary.impl;


import org.rdfhdt.hdt.dictionary.DictionaryPrivate;
import org.rdfhdt.hdt.dictionary.DictionarySection;
import org.rdfhdt.hdt.dictionary.DictionarySectionPrivate;
import org.rdfhdt.hdt.dictionary.impl.section.PFCOptimizedExtractor;
import org.rdfhdt.hdt.enums.DictionarySectionRole;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.util.LiteralsUtils;
import org.rdfhdt.hdt.util.string.CompactString;
import org.rdfhdt.hdt.util.string.DelayedString;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public abstract class MultipleBaseDictionary implements DictionaryPrivate {

    protected final HDTOptions spec;

    protected DictionarySectionPrivate subjects;
    protected DictionarySectionPrivate predicates;
    protected TreeMap<String,DictionarySectionPrivate> objects;
    protected DictionarySectionPrivate shared;

    public MultipleBaseDictionary(HDTOptions spec) {
        this.spec = spec;
    }

    protected long getGlobalId(long id, DictionarySectionRole position,CharSequence str) {
        switch (position) {
            case SUBJECT:
                return id + shared.getNumberOfElements();
            case OBJECT: {
                Iterator iter = objects.entrySet().iterator();
                int count = 0;
                while (iter.hasNext()){
                    Map.Entry entry = (Map.Entry)iter.next();
                    count+= ((DictionarySectionPrivate)entry.getValue()).getNumberOfElements();
                    if(LiteralsUtils.getType(str).equals((String)entry.getKey())){
                        count -= ((DictionarySectionPrivate)entry.getValue()).getNumberOfElements();
                        break;
                    }

                }
                return shared.getNumberOfElements() + count+id;
            }


            case PREDICATE:
            case SHARED:
                return id;
            default:
                throw new IllegalArgumentException();
        }
    }

    /*
    TODO: Change the objects part to look over the sections according to some pointer
     */
    protected long getLocalId(long id, TripleComponentRole position) {
        switch (position) {
            case SUBJECT:
                if(id <= shared.getNumberOfElements())
                    return id;
                else
                    return id-shared.getNumberOfElements();
            case OBJECT:
                if(id<=shared.getNumberOfElements()) {
                    return id;
                } else {
                    Iterator hmIterator = objects.entrySet().iterator();
                    // iterate over all subsections in the objects section
                    long count = 0;
                    while (hmIterator.hasNext()){
                        Map.Entry entry = (Map.Entry)hmIterator.next();
                        long numElts = 0;
                        if(entry.getValue() instanceof DictionarySectionPrivate)
                            numElts = ((DictionarySectionPrivate)entry.getValue()).getNumberOfElements();
                        else if(entry.getValue() instanceof PFCOptimizedExtractor)
                            numElts = ((PFCOptimizedExtractor)entry.getValue()).getNumStrings();
                        count+= numElts;
                        if(id <= shared.getNumberOfElements()+ count){
                            count -= numElts;
                            break;
                        }
                    }
                    // subtract the number of elements in the shared + the subsections in the objects section
                    return id - count - shared.getNumberOfElements();
                }
            case PREDICATE:
                return id;
            default:
                throw new IllegalArgumentException();
        }
    }
    /* (non-Javadoc)
     * @see hdt.dictionary.Dictionary#stringToId(java.lang.CharSequence, datatypes.TripleComponentRole)
     */
    @Override
    public long stringToId(CharSequence str, TripleComponentRole position) {
        str = DelayedString.unwrap(str);

        if(str==null || str.length()==0) {
            return 0;
        }

        if(str instanceof String) {
            // CompactString is more efficient for the binary search.
            str = new CompactString(str);
        }

        long ret=0;
        switch(position) {
            case SUBJECT:
                ret = shared.locate(str);
                if(ret!=0) {
                    return getGlobalId(ret, DictionarySectionRole.SHARED,str);
                }
                ret = subjects.locate(str);
                if(ret!=0) {
                    return getGlobalId(ret, DictionarySectionRole.SUBJECT,str);
                }
                return -1;
            case PREDICATE:
                ret = predicates.locate(str);
                if(ret!=0) {
                    return getGlobalId(ret, DictionarySectionRole.PREDICATE,str);
                }
                return -1;
            case OBJECT:
                if(str.charAt(0)!='"') {
                    ret = shared.locate(str);
                    if(ret!=0) {
                        return getGlobalId(ret, DictionarySectionRole.SHARED,str);
                    }
                }
                DictionarySectionPrivate subSection = getSubSection(str);
                if( subSection!= null)
                    ret = subSection.locate(new CompactString(LiteralsUtils.removeType(str)));
                else
                    return -1;
                if(ret!=0) {
                    return getGlobalId(ret, DictionarySectionRole.OBJECT,str);
                }
                return -1;
            default:
                throw new IllegalArgumentException();
        }
    }

    private long getNumberObjectsAllSections(){
        Iterator hmIterator = objects.entrySet().iterator();
        // iterate over all subsections in the objects section
        long total = 0;
        while (hmIterator.hasNext()){
            Map.Entry entry = (Map.Entry)hmIterator.next();
            DictionarySectionPrivate subSection = (DictionarySectionPrivate) entry.getValue();
            total += subSection.getNumberOfElements();
        }
        return total;
    }
    @Override
    public long getNumberOfElements() {

        return subjects.getNumberOfElements()+predicates.getNumberOfElements()+getNumberObjectsAllSections()+shared.getNumberOfElements();
    }

    @Override
    public long size() {
        return subjects.size()+predicates.size()+objects.size()+shared.size();
    }

    @Override
    public long getNsubjects() {
        return subjects.getNumberOfElements()+shared.getNumberOfElements();
    }

    @Override
    public long getNpredicates() {
        return predicates.getNumberOfElements();
    }

    @Override
    public long getNobjects() {
        return getNumberObjectsAllSections()+shared.getNumberOfElements();
    }

    @Override
    public long getNshared() {
        return shared.getNumberOfElements();
    }

    @Override
    public DictionarySection getSubjects() {
        return subjects;
    }

    @Override
    public DictionarySection getPredicates() {
        return predicates;
    }

    @Override
    public TreeMap<String, DictionarySection> getAllObjects() {
        return new TreeMap<>(this.objects);
    }

    @Override
    public DictionarySection getObjects() {
        return null;
    }

    @Override
    public DictionarySection getShared() {
        return shared;
    }

    private AbstractMap.SimpleEntry<String,DictionarySectionPrivate> getSection(long id, TripleComponentRole role) {
        switch (role) {
            case SUBJECT:
                if(id<=shared.getNumberOfElements()) {
                    return new AbstractMap.SimpleEntry<>("section",shared);
                } else {
                    return new AbstractMap.SimpleEntry<>("section",subjects);
                }
            case PREDICATE:
                return new AbstractMap.SimpleEntry<>("section",predicates);
            case OBJECT:
                if(id<=shared.getNumberOfElements()) {
                    return new AbstractMap.SimpleEntry<>("section",shared);
                } else {

                    Iterator hmIterator = objects.entrySet().iterator();
                    // iterate over all subsections in the objects section
                    DictionarySectionPrivate desiredSection = null;
                    String type = "";
                    int count = 0;
                    while (hmIterator.hasNext()){
                        Map.Entry entry = (Map.Entry)hmIterator.next();
                        DictionarySectionPrivate subSection = (DictionarySectionPrivate)entry.getValue();
                        count += subSection.getNumberOfElements();
                        if(id <= shared.getNumberOfElements()+ count){
                            desiredSection = subSection;
                            type = (String)entry.getKey();
                            break;
                        }
                    }
                    return new AbstractMap.SimpleEntry<>(type,desiredSection);
                }
            default:
                throw new IllegalArgumentException();
        }
    }
    static Pattern pattern = Pattern.compile("@[a-zA-Z0-9\\-]+$");
    /* (non-Javadoc)
     * @see hdt.dictionary.Dictionary#idToString(int, datatypes.TripleComponentRole)
     */
    @Override
    public CharSequence idToString(long id, TripleComponentRole role) {
        AbstractMap.SimpleEntry<String,DictionarySectionPrivate> section = getSection(id, role);
        long localId = getLocalId(id, role);
        if(section.getKey().equals("NO_DATATYPE") || section.getKey().equals("section"))
            return section.getValue().extract(localId);
        else {
            if(section.getValue() == null){
                // this should not happen, means that the given id wasn't found in any section
                System.out.println("Error couldn't find the section for the given ID: ["+id+"]");
                return null;
            }else {
                String label = section.getValue().extract(localId).toString();
                String dType = section.getKey();
                //Matcher matcher = pattern.matcher(label);
                if (LiteralsUtils.containsLanguage(label)) {
                    return label;
                } else {
                    return label + "^^" + dType;
                }
            }
        }
    }
    private DictionarySectionPrivate getSubSection(CharSequence str){
        String dataType = "";
//        if(str.toString().startsWith("\"")) {
//            if(str.toString().matches("\".*\"\\^\\^<.*>")){
//                dataType = str.toString().split("\\^")[2];
//            }else{
//                dataType = "NO_DATATYPE";
//            }
//        }else{
//            dataType = "NO_DATATYPE";
//        }
        dataType = LiteralsUtils.getType(str);
        return objects.get(dataType);
    }
    @Override
    public String dataTypeOfId(long id) {
        return getSection(id,TripleComponentRole.OBJECT).getKey();
    }
    public AbstractMap.SimpleEntry<Long,Long> getDataTypeRange(String dataType){
        if(!dataType.equals("NO_DATATYPE"))
            dataType = "<"+dataType+">";
        if(objects.containsKey(dataType)) { // literals subsection exist
            Iterator iter = objects.entrySet().iterator();
            int count = 0;
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                count += ((DictionarySectionPrivate) entry.getValue()).getNumberOfElements();
                if (dataType.equals((String) entry.getKey())) {
                    count -= ((DictionarySectionPrivate) entry.getValue()).getNumberOfElements();
                    break;
                }

            }
            long offset = shared.getNumberOfElements() + count;
            long size = offset + objects.get(dataType).getNumberOfElements();
            return new AbstractMap.SimpleEntry<>(offset +1, size);
        }
        return new AbstractMap.SimpleEntry<>(0L,0L);
    }
}
