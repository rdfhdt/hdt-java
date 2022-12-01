package org.rdfhdt.hdt.dictionary.impl;

import org.rdfhdt.hdt.dictionary.DictionarySection;
import org.rdfhdt.hdt.dictionary.DictionarySectionPrivate;
import org.rdfhdt.hdt.dictionary.TempDictionary;
import org.rdfhdt.hdt.dictionary.impl.section.WriteDictionarySection;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.header.Header;
import org.rdfhdt.hdt.listener.MultiThreadListener;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInfo;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.util.concurrent.ExceptionThread;
import org.rdfhdt.hdt.util.io.CountInputStream;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.listener.IntermediateListener;
import org.rdfhdt.hdt.util.listener.ListenerUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * Version of four section dictionary with {@link org.rdfhdt.hdt.dictionary.impl.section.WriteDictionarySection}
 *
 * @author Antoine Willerval
 */
public class WriteFourSectionDictionary extends BaseDictionary {
	public WriteFourSectionDictionary(HDTOptions spec, Path filename, int bufferSize) {
		super(spec);
		String name = filename.getFileName().toString();
		subjects = new WriteDictionarySection(spec, filename.resolveSibling(name + "SU"), bufferSize);
		predicates = new WriteDictionarySection(spec, filename.resolveSibling(name + "PR"), bufferSize);
		objects = new WriteDictionarySection(spec, filename.resolveSibling(name + "OB"), bufferSize);
		shared = new WriteDictionarySection(spec, filename.resolveSibling(name + "SH"), bufferSize);
	}

	public WriteFourSectionDictionary(HDTOptions spec, DictionarySectionPrivate subjects,
									  DictionarySectionPrivate predicates,
									  DictionarySectionPrivate objects,
									  DictionarySectionPrivate shared) {
		super(spec);
		this.subjects = subjects;
		this.predicates = predicates;
		this.objects = objects;
		this.shared = shared;
	}

	@Override
	public void loadAsync(TempDictionary other, ProgressListener listener) throws InterruptedException {
		MultiThreadListener ml = ListenerUtil.multiThreadListener(listener);
		ml.unregisterAllThreads();
		ExceptionThread.async("FourSecSAsyncReader",
						() -> predicates.load(other.getPredicates(), new IntermediateListener(ml, "Predicate: ")),
						() -> subjects.load(other.getSubjects(), new IntermediateListener(ml, "Subjects:  ")),
						() -> shared.load(other.getShared(), new IntermediateListener(ml, "Shared:    ")),
						() -> objects.load(other.getObjects(), new IntermediateListener(ml, "Object:    "))
				)
				.startAll()
				.joinAndCrashIfRequired();
		ml.unregisterAllThreads();
	}

	@Override
	public void load(InputStream input, ControlInfo ci, ProgressListener listener) throws IOException {
		throw new NotImplementedException();
	}

	@Override
	public void mapFromFile(CountInputStream in, File f, ProgressListener listener) throws IOException {
		throw new NotImplementedException();
	}

	@Override
	public void load(TempDictionary other, ProgressListener listener) {
		throw new NotImplementedException();
	}

	@Override
	public void save(OutputStream output, ControlInfo ci, ProgressListener listener) throws IOException {
		ci.setType(ControlInfo.Type.DICTIONARY);
		ci.setFormat(getType());
		ci.setInt("elements", this.getNumberOfElements());
		ci.save(output);

		IntermediateListener iListener = new IntermediateListener(listener);
		iListener.setRange(0, 25);
		iListener.setPrefix("Save shared: ");
		shared.save(output, iListener);
		iListener.setRange(25, 50);
		iListener.setPrefix("Save subjects: ");
		subjects.save(output, iListener);
		iListener.setRange(50, 75);
		iListener.setPrefix("Save predicates: ");
		predicates.save(output, iListener);
		iListener.setRange(75, 100);
		iListener.setPrefix("Save objects: ");
		objects.save(output, iListener);
	}

	@Override
	public void populateHeader(Header header, String rootNode) {
		header.insert(rootNode, HDTVocabulary.DICTIONARY_TYPE, getType());
		header.insert(rootNode, HDTVocabulary.DICTIONARY_NUMSHARED, getNshared());
		header.insert(rootNode, HDTVocabulary.DICTIONARY_SIZE_STRINGS, size());
	}

	@Override
	public String getType() {
		return HDTVocabulary.DICTIONARY_TYPE_FOUR_SECTION;
	}

	@Override
	public void close() throws IOException {
		IOUtil.closeAll(shared, subjects, predicates, objects);
	}
}
