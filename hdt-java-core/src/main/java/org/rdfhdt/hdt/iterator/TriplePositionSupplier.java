package org.rdfhdt.hdt.iterator;

@FunctionalInterface
public interface TriplePositionSupplier {
    /**
     * create a simple supplier with a static value
     * @param value the value to supply
     * @return BufferedTriplePosition
     */
    static TriplePositionSupplier of(long value) {
        return () -> value;
    }
    /**
     * compute the position
     * @return position
     */
    long compute();
}
