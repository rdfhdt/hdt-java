package org.rdfhdt.hdt.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

/**
 * On the close() event, call a Callable<Void>
 * 
 * @author Mario Arias
 *
 */
public class OnCloseInputStream extends InputStream {

	private InputStream in;
	private Callable<Void> toCall;
	
	public OnCloseInputStream(InputStream in, Callable<Void> toCall) {
		this.in = in;
		this.toCall = toCall;
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
		try {
			in.close();
		} finally {
			try {
				toCall.call();
			} catch (Exception e) {
				e.printStackTrace();
				throw new IOException(e);
			}
		}
	}

}
