package org.rdfhdt.hdt.util.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * a file that delete itself when we close it
 */
public class CloseSuppressPath implements Path, Closeable {
	public static final int BUFFER_SIZE = 1 << 13;
	private final Path wrapper;
	private boolean isDir;

	CloseSuppressPath(Path wrapper) {
		this.wrapper = wrapper;
	}

	public static CloseSuppressPath of(String first, String... more) {
		return new CloseSuppressPath(Path.of(first, more));
	}

	public static CloseSuppressPath of(Path component) {
		return component instanceof CloseSuppressPath ? (CloseSuppressPath) component : new CloseSuppressPath(component);
	}

	@Override
	public FileSystem getFileSystem() {
		return wrapper.getFileSystem();
	}

	@Override
	public boolean isAbsolute() {
		return wrapper.isAbsolute();
	}

	@Override
	public Path getRoot() {
		return wrapper.getRoot();
	}

	@Override
	public Path getFileName() {
		return wrapper.getFileName();
	}

	@Override
	public Path getParent() {
		return wrapper.getParent();
	}

	@Override
	public int getNameCount() {
		return wrapper.getNameCount();
	}

	@Override
	public Path getName(int index) {
		return wrapper.getName(index);
	}

	@Override
	public Path subpath(int beginIndex, int endIndex) {
		return wrapper.subpath(beginIndex, endIndex);
	}

	@Override
	public boolean startsWith(Path other) {
		return wrapper.startsWith(other);
	}

	@Override
	public boolean startsWith(String other) {
		return wrapper.startsWith(other);
	}

	@Override
	public boolean endsWith(Path other) {
		return wrapper.endsWith(other);
	}

	@Override
	public boolean endsWith(String other) {
		return wrapper.endsWith(other);
	}

	@Override
	public Path normalize() {
		return wrapper.normalize();
	}

	@Override
	public CloseSuppressPath resolve(Path other) {
		return of(wrapper.resolve(other));
	}

	@Override
	public CloseSuppressPath resolve(String other) {
		return of(wrapper.resolve(other));
	}

	@Override
	public CloseSuppressPath resolveSibling(Path other) {
		return of(wrapper.resolveSibling(other));
	}

	@Override
	public CloseSuppressPath resolveSibling(String other) {
		return of(wrapper.resolveSibling(other));
	}

	@Override
	public CloseSuppressPath relativize(Path other) {
		return of(wrapper.relativize(other));
	}

	@Override
	public URI toUri() {
		return wrapper.toUri();
	}

	@Override
	public Path toAbsolutePath() {
		return wrapper.toAbsolutePath();
	}

	@Override
	public Path toRealPath(LinkOption... options) throws IOException {
		return wrapper.toRealPath(options);
	}

	@Override
	public File toFile() {
		return wrapper.toFile();
	}

	@Override
	public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
		return wrapper.register(watcher, events, modifiers);
	}

	@Override
	public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
		return wrapper.register(watcher, events);
	}

	@Override
	public Iterator<Path> iterator() {
		return wrapper.iterator();
	}

	@Override
	public int compareTo(Path other) {
		return wrapper.compareTo(other);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof CloseSuppressPath) {
			return wrapper.equals(((CloseSuppressPath) other).wrapper);
		}
		return wrapper.equals(other);
	}

	@Override
	public int hashCode() {
		return wrapper.hashCode();
	}

	@Override
	public String toString() {
		return wrapper.toString();
	}

	@Override
	public void forEach(Consumer<? super Path> action) {
		wrapper.forEach(action);
	}

	@Override
	public Spliterator<Path> spliterator() {
		return wrapper.spliterator();
	}

	private InputStream openInputStream(boolean buffered) throws IOException {
		if (buffered) {
			return openInputStream(BUFFER_SIZE);
		} else {
			return Files.newInputStream(wrapper);
		}
	}

	public InputStream openInputStream(int bufferSize) throws IOException {
		return new BufferedInputStream(openInputStream(false), bufferSize);
	}

	private OutputStream openOutputStream(boolean buffered) throws IOException {
		if (buffered) {
			return openOutputStream(BUFFER_SIZE);
		} else {
			return Files.newOutputStream(wrapper);
		}
	}

	public OutputStream openOutputStream(int bufferSize) throws IOException {
		return new BufferedOutputStream(openOutputStream(false), bufferSize);
	}

	/**
	 * close this path with a delete recurse instead of delete if exists
	 */
	public void closeWithDeleteRecurse() {
		isDir = true;
	}

	public void mkdirs() throws IOException {
		Files.createDirectories(wrapper);
	}

	public Path getJavaPath() {
		return wrapper;
	}

	@Override
	public void close() throws IOException {
		if (isDir) {
			IOUtil.deleteDirRecurse(wrapper);
		} else {
			Files.deleteIfExists(wrapper);
		}
	}
}
