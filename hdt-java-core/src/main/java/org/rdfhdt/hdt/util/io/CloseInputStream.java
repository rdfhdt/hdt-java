package org.rdfhdt.hdt.util.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * Propagates the close() event to another Closeable
 * 
 * @author Mario Arias
 *
 */
public class CloseInputStream extends InputStream {

	private InputStream in;
	private Closeable toClose;
	
	public CloseInputStream(InputStream in, Closeable toClose) {
		this.in = in;
		this.toClose = toClose;
	}
	
	@Override
	public int read() throws IOException {
		return in.read();
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {

		return in.read(b, off, len);
	}
	
	
	@Override
	public int read(byte[] b) throws IOException {

		return in.read(b);
	}
	
	@Override
	public long skip(long n) throws IOException {

		return in.skip(n);
	}
	
	@Override
	public void close() throws IOException {
		in.close();
		toClose.close();
	}

}
