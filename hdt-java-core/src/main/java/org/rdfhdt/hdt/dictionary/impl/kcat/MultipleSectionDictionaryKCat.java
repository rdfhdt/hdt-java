package org.rdfhdt.hdt.dictionary.impl.kcat;

import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.dictionary.DictionaryKCat;
import org.rdfhdt.hdt.dictionary.DictionarySection;
import org.rdfhdt.hdt.util.LiteralsUtils;
import org.rdfhdt.hdt.util.string.ByteString;
import org.rdfhdt.hdt.util.string.CharSequenceComparator;

import java.util.Map;
import java.util.TreeMap;

public class MultipleSectionDictionaryKCat implements DictionaryKCat {
	private final Dictionary dictionary;

	public MultipleSectionDictionaryKCat(Dictionary dictionary) {
		this.dictionary = dictionary;
	}

	@Override
	public Map<CharSequence, DictionarySection> getSubSections() {
		Map<CharSequence, DictionarySection> sections = new TreeMap<>(CharSequenceComparator.getInstance());
		dictionary.getAllObjects().forEach((key, section) -> {
			if (!LiteralsUtils.NO_DATATYPE.equals(key)) {
				// we ignore this section because it will be used in the shared compute
				sections.put(ByteString.of(key), section);
			}
		});
		return sections;
	}

	@Override
	public DictionarySection getSubjectSection() {
		return dictionary.getSubjects();
	}

	@Override
	public DictionarySection getPredicateSection() {
		return dictionary.getPredicates();
	}

	@Override
	public DictionarySection getObjectSection() {
		return dictionary.getAllObjects().get("NO_DATATYPE");
	}

	@Override
	public DictionarySection getSharedSection() {
		return dictionary.getShared();
	}

	@Override
	public long countSubjects() {
		return dictionary.getSubjects().getNumberOfElements() + countShared();
	}

	@Override
	public long countShared() {
		return dictionary.getShared().getNumberOfElements();
	}

	@Override
	public long countPredicates() {
		return dictionary.getPredicates().getNumberOfElements();
	}

	@Override
	public long countObjects() {
		long count = 0;
		for (DictionarySection sec : dictionary.getAllObjects().values()) {
			count += sec.getNumberOfElements();
		}
		return count + countShared();
	}

	@Override
	public long objectShift() {
		return countObjects() - getObjectSection().getNumberOfElements();
	}
}
