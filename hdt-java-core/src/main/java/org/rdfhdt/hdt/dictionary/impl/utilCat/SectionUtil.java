package org.rdfhdt.hdt.dictionary.impl.utilCat;

import org.rdfhdt.hdt.compact.integer.VByte;
import org.rdfhdt.hdt.compact.sequence.SequenceLog64BigDisk;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.util.crc.CRC32;
import org.rdfhdt.hdt.util.crc.CRC8;
import org.rdfhdt.hdt.util.crc.CRCOutputStream;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.listener.ListenerUtil;
import org.rdfhdt.hdt.util.string.ByteStringUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class SectionUtil {

    private static final int DEFAULT_BLOCK_SIZE = 16;
    private static final int BLOCK_PER_BUFFER = 1000000;

    public static void createSection(String location,long numEntries, int type, CatUnion itAdd ,
                                     CatUnion itSkip , HashMap<String,CatMapping> mappings,long offset, ProgressListener listener)  throws IOException {
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
        long numberElements = 0;
        SequenceLog64BigDisk blocks;
        ByteArrayOutputStream byteOut;
        try (CRCOutputStream outBuffer = new CRCOutputStream(new FileOutputStream(location+"section_buffer_"+type), new CRC32())) {
            blocks = new SequenceLog64BigDisk(location + "SequenceLog64BigDisk" + type, 64, numEntries / 16);
            byteOut = new ByteArrayOutputStream(16 * 1024);
            if (numEntries > 0) {
                CharSequence previousStr = null;

                CatElement skipElement = null;
                if (itSkip.hasNext()) {
                    skipElement = itSkip.next();
                }
                while (itAdd.hasNext()) {
                    ListenerUtil.notifyCond(listener, "Analyze section " + name + " ", numberElements, numberElements, numEntries);
                    CatElement nextElement = itAdd.next();

                    if (skipElement != null && nextElement.entity.toString().equals(skipElement.entity.toString())) {
                        if (itSkip.hasNext()) {
                            skipElement = itSkip.next();
                        } else {
                            skipElement = null;
                        }
                    } else {
                        for (int i = 0; i < nextElement.IDs.size(); i++) {
                            long id = nextElement.IDs.get(i).pos;
                            String iter = nextElement.IDs.get(i).iter.toString();
                            if (iter.equals("shared"))
                                mappings.get(iter).set(id - 1, offset + numberElements + 1, type);
                            else
                                mappings.get(iter).set(id - 1, numberElements + 1, type);
                        }
                        String str = nextElement.entity.toString();
                        if (numberElements % DEFAULT_BLOCK_SIZE == 0) {
                            blocks.append(storedBuffersSize + byteOut.size());
                            numBlocks++;

                            // if a buffer is filled, flush the byteOut and store it
                            if (((numBlocks - 1) % BLOCK_PER_BUFFER == 0) && ((numBlocks - 1) / BLOCK_PER_BUFFER != 0) || byteOut.size() > 200000) {
                                storedBuffersSize += byteOut.size();
                                byteOut.flush();
                                byte[] arr = byteOut.toByteArray();
                                IOUtil.writeBuffer(outBuffer, arr, 0, arr.length, null);
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
            blocks.append(storedBuffersSize + byteOut.size());
            // Trim text/blocks
            blocks.aggressiveTrimToSize();
            byteOut.flush();
            //section.addBuffer(buffer, byteOut.toByteArray());
            byte[] arr = byteOut.toByteArray();
            IOUtil.writeBuffer(outBuffer, arr, 0, arr.length, null);
            outBuffer.writeCRC();
        }
        //Save the section conforming to the HDT format
        try (CRCOutputStream out = new CRCOutputStream(new FileOutputStream(location + "section" + type), new CRC8())) {
            //write the index type
            out.write(2);
            //write the number of strings
            VByte.encode(out, numberElements);
            //write the datasize
            VByte.encode(out, storedBuffersSize + byteOut.size());
            //wirte the blocksize
            VByte.encode(out, DEFAULT_BLOCK_SIZE);
            //write CRC
            out.writeCRC();
            //write the blocks
            blocks.save(out, null);    // Write blocks directly to output, they have their own CRC check.
            blocks.close();
            //write out_buffer
            Files.copy(Path.of(location + "section_buffer_" + type), out);
        }

        Files.deleteIfExists(Paths.get(location + "section_buffer_" + type));
        Files.deleteIfExists(Paths.get(location + "SequenceLog64BigDisk" + type));
    }
}
