package org.rdfhdt.hdt.util;

import org.rdfhdt.hdt.compact.integer.VByte;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTOptionsKeys;
import org.rdfhdt.hdt.util.crc.CRC32;
import org.rdfhdt.hdt.util.crc.CRCInputStream;
import org.rdfhdt.hdt.util.crc.CRCOutputStream;
import org.rdfhdt.hdt.util.io.IOUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * tool to profile time
 *
 * @author Antoine Willerval
 */
public class Profiler {
	/**
	 * Read the profiling values from an input path
	 *
	 * @param inputPath input path
	 * @throws java.io.IOException                reading exception
	 * @throws java.lang.IllegalArgumentException if the file's CRC doesn't match
	 */
	public static Profiler readFromDisk(Path inputPath) throws IOException {
		Profiler p = new Profiler("");
		try (CRCInputStream is = new CRCInputStream(new BufferedInputStream(Files.newInputStream(inputPath)), new CRC32())) {
			for (byte b : HEADER) {
				if (is.read() != b) {
					throw new IOException("Missing header for the profiling file!");
				}
			}
			p.mainSection = p.new Section(is);
			if (!is.readCRCAndCheck()) {
				throw new IllegalArgumentException("CRC doesn't match when reading the CRC!");
			}
		}
		return p;
	}

	private static final byte[] HEADER = {'H', 'D', 'T', 'P', 'R', 'O', 'F', 'I', 'L', 'E'};
	private int maxSize = 0;
	private final String name;
	private Section mainSection;
	private boolean disabled;
	private Path outputPath;

	/**
	 * create a profiler
	 *
	 * @param name the profiler name
	 */
	public Profiler(String name) {
		this(name, null);
	}

	/**
	 * create a profiler from specifications
	 *
	 * @param name profiler name
	 * @param spec spec (nullable)
	 */
	public Profiler(String name, HDTOptions spec) {
		this.name = Objects.requireNonNull(name, "name can't be null!");
		if (spec != null) {
			disabled = !"true".equalsIgnoreCase(spec.get(HDTOptionsKeys.PROFILER_KEY));
			String profilerOutputLocation = spec.get(HDTOptionsKeys.PROFILER_OUTPUT_KEY);
			if (profilerOutputLocation != null && !profilerOutputLocation.isEmpty()) {
				outputPath = Path.of(profilerOutputLocation);
			}
		}
	}

	/**
	 * disable the profiler methods
	 *
	 * @param disable if true, the methods will be callable, but won't do anything
	 */
	public void setDisabled(boolean disable) {
		this.disabled = disable;
	}

	/**
	 * start a section
	 *
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
	 * reset the profiler
	 */
	public void reset() {
		mainSection = null;
	}

	/**
	 * write the profile into the console
	 */
	public void writeProfiling() throws IOException {
		if (disabled) {
			return;
		}
		getMainSection().writeProfiling("", true);
		if (outputPath != null) {
			writeToDisk(outputPath);
		}
	}

	/**
	 * Write the profiling values into the output path
	 *
	 * @param outputPath output path
	 */
	public void writeToDisk(Path outputPath) throws IOException {
		try (CRCOutputStream os = new CRCOutputStream(new BufferedOutputStream(Files.newOutputStream(outputPath)), new CRC32())) {
			for (byte b : HEADER) {
				os.write(b);
			}
			getMainSection().writeSection(os);
			os.writeCRC();
		}
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
		private final long start;
		private long end;
		private final List<Section> subSections;
		private transient Section currentSection;

		Section(String name) {
			this.name = name;
			start = System.nanoTime();
			end = start;
			subSections = new ArrayList<>();
		}

		/**
		 * read the section from the input stream
		 *
		 * @param is input stream
		 * @throws IOException io exception
		 */
		Section(InputStream is) throws IOException {
			start = VByte.decode(is);
			end = VByte.decode(is);

			int nameLength = (int) VByte.decode(is);
			byte[] nameBytes = IOUtil.readBuffer(is, nameLength, null);
			name = new String(nameBytes, StandardCharsets.UTF_8);

			int subSize = (int) VByte.decode(is);
			subSections = new ArrayList<>(subSize);
			for (int i = 0; i < subSize; i++) {
				subSections.add(new Section(is));
			}
		}

		void writeSection(OutputStream os) throws IOException {
			byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);

			VByte.encode(os, start);
			VByte.encode(os, end);

			VByte.encode(os, nameBytes.length);
			os.write(nameBytes);

			List<Section> sub = getSubSections();
			VByte.encode(os, sub.size());

			for (Section s : sub) {
				s.writeSection(os);
			}
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

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			Section section = (Section) o;

			return start == section.start
					&& end == section.end
					&& name.equals(section.name)
					&& subSections.equals(section.subSections);
		}

		@Override
		public int hashCode() {
			int result = name.hashCode();
			result = 31 * result + (int) (start ^ (start >>> 32));
			result = 31 * result + (int) (end ^ (end >>> 32));
			result = 31 * result + subSections.hashCode();
			return result;
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
