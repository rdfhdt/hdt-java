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
import java.nio.file.Paths;
import java.util.HashMap;

public class SectionUtil {


    private static int DEFAULT_BLOCK_SIZE = 16;
    private static int BLOCK_PER_BUFFER = 1000000;

    public static void createSection(String location,long numEntries, int type, CatUnion itAdd ,
                                     CatUnion itSkip , HashMap<String,CatMapping> mappings,long offset, ProgressListener listener) {
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
                            if(iter.equals("shared"))
                                mappings.get(iter).set(id - 1, offset+numberElements + 1, type);
                            else
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
                            if (((numBlocks - 1) % BLOCK_PER_BUFFER == 0) && ((numBlocks - 1) / BLOCK_PER_BUFFER != 0) || byteOut.size()>200000) {
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
            try {
                Files.delete(Paths.get(location + "section_buffer_" + type));
                Files.delete(Paths.get(location + "SequenceLog64BigDisk" + type));
            } catch (Exception e) {
                // swallow this exception intentionally. See javadoc on Files.delete for details.
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
