package org.rdfhdt.hdt.tools;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Lists;
import org.rdfhdt.hdt.dictionary.DictionarySection;
import org.rdfhdt.hdt.dictionary.impl.MultipleBaseDictionary;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.util.listener.ColorTool;
import org.rdfhdt.hdt.util.string.ByteString;
import org.rdfhdt.hdt.util.string.CompactString;
import org.rdfhdt.hdt.util.string.ReplazableString;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HDTVerify {

    private HDTVerify() {
    }

    @Parameter(description = "<input HDT>")
    public List<String> parameters = Lists.newArrayList();

    @Parameter(names = "-unicode", description = "Ignore UNICODE order")
    public boolean unicode;

    @Parameter(names = "-color", description = "Print using color (if available)")
    public boolean color;

    @Parameter(names = "-binary", description = "Print binaries of the string in case of signum error")
    public boolean binary;

    @Parameter(names = "-quiet", description = "Do not show progress of the conversion")
    public boolean quiet;

    @Parameter(names = "-load", description = "Load the HDT in memory for faster results (might be impossible for large a HDT)")
    public boolean load;

    public ColorTool colorTool;

    private HDT loadOrMap(String file) throws IOException {
        return load ? HDTManager.loadHDT(file) : HDTManager.mapHDT(file);
    }

    private void print(byte[] arr) {
        for (byte b : arr) {
            System.out.printf("%02X ", b);
        }
        System.out.println();
    }

    private void print(CharSequence seq) {
        if (seq instanceof CompactString) {
            CompactString cs1 = (CompactString) seq;
            print(cs1.getData());
        }

        if (seq instanceof String) {
            String rs1 = (String) seq;
            print(rs1.getBytes());
        }
    }

    public boolean checkDictionarySectionOrder(Iterator<? extends CharSequence> it) {
        ReplazableString prev = new ReplazableString();
        String lastStr = "";
        boolean error = false;
        while (it.hasNext()) {
            ByteString charSeq = ByteString.of(it.next());
            String str = charSeq.toString();

            int cmp = prev.compareTo(charSeq);

            if (cmp >= 0) {
                error = true;
                if (cmp == 0) {
                    colorTool.error("Duplicated(bs)", prev + " == " + charSeq);
                } else {
                    colorTool.error("Bad order(bs)", prev + " > " + charSeq);
                }
            }

            if (!unicode) {
                int cmp2 = lastStr.compareTo(str);

                if (cmp2 >= 0) {
                    error = true;
                    if (cmp == 0) {
                        colorTool.error("Duplicated(str)", lastStr + " == " + str);
                    } else {
                        colorTool.error("Bad order(str)", lastStr + " > " + str);
                    }
                }

                if (Math.signum(cmp) != Math.signum(cmp2)) {
                    error = true;
                    colorTool.error("Not equal", cmp + " != " + cmp2 + " for " + lastStr + " / " + str);
                    if (binary) {
                        print(prev);
                        print(charSeq);
                        print(lastStr);
                        print(str);
                    }
                }

                lastStr = str;
            }

            prev.replace(charSeq);
        }
        if (error) {
            colorTool.warn("Not valid section");
        } else {
            colorTool.log("valid section");
        }
        return error;
    }

    public void exec() throws Throwable {
        try (HDT hdt = loadOrMap(parameters.get(0))) {
            boolean error;
            long count = 0;
            if (hdt.getDictionary() instanceof MultipleBaseDictionary) {
                colorTool.log("Checking subject entries");
                error = checkDictionarySectionOrder(hdt.getDictionary().getSubjects().getSortedEntries());
                count += hdt.getDictionary().getSubjects().getNumberOfElements();
                colorTool.log("Checking predicate entries");
                error |= checkDictionarySectionOrder(hdt.getDictionary().getPredicates().getSortedEntries());
                count += hdt.getDictionary().getPredicates().getNumberOfElements();
                colorTool.log("Checking object entries");
                Map<? extends CharSequence, DictionarySection> allObjects = hdt.getDictionary().getAllObjects();
                for (Map.Entry<? extends CharSequence, DictionarySection> entry : allObjects.entrySet()) {
                    CharSequence sectionName = entry.getKey();
                    DictionarySection section = entry.getValue();
                    colorTool.log("Checking object section " + sectionName);
                    error |= checkDictionarySectionOrder(section.getSortedEntries());
                    count += section.getNumberOfElements();
                }
                colorTool.log("Checking shared entries");
                error |= checkDictionarySectionOrder(hdt.getDictionary().getShared().getSortedEntries());
                count += hdt.getDictionary().getShared().getNumberOfElements();
            } else {
                colorTool.log("Checking subject entries");
                error = checkDictionarySectionOrder(hdt.getDictionary().getSubjects().getSortedEntries());
                count += hdt.getDictionary().getSubjects().getNumberOfElements();
                colorTool.log("Checking predicate entries");
                error |= checkDictionarySectionOrder(hdt.getDictionary().getPredicates().getSortedEntries());
                count += hdt.getDictionary().getPredicates().getNumberOfElements();
                colorTool.log("Checking object entries");
                error |= checkDictionarySectionOrder(hdt.getDictionary().getObjects().getSortedEntries());
                count += hdt.getDictionary().getObjects().getNumberOfElements();
                colorTool.log("Checking shared entries");
                error |= checkDictionarySectionOrder(hdt.getDictionary().getShared().getSortedEntries());
                count += hdt.getDictionary().getShared().getNumberOfElements();
            }

            if (error) {
                colorTool.error("This HDT isn't valid", true);
                System.exit(-1);
            } else {
                colorTool.log(count + " element(s) parsed");
                colorTool.log(colorTool.color(0, 5, 0) + "This HDT is valid", true);
            }
        }
    }

    public static void main(String[] args) throws Throwable {
        HDTVerify verify = new HDTVerify();
        JCommander com = new JCommander(verify);
        com.parse(args);
        verify.colorTool = new ColorTool(verify.color, verify.quiet);
        com.setProgramName("hdtVerify");
        if (verify.parameters.size() < 1) {
            com.usage();
            System.exit(-1);
        }
        verify.exec();
    }
}
