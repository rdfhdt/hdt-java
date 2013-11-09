package org.rdfhdt.hdt.exceptions;


public class IllegalFormatException extends RuntimeException {

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
