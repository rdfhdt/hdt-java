package org.rdfhdt.hdt.dictionary.impl;

import org.rdfhdt.hdt.compact.integer.VByte;
import org.rdfhdt.hdt.dictionary.DictionarySectionPrivate;
import org.rdfhdt.hdt.dictionary.TempDictionary;
import org.rdfhdt.hdt.dictionary.impl.section.WriteDictionarySection;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.header.Header;
import org.rdfhdt.hdt.iterator.utils.PipedCopyIterator;
import org.rdfhdt.hdt.listener.MultiThreadListener;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInfo;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.util.LiteralsUtils;
import org.rdfhdt.hdt.util.concurrent.ExceptionThread;
import org.rdfhdt.hdt.util.io.CountInputStream;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.listener.IntermediateListener;
import org.rdfhdt.hdt.util.listener.ListenerUtil;
import org.rdfhdt.hdt.util.string.ByteStringUtil;
import org.rdfhdt.hdt.util.string.CharSequenceComparator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Version of mutli-section dictionary with {@link org.rdfhdt.hdt.dictionary.impl.section.WriteDictionarySection}
 * @author Antoine Willerval
 */
public class WriteMultipleSectionDictionary extends MultipleBaseDictionary {
	private final Path filename;
	private final int bufferSize;
	public WriteMultipleSectionDictionary(HDTOptions spec, Path filename, int bufferSize) {
		super(spec);
		this.filename = filename;
		this.bufferSize = bufferSize;
		String name = filename.getFileName().toString();
		subjects = new WriteDictionarySection(spec, filename.resolveSibling(name + "SU"), bufferSize);
		predicates = new WriteDictionarySection(spec, filename.resolveSibling(name + "PR"), bufferSize);
		objects = new TreeMap<>(CharSequenceComparator.getInstance());
		shared = new WriteDictionarySection(spec, filename.resolveSibling(name + "SH"), bufferSize);
	}

	@Override
	public long getNAllObjects() {
		return objects.values().stream().mapToLong(DictionarySectionPrivate::getNumberOfElements).sum();
	}

	private ExceptionThread fillSection(Iterator<? extends CharSequence> objects, ProgressListener listener) throws InterruptedException {
		PipedCopyIterator<CharSequence> noDatatypeIterator = new PipedCopyIterator<>();
		PipedCopyIterator<CharSequence> datatypeIterator = new PipedCopyIterator<>();
		String name = filename.getFileName().toString();
		WriteDictionarySection noDatatypeSection = new WriteDictionarySection(spec, filename.resolveSibling(name + LiteralsUtils.NO_DATATYPE), bufferSize);
		this.objects.put(LiteralsUtils.NO_DATATYPE, noDatatypeSection);
		return new ExceptionThread(() -> {
			// object reader
			try {
				CharSequence oldType = null;
				boolean noDatatype = false;
				while (objects.hasNext()) {
					CharSequence next = objects.next();

					CharSequence type = LiteralsUtils.getType(next);

					if (oldType != null) {
						if (oldType.equals(type)) {
							if (noDatatype) {
								noDatatypeIterator.addElement(next);
							} else {
								datatypeIterator.addElement(next);
							}
							continue;
						} else {
							if (!noDatatype) {
								datatypeIterator.closePipe();
							}
						}
					}
					oldType = type;

					if (LiteralsUtils.isNoDatatype(type)) {
						noDatatypeIterator.addElement(next);
						noDatatype = true;
					} else {
						datatypeIterator.addElement(next);
						noDatatype = false;
					}
				}
				noDatatypeIterator.closePipe();
				datatypeIterator.closePipe();
			} catch (Throwable e) {
				try {
					throw e;
				} finally {
					try {
						noDatatypeIterator.closePipe(e);
					} finally {
						datatypeIterator.closePipe(e);
					}
				}
			}
		}, "MultiSecSAsyncObjectReader").attach(new ExceptionThread(() -> {
			// datatype writer
			throw new NotImplementedException("MultiSecSAsyncObjectReader");
		}, "MultiSecSAsyncObjectDatatypeWriter")).attach(new ExceptionThread(() -> {
			// no datatype writer
//			noDatatypeSection.load(new OneReadDictionarySection(noDatatypeIterator), );
			throw new NotImplementedException("MultiSecSAsyncObjectReader");
		}, "MultiSecSAsyncObjectNoDatatypeWriter"));
	}

	@Override
	public void loadAsync(TempDictionary other, ProgressListener listener) throws InterruptedException {
		MultiThreadListener ml = ListenerUtil.multiThreadListener(listener);
		ml.unregisterAllThreads();
		ExceptionThread.async("MultiSecSAsyncReader",
						() -> predicates.load(other.getPredicates(), new IntermediateListener(ml, "Predicate: ")),
						() -> subjects.load(other.getSubjects(), new IntermediateListener(ml, "Subjects:  ")),
						() -> shared.load(other.getShared(), new IntermediateListener(ml, "Shared:    "))
				).attach(fillSection(other.getObjects().getEntries(), new IntermediateListener(ml, "Objects:   ")))
				.startAll()
				.joinAndCrashIfRequired();
		ml.unregisterAllThreads();
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

		VByte.encode(output, objects.size());

		for (Map.Entry<CharSequence, DictionarySectionPrivate> entry : objects.entrySet()) {
			IOUtil.writeSizedBuffer(output, entry.getKey().toString().getBytes(ByteStringUtil.STRING_ENCODING), listener);
		}

		for (Map.Entry<CharSequence, DictionarySectionPrivate> entry : objects.entrySet()) {
			entry.getValue().save(output, iListener);
		}

	}

	@Override
	public void close() throws IOException {
		try {
			IOUtil.closeAll(shared, subjects, predicates);
		} finally {
			IOUtil.closeAll(objects.values());
		}
	}

	@Override
	public void populateHeader(Header header, String rootNode) {
		header.insert(rootNode, HDTVocabulary.DICTIONARY_TYPE, getType());
		header.insert(rootNode, HDTVocabulary.DICTIONARY_NUMSHARED, getNshared());
		header.insert(rootNode, HDTVocabulary.DICTIONARY_SIZE_STRINGS, size());
	}

	@Override
	public String getType() {
		return HDTVocabulary.DICTIONARY_TYPE_MULT_SECTION;
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

}
