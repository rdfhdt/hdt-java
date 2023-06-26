package org.rdfhdt.hdt.hdt.impl.diskimport;

import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.header.Header;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.Triples;
import org.rdfhdt.hdt.util.io.CloseSuppressPath;
import org.rdfhdt.hdt.util.io.IOUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * HDT implementation delaying the map method to avoid mapping into memory a file if it's not required
 *
 * @author Antoine Willerval
 */
public class MapOnCallHDT implements HDT {
	private final Path hdtFile;
	private HDT hdt;

	public MapOnCallHDT(Path hdtFile) {
		// remove close suppress path files
		this.hdtFile = CloseSuppressPath.unpack(hdtFile.toAbsolutePath());
	}

	private HDT mapOrGetHDT() {
		if (hdt == null) {
			// map the HDT into memory
			try {
				hdt = HDTManager.mapHDT(hdtFile.toString());
			} catch (IOException e) {
				throw new RuntimeException("Can't map the hdt file", e);
			}
		}
		return hdt;
	}

	@Override
	public Header getHeader() {
		return mapOrGetHDT().getHeader();
	}

	@Override
	public Dictionary getDictionary() {
		return mapOrGetHDT().getDictionary();
	}

	@Override
	public Triples getTriples() {
		return mapOrGetHDT().getTriples();
	}

	@Override
	public void saveToHDT(OutputStream output, ProgressListener listener) throws IOException {
		Files.copy(hdtFile, output);
	}

	@Override
	public void saveToHDT(String fileName, ProgressListener listener) throws IOException {
		Path future = Path.of(fileName).toAbsolutePath();
		if (!future.equals(hdtFile)) {
			// copy file if not equals
			Files.copy(hdtFile, future);
		}
		// otherwise, no need to copy a file if it's already there
	}

	@Override
	public long size() {
		return mapOrGetHDT().size();
	}

	@Override
	public String getBaseURI() {
		return mapOrGetHDT().getBaseURI();
	}

	@Override
	public void close() throws IOException {
		IOUtil.closeAll(hdt);
		hdt = null;
	}

	@Override
	public IteratorTripleString search(CharSequence subject, CharSequence predicate, CharSequence object) throws NotFoundException {
		return mapOrGetHDT().search(subject, predicate, object);
	}
	@Override
	public IteratorTripleString search(
		CharSequence subject,
		CharSequence predicate,
		CharSequence object,
		CharSequence graph
	) throws NotFoundException {
		return mapOrGetHDT().search(subject, predicate, object, graph);
	}
}
