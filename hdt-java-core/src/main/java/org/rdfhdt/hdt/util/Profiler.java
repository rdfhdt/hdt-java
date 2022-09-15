package org.rdfhdt.hdt.util;

import java.util.ArrayList;
import java.util.List;

/**
 * tool to profile time
 * @author Antoine Willerval
 */
public class Profiler {
	private int maxSize = 0;
	private final String name;
	private Section mainSection;
	private boolean disabled;

	/**
	 * create a profiler
	 * @param name the profiler name
	 */
	public Profiler(String name) {
		this.name = name;
	}

	/**
	 * disable the profiler methods
	 * @param disable if true, the methods will be callable, but won't do anything
	 */
	public void setDisabled(boolean disable) {
		this.disabled = disable;
	}

	/**
	 * start a section
	 * @param name the section name
	 */
	public void pushSection(String name) {
		if (disabled) {
			return;
		}
		getMainSection().pushSection(name, 0);
	}

	/**
	 * complete a section
	 */
	public void popSection() {
		if (disabled) {
			return;
		}
		if (!getMainSection().isRunning()) {
			throw new IllegalArgumentException("profiler not running!");
		}
		getMainSection().popSection();
	}

	/**
	 * stop the profiler without poping sections
	 */
	public void stop() {
		if (disabled) {
			return;
		}
		getMainSection().stop();
	}

	/**
	 * write the profile into the console
	 */
	public void writeProfiling() {
		if (disabled) {
			return;
		}
		getMainSection().writeProfiling("", true);
	}

	/**
	 * @return the main section of the profiler
	 */
	public Section getMainSection() {
		if (this.mainSection == null) {
			this.mainSection = new Section(name);
		}
		return this.mainSection;
	}

	/**
	 * a section in the profiling
	 */
	public class Section {
		private final String name;
		private final long start = System.nanoTime();
		private long end = start;
		private final List<Section> subSections = new ArrayList<>();
		private Section currentSection;

		Section(String name) {
			this.name = name;
		}

		/**
		 * @return the subsections
		 */
		public List<Section> getSubSections() {
			return subSections;
		}

		/**
		 * @return the section name
		 */
		public String getName() {
			return name;
		}

		boolean isRunning() {
			return currentSection != null;
		}

		void pushSection(String name, int deep) {
			if (isRunning()) {
				currentSection.pushSection(name, deep + 1);
				return;
			}

			subSections.add(currentSection = new Section(name));
			maxSize = Math.max(name.length() + deep * 2, maxSize);
		}

		boolean popSection() {
			if (isRunning()) {
				if (currentSection.popSection()) {
					currentSection = null;
				}
				return false;
			} else {
				end = System.nanoTime();
				return true;
			}
		}

		void stop() {
			if (isRunning()) {
				currentSection.stop();
			}
			end = System.nanoTime();
		}

		void writeProfiling(String prefix, boolean isLast) {
			System.out.println(prefix + (getSubSections().isEmpty() ? "+--" : "+-+") + " [" + getName() + "] " + "-".repeat(1 + maxSize - getName().length()) + " elapsed=" + (end - start) / 1_000_000L + "ms");
			for (int i = 0; i < subSections.size(); i++) {
				Section s = subSections.get(i);
				s.writeProfiling(prefix + (isLast ? "  " : "| "), i == subSections.size() - 1);
			}
		}
	}
}
