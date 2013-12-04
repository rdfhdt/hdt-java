package org.rdfhdt.hdt.fuseki;

import java.io.IOException;

import org.rdfhdt.hdt.hdt.HDTManager;

public class HDTGenerateIndex {
	public static void main(String[] args) {
		try {
			if(args.length!=1) {
				System.out.println("HDTGenerateIndex <hdtFile>");
				System.exit(-1);
			}
			
			HDTManager.mapIndexedHDT(args[0], null).close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		};
	}
}
