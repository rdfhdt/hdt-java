package org.rdfhdt.hdt.rdf.parsers;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.rdfhdt.hdt.util.io.CountInputStream;
import org.rdfhdt.hdt.util.io.NonCloseInputStream;

public class TarTest {

	public static void main(String[] args) throws Throwable {
				
		InputStream input = new CountInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream("/Users/mck/rdf/dataset/tgztest.tar.gz"))));
//		InputStream input = new CountInputStream(new BufferedInputStream(new FileInputStream("/Users/mck/rdf/dataset/tgztest.tar")));
		final TarArchiveInputStream debInputStream = new TarArchiveInputStream(input);
		TarArchiveEntry entry;
		
		NonCloseInputStream nonCloseIn = new NonCloseInputStream(debInputStream);

		while((entry = (TarArchiveEntry)debInputStream.getNextEntry()) != null) {
			System.out.println(entry.getName());

		}
		
		debInputStream.close();
	}

}
