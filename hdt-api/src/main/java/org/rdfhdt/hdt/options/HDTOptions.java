/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/iface/org/rdfhdt/hdt/options/HDTOptions.java $
 * Revision: $Rev: 191 $
 * Last modified: $Date: 2013-03-03 11:41:43 +0000 (dom, 03 mar 2013) $
 * Last modified by: $Author: mario.arias $
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Contacting the authors:
 *   Mario Arias:               mario.arias@deri.org
 *   Javier D. Fernandez:       jfergar@infor.uva.es
 *   Miguel A. Martinez-Prieto: migumar2@infor.uva.es
 *   Alejandro Andres:          fuzzy.alej@gmail.com
 */

package org.rdfhdt.hdt.options;

import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.rdf.RDFFluxStop;
import org.rdfhdt.hdt.util.Profiler;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Options storage, see {@link org.rdfhdt.hdt.options.HDTOptionsKeys} for more information.
 * @author mario.arias
 */
public interface HDTOptions {
	/**
	 * empty option, can be used to set values
	 */
	HDTOptions EMPTY = new HDTOptions() {
		@Override
		public void clear() {
			// already empty
		}

		@Override
		public String get(String key) {
			// no value for key
			return null;
		}

		@Override
		public void set(String key, String value) {
			throw new NotImplementedException("set");
		}
	};

	/**
	 * @return create empty, modifiable options
	 */
	static HDTOptions of() {
		return of(Map.of());
	}

	/**
	 * create modifiable options starting from the copy of the data map
	 * @param data data map
	 * @return options
	 */
	static HDTOptions of(Map<String, String> data) {
		Map<String, String> map = new HashMap<>(data);
		return new HDTOptions() {
			@Override
			public void clear() {
				map.clear();
			}

			@Override
			public String get(String key) {
				return map.get(key);
			}

			@Override
			public void set(String key, String value) {
				map.put(key, value);
			}
		};
	}


	/**
	 * clear all the options
	 */
	void clear();

	/**
	 * get an option value
	 *
	 * @param key key
	 * @return value or null if not defined
	 */
	String get(String key);

	default Set<Object> getKeys() {
		throw new NotImplementedException();
	}

	/**
	 * get a value
	 *
	 * @param key          key
	 * @param defaultValue default value
	 * @return value or defaultValue if the value isn't defined
	 */
	default String get(String key, String defaultValue) {
		return Objects.requireNonNullElse(get(key), defaultValue);
	}

	/**
	 * get a value
	 *
	 * @param key          key
	 * @param defaultValue default value
	 * @return value or defaultValue if the value isn't defined
	 */
	default String get(String key, Supplier<String> defaultValue) {
		return Objects.requireNonNullElseGet(get(key), defaultValue);
	}

	/**
	 * get a boolean
	 *
	 * @param key key
	 * @return boolean or false if the value isn't defined
	 */
	default boolean getBoolean(String key) {
		return "true".equalsIgnoreCase(get(key));
	}
	/**
	 * get a boolean
	 *
	 * @param key key
	 * @param defaultValue default value
	 * @return boolean or false if the value isn't defined
	 */
	default boolean getBoolean(String key, boolean defaultValue) {
		String v = get(key);
		if (v == null) {
			return defaultValue;
		}
		return "true".equalsIgnoreCase(v);
	}

	/**
	 * get a double
	 *
	 * @param key key
	 * @return double or 0 if the value isn't defined
	 */
	default double getDouble(String key) {
		return getDouble(key, 0);
	}

	/**
	 * get a double
	 *
	 * @param key          key
	 * @param defaultValue default value
	 * @return double or defaultValue if the value isn't defined
	 */
	default double getDouble(String key, DoubleSupplier defaultValue) {
		String l = get(key);
		if (l == null) {
			return defaultValue.getAsDouble();
		}
		return Double.parseDouble(l);
	}

	/**
	 * get a double
	 *
	 * @param key          key
	 * @param defaultValue default value
	 * @return double or defaultValue if the value isn't defined
	 */
	default double getDouble(String key, double defaultValue) {
		return getDouble(key, () -> defaultValue);
	}

	/**
	 * get an {@link org.rdfhdt.hdt.rdf.RDFFluxStop}
	 *
	 * @param key key
	 * @return RDFFluxStop or false if the value isn't defined
	 */
	default RDFFluxStop getFluxStop(String key) {
		return RDFFluxStop.readConfig(get(key));
	}

	/**
	 * get an {@link org.rdfhdt.hdt.rdf.RDFFluxStop}
	 *
	 * @param key          key
	 * @param defaultValue default value
	 * @return RDFFluxStop or defaultValue if the value isn't defined
	 */
	default RDFFluxStop getFluxStop(String key, Supplier<RDFFluxStop> defaultValue) {
		return Objects.requireNonNullElseGet(getFluxStop(key), defaultValue);
	}

	/**
	 * get an {@link org.rdfhdt.hdt.rdf.RDFFluxStop}
	 *
	 * @param key          key
	 * @param defaultValue default value
	 * @return RDFFluxStop or defaultValue if the value isn't defined
	 */
	default RDFFluxStop getFluxStop(String key, RDFFluxStop defaultValue) {
		return getFluxStop(key, () -> defaultValue);
	}

	/**
	 * get a long value
	 *
	 * @param key key
	 * @return value or 0 if not defined
	 */
	default long getInt(String key) {
		return getInt(key, 0);
	}

	/**
	 * get a long
	 *
	 * @param key          key
	 * @param defaultValue default value
	 * @return long or defaultValue if the value isn't defined
	 */
	default long getInt(String key, LongSupplier defaultValue) {
		String l = get(key);
		if (l == null) {
			return defaultValue.getAsLong();
		}
		return Long.parseLong(l);
	}

	/**
	 * get a long
	 *
	 * @param key          key
	 * @param defaultValue default value
	 * @return long or defaultValue if the value isn't defined
	 */
	default long getInt(String key, long defaultValue) {
		return getInt(key, () -> defaultValue);
	}

	/**
	 * set an option value
	 *
	 * @param key   key
	 * @param value value
	 */
	void set(String key, String value);

	/**
	 * set a value, same as using {@link String#valueOf(Object)} with {@link #set(String, String)}
	 *
	 * @param key   key
	 * @param value value
	 */
	default void set(String key, Object value) {
		set(key, String.valueOf(value));
	}

	/**
	 * set a flux stop value, same as using {@link #set(String, String)} with {@link org.rdfhdt.hdt.rdf.RDFFluxStop#asConfig()}
	 *
	 * @param key      key
	 * @param fluxStop value
	 */
	default void set(String key, RDFFluxStop fluxStop) {
		set(key, fluxStop.asConfig());
	}

	/**
	 * set a profiler id
	 * @param key key
	 * @param profiler profiler
	 */
	default void set(String key, Profiler profiler) {
		set(key, "!" + profiler.getId());
	}

	/**
	 * set a long value
	 *
	 * @param key   key
	 * @param value value
	 */
	default void setInt(String key, long value) {
		set(key, String.valueOf(value));
	}

	/**
	 * read an option config, format: (key=value)?(;key=value)*
	 *
	 * @param options options
	 */
	default void setOptions(String options) {
		for (String item : options.split(";")) {
			int pos = item.indexOf('=');
			if (pos != -1) {
				String property = item.substring(0, pos);
				String value = item.substring(pos+1);
				set(property, value);
			}
		}
	}
}
