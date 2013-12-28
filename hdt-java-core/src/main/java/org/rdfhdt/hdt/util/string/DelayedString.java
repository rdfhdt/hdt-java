package org.rdfhdt.hdt.util.string;

public final class DelayedString implements CharSequence {
	CharSequence str;

	public DelayedString(CharSequence str) {
		this.str = str;
	}
	
	private void ensure() {
		if(!(str instanceof String)) {
			str = str.toString();
		}
	}
	
	@Override
	public int length() {
		ensure();
		return str.length();
	}

	@Override
	public char charAt(int index) {
		ensure();
		return str.charAt(index);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		ensure();
		return subSequence(start, end);
	}
	
	@Override
	public String toString() {
		ensure();
		return str.toString();
	}
	
	public CharSequence getInternal() {
		return str;
	}

}
