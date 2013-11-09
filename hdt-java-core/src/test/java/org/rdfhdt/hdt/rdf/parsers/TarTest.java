package org.rdfhdt.hdt.rdf.parsers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.rdfhdt.hdt.util.io.ExternalDecompressStream;
import org.rdfhdt.hdt.util.io.NonCloseInputStream;

public class TarTest {

	public static void main(String[] args) throws Throwable {
				
		InputStream input = new ExternalDecompressStream(new File("/Users/mck/rdf/dataset/tgztest.tar.gz"), ExternalDecompressStream.GZIP);
//		InputStream input = new CountInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream("/Users/mck/rdf/dataset/tgztest.tar.gz"))));
//		InputStream input = new CountInputStream(new BufferedInputStream(new FileInputStream("/Users/mck/rdf/dataset/tgztest.tar")));
		final TarArchiveInputStream debInputStream = new TarArchiveInputStream(input);
		TarArchiveEntry entry = null;
		
		NonCloseInputStream nonCloseIn = new NonCloseInputStream(debInputStream);

		while((entry = (TarArchiveEntry)debInputStream.getNextEntry()) != null) {
			System.out.println(entry.getName());

		}
		
		debInputStream.close();
	}

}
