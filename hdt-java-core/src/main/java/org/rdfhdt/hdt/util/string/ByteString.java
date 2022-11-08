package org.rdfhdt.hdt.util.string;

public interface ByteString extends CharSequence, Comparable<ByteString> {
	int UTF8_BIG = 2;
	static ByteString of(CharSequence sec) {
		return ByteStringUtil.asByteString(sec);
	}

	byte[] getBuffer();

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	default int compareTo(ByteString other) {
		int n = Math.min(length(), other.length());
		byte[] buffer1 = getBuffer();
		byte[] buffer2 = other.getBuffer();

		int k = 0;
		while (k < n) {
			byte c1 = buffer1[k];
			byte c2 = buffer2[k];
			if (c1 != c2) {
				return (c1 & 0xFF) - (c2 & 0xFF);
			}
			k++;
		}
		return  length() - other.length();
	}
}
