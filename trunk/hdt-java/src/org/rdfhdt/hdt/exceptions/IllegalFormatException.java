package org.rdfhdt.hdt.exceptions;

import java.io.IOException;

public class IllegalFormatException extends IOException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5375371760409400495L;

	public IllegalFormatException() {
		super();
	}
	
	public IllegalFormatException(String reason) {
		super(reason);
	}
}
