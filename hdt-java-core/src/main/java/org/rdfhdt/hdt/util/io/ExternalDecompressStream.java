package org.rdfhdt.hdt.util.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ExternalDecompressStream extends InputStream {

	private InputStream in;
	private Process process;
	
	// GZIP
	public static final String [] GZIP = { "gzip", "-c", "-d"};
	public static final String [] PIGZ = { "pigz", "-c", "-d"};
	
	// BZIP2
	public static final String [] BZIP2 = { "bzip2", "-c", "-d" };
	public static final String [] PBZIP2 = { "pbzip2", "-c", "-d" };
	
	// SNAPPY
	public static final String [] SNZIP = { "snzip", "-c", "-d"};
	
	public ExternalDecompressStream(File inFile, String [] cmd) throws IOException {
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.redirectInput(inFile);
		process = pb.start();
		in = process.getInputStream();
	}
	
	@Override
	public int read() throws IOException {
		return in.read();
	}
	
	@Override
	public int read(byte[] b) throws IOException {
		return in.read(b);
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return in.read(b, off, len);
	}
	
	@Override
	public long skip(long n) throws IOException {
//		return in.skip(n);
		
		// FIXME: ProcessBuilder crashes on skip, we read instead.
		byte [] buf = new byte[(int) (n < 8192 ? n : 8192)];		
		return in.read(buf);
	}
	
	@Override
	public int available() throws IOException {
		return in.available();
	}
	
	@Override
	public void close() throws IOException {
		in.close();
		try {
			process.waitFor();
			
		    process.getOutputStream().close();
		    process.getErrorStream().close(); 
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


}
