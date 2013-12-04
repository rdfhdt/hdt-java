package org.rdfhdt.hdt.fuseki;

import java.io.IOException;

import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.listener.ProgressListener;

public class HDTGenerateIndex {
	public static void main(String[] args) {
		try {
			if(args.length!=1) {
				System.out.println("HDTGenerateIndex <hdtFile>");
				System.exit(-1);
			}
			System.out.println("Generating "+args[0]+".index");
			HDTManager.mapIndexedHDT(args[0], new ProgressListener() {
				@Override
				public void notifyProgress(float level, String message) {
					System.out.println(message + "\t"+ Float.toString(level));
				}
			}).close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		};
	}
}
