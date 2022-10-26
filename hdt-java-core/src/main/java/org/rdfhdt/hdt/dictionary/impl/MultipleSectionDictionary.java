package org.rdfhdt.hdt.dictionary.impl;

import org.rdfhdt.hdt.compact.integer.VByte;
import org.rdfhdt.hdt.dictionary.DictionarySection;
import org.rdfhdt.hdt.dictionary.DictionarySectionPrivate;
import org.rdfhdt.hdt.dictionary.TempDictionary;
import org.rdfhdt.hdt.dictionary.impl.section.DictionarySectionFactory;
import org.rdfhdt.hdt.dictionary.impl.section.PFCDictionarySection;
import org.rdfhdt.hdt.exceptions.IllegalFormatException;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.header.Header;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInfo;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.util.CustomIterator;
import org.rdfhdt.hdt.util.LiteralsUtils;
import org.rdfhdt.hdt.util.io.CountInputStream;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdt.util.listener.IntermediateListener;
import org.rdfhdt.hdt.util.string.ByteStringUtil;
import org.rdfhdt.hdt.util.string.CharSequenceComparator;
import org.rdfhdt.hdt.util.string.CompactString;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class
MultipleSectionDictionary extends MultipleBaseDictionary {


	public MultipleSectionDictionary(HDTOptions spec) {
		super(spec);
		// FIXME: Read type from spec.
		subjects = new PFCDictionarySection(spec);
		predicates = new PFCDictionarySection(spec);
		objects = new TreeMap<>(CharSequenceComparator.getInstance());
		shared = new PFCDictionarySection(spec);
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#load(hdt.dictionary.Dictionary)
	 */
	@Override
	public void load(TempDictionary other, ProgressListener listener) {
		IntermediateListener iListener = new IntermediateListener(listener);
		subjects.load(other.getSubjects(), iListener);
		predicates.load(other.getPredicates(), iListener);
		Iterator<? extends CharSequence> iter = other.getObjects().getEntries();

		// TODO: allow the usage of OneReadDictionarySection
		Map<CharSequence, Long> literalsCounts = new HashMap<>(other.getObjects().getLiteralsCounts());
		literalsCounts.computeIfPresent(LiteralsUtils.NO_DATATYPE, (key, value) -> (value - other.getShared().getNumberOfElements()));
		CustomIterator customIterator = new CustomIterator(iter, literalsCounts);
		while (customIterator.hasNext()) {
			PFCDictionarySection section = new PFCDictionarySection(spec);
			CharSequence type = LiteralsUtils.getType(customIterator.prev);
			long numEntries = literalsCounts.get(type);

			section.load(customIterator, numEntries, listener);
			section.locate(new CompactString("\"\uD83C\uDDEB\uD83C\uDDF7\"@ro"));
			objects.put(type, section);
		}
		shared.load(other.getShared(), iListener);
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#save(java.io.OutputStream, hdt.ControlInformation, hdt.ProgressListener)
	 */
	@Override
	public void save(OutputStream output, ControlInfo ci, ProgressListener listener) throws IOException {
		ci.setType(ControlInfo.Type.DICTIONARY);
		ci.setFormat(getType());
		ci.setInt("elements", this.getNumberOfElements());
		ci.save(output);

		IntermediateListener iListener = new IntermediateListener(listener);
		shared.save(output, iListener);
		subjects.save(output, iListener);
		predicates.save(output, iListener);

		writeLiteralsMap(output, iListener);

	}

	/*
	------------------
	|len| Literal URI|
	------------------
	 */
	private void writeLiteralsMap(OutputStream output, ProgressListener listener) throws IOException {
		Iterator<Map.Entry<CharSequence, DictionarySectionPrivate>> hmIterator = objects.entrySet().iterator();
		int numberOfTypes = objects.size();
		VByte.encode(output, numberOfTypes);

		List<CharSequence> types = new ArrayList<>();

		while (hmIterator.hasNext()) {
			Map.Entry<CharSequence, DictionarySectionPrivate> entry = hmIterator.next();
			CharSequence uri = entry.getKey();
			String uriStr = uri.toString();
			byte[] bytes = uriStr.getBytes();
			VByte.encode(output, bytes.length);
			IOUtil.writeBuffer(output, bytes, 0, bytes.length, listener);
			types.add(uri);
		}
		for (CharSequence type : types) {
			this.objects.get(type).save(output, listener);
		}
	}

	private void readLiteralsMap(InputStream input, ProgressListener listener) throws IOException {
		int numberOfTypes = (int) VByte.decode(input);
		List<CharSequence> types = new ArrayList<>();
		for (int i = 0; i < numberOfTypes; i++) {
			int length = (int) VByte.decode(input);
			byte[] type = IOUtil.readBuffer(input, length, listener);
			types.add(new CompactString(type));
		}
		for (CharSequence type : types) {
			this.objects.put(type, DictionarySectionFactory.loadFrom(input, listener));
		}
	}

	private void mapLiteralsMap(CountInputStream input, File f, ProgressListener listener) throws IOException {
		int numberOfTypes = (int) VByte.decode(input);
		List<CharSequence> types = new ArrayList<>();
		for (int i = 0; i < numberOfTypes; i++) {
			int length = (int) VByte.decode(input);
			byte[] type = IOUtil.readBuffer(input, length, listener);
			types.add(new CompactString(type));
		}
		for (CharSequence type : types) {
			this.objects.put(type, DictionarySectionFactory.loadFrom(input, f, listener));
		}

	}


	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#load(java.io.InputStream)
	 */
	@Override
	public void load(InputStream input, ControlInfo ci, ProgressListener listener) throws IOException {
		if (ci.getType() != ControlInfo.Type.DICTIONARY) {
			throw new IllegalFormatException("Trying to read a dictionary section, but was not dictionary.");
		}

		IntermediateListener iListener = new IntermediateListener(listener);

		shared = DictionarySectionFactory.loadFrom(input, iListener);
		subjects = DictionarySectionFactory.loadFrom(input, iListener);
		predicates = DictionarySectionFactory.loadFrom(input, iListener);

		readLiteralsMap(input, listener);
	}

	@Override
	public void mapFromFile(CountInputStream in, File f, ProgressListener listener) throws IOException {
		ControlInformation ci = new ControlInformation();
		ci.load(in);
		if (ci.getType() != ControlInfo.Type.DICTIONARY) {
			throw new IllegalFormatException("Trying to read a dictionary section, but was not dictionary.");
		}

		IntermediateListener iListener = new IntermediateListener(listener);
		shared = DictionarySectionFactory.loadFrom(in, f, iListener);
		subjects = DictionarySectionFactory.loadFrom(in, f, iListener);
		predicates = DictionarySectionFactory.loadFrom(in, f, iListener);

		mapLiteralsMap(in, f, listener);

		// Use cache only for predicates. Preload only up to 100K predicates.
		// FIXME: DISABLED
//		predicates = new DictionarySectionCacheAll(predicates, predicates.getNumberOfElements()<100000);
	}

	@Override
	public long getNAllObjects() {
		return objects.values().stream().mapToLong(DictionarySectionPrivate::getNumberOfElements).sum();
	}

	@Override
	public TreeMap<CharSequence, DictionarySection> getAllObjects() {
		return new TreeMap<>(objects);
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#populateHeader(hdt.header.Header, java.lang.String)
	 */
	@Override
	public void populateHeader(Header header, String rootNode) {
		header.insert(rootNode, HDTVocabulary.DICTIONARY_TYPE, getType());
//		header.insert(rootNode, HDTVocabulary.DICTIONARY_NUMSUBJECTS, getNsubjects());
//		header.insert(rootNode, HDTVocabulary.DICTIONARY_NUMPREDICATES, getNpredicates());
//		header.insert(rootNode, HDTVocabulary.DICTIONARY_NUMOBJECTS, getNobjects());
		header.insert(rootNode, HDTVocabulary.DICTIONARY_NUMSHARED, getNshared());
//		header.insert(rootNode, HDTVocabulary.DICTIONARY_MAXSUBJECTID, getMaxSubjectID());
//		header.insert(rootNode, HDTVocabulary.DICTIONARY_MAXPREDICATEID, getMaxPredicateID());
//		header.insert(rootNode, HDTVocabulary.DICTIONARY_MAXOBJECTTID, getMaxObjectID());
		header.insert(rootNode, HDTVocabulary.DICTIONARY_SIZE_STRINGS, size());
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#getType()
	 */
	@Override
	public String getType() {
		return HDTVocabulary.DICTIONARY_TYPE_MULT_SECTION;
	}

	@Override
	public void close() throws IOException {
		shared.close();
		subjects.close();
		predicates.close();

		// close all subsections
		IOUtil.closeAll(objects.values());
	}

	@Override
	public void loadAsync(TempDictionary other, ProgressListener listener) {
		throw new NotImplementedException();
	}
}
