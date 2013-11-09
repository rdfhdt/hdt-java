package org.rdfhdt.hdt.util.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * Emulates skip() using calls to read() for those underlying streams where skip() does not work correctly
 * 
 * @author mario.arias
 *
 */
public class SkipReadInputStream extends InputStream {
	
	private InputStream in;
	
	/**
	 * @param parent
	 */
	public SkipReadInputStream(InputStream input) {
		this.in = input;
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
		return in.read(b,off, len);
	}

	private static final int BUF_SIZE = 16384;
	private static byte [] buf = new byte[BUF_SIZE];		
	
	@Override
	public long skip(long n) throws IOException {
		// Read dummy instead of skip
		return in.read(buf, 0, (int) (n < BUF_SIZE ? n : BUF_SIZE));
	}
	
	@Override
	public void close() throws IOException {
		in.close();
	}
	
	@Override
	public void mark(int readlimit) {
		in.mark(readlimit);
	}
	
	@Override
	public boolean markSupported() {
		return in.markSupported();
	}
	
	@Override
	public void reset() throws IOException {
		in.reset();
	}

}
