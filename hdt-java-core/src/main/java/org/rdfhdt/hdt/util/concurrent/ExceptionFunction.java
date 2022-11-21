package org.rdfhdt.hdt.util.concurrent;

@FunctionalInterface
public interface ExceptionFunction<I, O, E extends Throwable> {
	O apply(I value) throws E;
}
