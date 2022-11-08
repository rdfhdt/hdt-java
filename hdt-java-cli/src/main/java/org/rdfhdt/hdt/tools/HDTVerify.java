package org.rdfhdt.hdt.tools;

import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.util.string.ByteString;
import org.rdfhdt.hdt.util.string.CharSequenceComparator;
import org.rdfhdt.hdt.util.string.CompactString;
import org.rdfhdt.hdt.util.string.ReplazableString;

import java.util.Comparator;
import java.util.Iterator;

public class HDTVerify {

    private HDTVerify() {
    }

    private static void print(byte[] arr) {
        for (byte b : arr) {
            System.out.printf("%02X ", b);
        }
        System.out.println();
    }

    private static void print(CharSequence seq) {
        if (seq instanceof CompactString) {
            CompactString cs1 = (CompactString) seq;
            print(cs1.getData());
        }

        if (seq instanceof String) {
            String rs1 = (String) seq;
            print(rs1.getBytes());
        }
    }

    public static void checkDictionarySectionOrder(Iterator<? extends CharSequence> it) {
        ReplazableString prev = new ReplazableString();
        String lastStr = "";
        while (it.hasNext()) {
            ByteString charSeq = ByteString.of(it.next());
            String str = charSeq.toString();

            int cmp = prev.compareTo(charSeq);

            if (cmp >= 0) {
                System.out.println("ERRA: " + prev + " / " + charSeq);
            }

            int cmp2 = lastStr.compareTo(str);

            if (cmp2 >= 0) {
                System.out.println("ERRB: " + lastStr + " / " + str);
            }

            if (Math.signum(cmp) != Math.signum(cmp2)) {
                System.out.println("Not equal: " + cmp + " / " + cmp2);
                print(prev);
                print(charSeq);
                print(lastStr);
                print(str);
            }

            prev.replace(charSeq);
            lastStr = str;
        }
    }

    public static void main(String[] args) throws Throwable {
        if (args.length < 1) {
            System.out.println("hdtVerify <file.hdt>");
            System.exit(-1);
        }
        try (HDT hdt = HDTManager.mapHDT(args[0], null)) {
            System.out.println("Checking subject entries");
            checkDictionarySectionOrder(hdt.getDictionary().getSubjects().getSortedEntries());
            System.out.println("Checking predicate entries");
            checkDictionarySectionOrder(hdt.getDictionary().getPredicates().getSortedEntries());
            System.out.println("Checking object entries");
            checkDictionarySectionOrder(hdt.getDictionary().getObjects().getSortedEntries());
            System.out.println("Checking shared entries");
            checkDictionarySectionOrder(hdt.getDictionary().getShared().getSortedEntries());
        }
    }
}
