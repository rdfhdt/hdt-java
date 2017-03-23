package org.rdfhdt.hdt.hdt.writer;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.GZIPOutputStream;

import org.rdfhdt.hdt.rdf.TripleWriter;
import org.rdfhdt.hdt.triples.TripleString;

public class TripleWriterNtriples implements TripleWriter {

	private Writer out;
	private boolean close=false;
	
	public TripleWriterNtriples(String outFile, boolean compress) throws IOException {
		if(compress) {
			this.out = new OutputStreamWriter(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(outFile))));
		} else {
			this.out = new BufferedWriter(new FileWriter(outFile));
		}
		close = true;
	}
	
	public TripleWriterNtriples(OutputStream out) {
		this.out = new BufferedWriter(new OutputStreamWriter(out));
	}
	
	@Override
	public void addTriple(TripleString str) throws IOException {
		str.dumpNtriple(out);
	}

	@Override
	public void close() throws IOException {
		if(close) {
			out.close();
		} else {
			out.flush();
		}
	}

}
