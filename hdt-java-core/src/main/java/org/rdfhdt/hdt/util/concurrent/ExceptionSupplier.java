package org.rdfhdt.hdt.util.concurrent;

@FunctionalInterface
public interface ExceptionSupplier<T, E extends Throwable> {
	T get() throws E;
}
