/**
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
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
 */

package org.rdfhdt.hdt.tools;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Lists;

import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.hdt.HDTVersion;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.util.StopWatch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author Dennis Diefenbach
 *
 */
public class HDTCat implements ProgressListener {

    public String hdtInput1;
    public String hdtInput2;
    public String hdtOutput;

    @Parameter(description = "<input HDT1> <input HDT2> <output HDT>")
    public List<String> parameters = Lists.newArrayList();

    @Parameter(names = "-options", description = "HDT Conversion options (override those of config file)")
    public String options;

    @Parameter(names = "-config", description = "Conversion config file")
    public String configFile;

    @Parameter(names = "-index", description = "Generate also external indices to solve all queries")
    public boolean generateIndex;

    @Parameter(names = "-version", description = "Prints the HDT version number")
    public static boolean showVersion;

    @Parameter(names = "-quiet", description = "Do not show progress of the conversion")
    public boolean quiet;

    public void execute() throws ParserException, IOException {

        HDTSpecification spec;
        if(configFile!=null) {
            spec = new HDTSpecification(configFile);
        } else {
            spec = new HDTSpecification();
        }
        if(options!=null) {
            spec.setOptions(options);
        }

        File file = new File(hdtOutput);
        File theDir = new File(file.getAbsolutePath()+"_tmp");
        theDir.mkdirs();
        String location = theDir.getAbsolutePath()+"/";
        HDT hdt = HDTManager.catHDT(location,hdtInput1, hdtInput2 , spec,this);


        try {
            // Show Basic stats
            if(!quiet){
                System.out.println("Total Triples: "+hdt.getTriples().getNumberOfElements());
                System.out.println("Different subjects: "+hdt.getDictionary().getNsubjects());
                System.out.println("Different predicates: "+hdt.getDictionary().getNpredicates());
                System.out.println("Different objects: "+hdt.getDictionary().getNobjects());
                System.out.println("Common Subject/Object:"+hdt.getDictionary().getNshared());
            }

            // Dump to HDT file
            StopWatch sw = new StopWatch();
            hdt.saveToHDT(hdtOutput, this);
            System.out.println("HDT saved to file in: "+sw.stopAndShow());
            Files.delete(Paths.get(location+"dictionary"));
            Files.delete(Paths.get(location+"triples"));
            theDir.delete();


            // Generate index and dump it to .hdt.index file
            sw.reset();
            if(generateIndex) {
                hdt = HDTManager.indexedHDT(hdt,this);
                System.out.println("Index generated and saved in: "+sw.stopAndShow());
            }
        } finally {
            if(hdt!=null) hdt.close();
        }

        // Debug all inserted triples
        //HdtSearch.iterate(hdt, "","","");
    }

    /* (non-Javadoc)
     * @see hdt.ProgressListener#notifyProgress(float, java.lang.String)
     */
    @Override
    public void notifyProgress(float level, String message) {
        if(!quiet) {
            System.out.print("\r"+message + "\t"+ Float.toString(level)+"                            \r");
        }
    }

    public static void main(String[] args) throws Throwable {
        HDTCat hdtCat = new HDTCat();
        System.out.println("Welcome to hdtCat!");
        System.out.println("This tool was developed by Dennis Diefenbach and Jośe M. Giḿenez-Garćıa");
        System.out.println("NOTE: this tool is not working under WINDOWS! This is a well-known BUG!");
        JCommander com = new JCommander(hdtCat, args);
        com.setProgramName("hdtCat");

        if(hdtCat.parameters.size()==3) {
            hdtCat.hdtInput1 = hdtCat.parameters.get(0);
            hdtCat.hdtInput2 = hdtCat.parameters.get(1);
            hdtCat.hdtOutput = hdtCat.parameters.get(2);
        } else if (showVersion){
            System.out.println(HDTVersion.get_version_string("."));
            System.exit(0);
        }
        else{
            com.usage();
            System.exit(1);
        }

        System.out.println("Cat "+ hdtCat.hdtInput1+" and "+ hdtCat.hdtInput2+" to "+ hdtCat.hdtOutput);

        hdtCat.execute();
    }
}