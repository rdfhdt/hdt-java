package org.rdfhdt.hdt.dictionary.impl.section;

import org.junit.Assert;
import org.junit.Test;
import org.rdfhdt.hdt.iterator.utils.ExceptionIterator;
import org.rdfhdt.hdt.iterator.utils.MapIterator;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.IndexedNode;
import org.rdfhdt.hdt.util.io.compress.CompressUtil;
import org.rdfhdt.hdt.util.string.ByteString;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class OneReadDictionarySectionTest {

	@Test
	public void sectionTest() {
		List<IndexedNode> aa = Arrays.asList(
				new IndexedNode("1", 1),
				new IndexedNode("2", 2),
				new IndexedNode("2", 3),
				new IndexedNode("3", 4),
				new IndexedNode("4", 5),
				new IndexedNode("5", 6),
				new IndexedNode("5", 7),
				new IndexedNode("5", 8),
				new IndexedNode("6", 9),
				new IndexedNode("7", 10),
				new IndexedNode("8", 11),
				new IndexedNode("9", 12)
		);

		OneReadDictionarySection sec1 = new OneReadDictionarySection(
				removeDupe(aa),
				aa.size()
		);
		assertIteratorEquals(removeDupe(aa), sec1.getSortedEntries());

		OneReadDictionarySection sec2 = new OneReadDictionarySection(
				removeDupe(aa),
				aa.size()
		);

		PFCDictionarySection section = new PFCDictionarySection(new HDTSpecification());
		section.load(sec2, null);

		assertIteratorEquals(removeDupe(aa), section.getSortedEntries());
	}

	private void assertIteratorEquals(Iterator<? extends CharSequence>it1, Iterator<? extends CharSequence> it2) {
		while (it1.hasNext()) {
			Assert.assertTrue(it2.hasNext());
			Assert.assertEquals(it1.next().toString(), it2.next().toString());
		}
		Assert.assertFalse(it2.hasNext());
	}

	private Iterator<CharSequence> removeDupe(List<IndexedNode> nodes) {
		return
			new MapIterator<>(
					CompressUtil.asNoDupeCharSequenceIterator(
							ExceptionIterator.of(nodes.iterator())
									.map(in -> {
										in.setNode(ByteString.of(in.getNode()));
										return in;
									}),
							(i, j, k) -> {
							}
					), IndexedNode::getNode
			);
	}
}
