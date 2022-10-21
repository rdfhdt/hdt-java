package org.rdfhdt.hdt.options;

import java.util.Objects;
import java.util.function.Function;

/**
 * {@link HDTOptions} wrapper to redirect a key to another key
 *
 * @author Antoine Willerval
 */
public class HideHDTOptions implements HDTOptions {
    private final HDTOptions spec;
    private final Function<String, String> mapper;

    /**
     * @param spec   wrapped options
     * @param mapper mapping function (key) -> newKey?
     */
    public HideHDTOptions(HDTOptions spec, Function<String, String> mapper) {
        this.spec = spec;
        this.mapper = mapper;
    }

    private String map(String key) {
        return Objects.requireNonNullElse(mapper.apply(key), "");
    }

    @Override
    public String get(String key) {
        return spec.get(map(key));
    }

    @Override
    public void set(String key, String value) {
        spec.set(map(key), value);
    }

    @Override
    public void setOptions(String options) {
        spec.setOptions(options);
    }

    @Override
    public long getInt(String key) {
        return spec.getInt(map(key));
    }

    @Override
    public void setInt(String key, long value) {
        spec.setInt(map(key), value);
    }

    @Override
    public void clear() {
        spec.clear();
    }
}
