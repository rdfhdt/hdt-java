package org.rdfhdt.hdt.options;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Key {
	enum Type {
		STRING("String"), PATH("Path"), NUMBER("Number"), DOUBLE("Double"), BOOLEAN("Boolean"), ENUM("Enum");

		private final String title;

		Type(String title) {
			this.title = title;
		}

		public String getTitle() {
			return title;
		}
	}

	String desc() default  "";

	Type type() default Type.STRING;
}
