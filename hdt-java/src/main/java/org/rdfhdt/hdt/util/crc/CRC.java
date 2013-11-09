package org.rdfhdt.hdt.util.crc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface CRC extends Comparable<CRC> {
	/** Update this CRC with the content of the buffer, from offset, using length bytes. */
	public void update(byte []buffer, int offset, int length);
	
	/** Update the CRC with the specified byte */
	public void update(byte data);
	
	/** Write this CRC to an Output Stream */
	public void writeCRC(OutputStream out) throws IOException;
	
	/** Read CRC from InputStream and compare it to this. 
	 * 
	 * @param in InputStream
	 * @return true if the checksum is the same, false if checksum error.
	 * @throws IOException
	 */
	public boolean readAndCheck(InputStream in) throws IOException;
	
	/**
	 * Get checksum value.
	 * @return
	 */
	public long getValue();
	
	/**
	 * Reset the checksum to the initial value.
	 */
	public void reset();
}
