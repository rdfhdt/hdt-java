package org.rdfhdt.hdt.dictionary.impl.kcat;

import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.iterator.utils.CombinedIterator;
import org.rdfhdt.hdt.iterator.utils.ExceptionIterator;
import org.rdfhdt.hdt.iterator.utils.MapIterator;
import org.rdfhdt.hdt.iterator.utils.MergeExceptionIterator;
import org.rdfhdt.hdt.iterator.utils.PeekIterator;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Antoine Willerval
 */
public class GroupBySubjectMapIterator implements Iterator<TripleID> {
	private final PeekIterator<TripleID> mergeIterator;
	private final List<TripleID> groupList = new ArrayList<>();
	private Iterator<TripleID> groupListIterator;
	private TripleID next;

	public GroupBySubjectMapIterator(Iterator<TripleID> mergeIterator) {
		this.mergeIterator = new PeekIterator<>(mergeIterator);
	}

	@Override
	public boolean hasNext() {
		if (next != null) {
			return true;
		}

		// get triples from the group
		if (groupListIterator != null) {
			if (groupListIterator.hasNext()) {
				next = groupListIterator.next();
				return true;
			}

			// clear the group and set to new iteration
			groupList.clear();
			groupListIterator = null;
		}

		// do we have more elements?
		if (!mergeIterator.hasNext()) {
			return false;
		}

		long subject = mergeIterator.peek().getSubject();

		// we add all the elements while the subject are the same
		do {
			groupList.add(mergeIterator.next().clone());
		} while (mergeIterator.hasNext() && mergeIterator.peek().getSubject() == subject);

		groupList.sort(TripleID::compareTo);

		groupListIterator = groupList.iterator();

		if (groupListIterator.hasNext()) {
			next = groupListIterator.next();
			return true;
		}

		// just to be sure
		return false;
	}

	@Override
	public TripleID next() {
		if (!hasNext()) {
			return null;
		}
		try {
			return next;
		} finally {
			next = null;
		}
	}

	private static long firstSubjectTripleId(HDT hdt) {
		if (hdt.getDictionary().getSubjects().getNumberOfElements() == 0) {
			// no subjects
			return -1;
		}
		IteratorTripleID it = hdt.getTriples().search(new TripleID(
				hdt.getDictionary().getNshared() + 1,
				0,
				0
		));
		if (it.hasNext()) {
			// extract result
			it.next();
			return it.getLastTriplePosition();
		} else {
			return -1;
		}
	}

	public static Iterator<TripleID> fromHDTs(KCatMerger merger, HDT[] hdts) {
		final long shared = merger.getCountShared();
		List<ExceptionIterator<TripleID, RuntimeException>> sharedSubjectIterators = IntStream.range(0, hdts.length)
				.mapToObj(hdtIndex -> {
					// extract hdt elements for this index
					HDT hdt = hdts[hdtIndex];

					if (hdt.getTriples().getNumberOfElements() == 0) {
						// no triples
						return ExceptionIterator.<TripleID, RuntimeException>empty();
					}
					// get the first subject triple id
					long firstSubjectTripleId = firstSubjectTripleId(hdt);

					ExceptionIterator<TripleID, RuntimeException> subjectIteratorMapped;
					if (firstSubjectTripleId == -1) {
						// no triples
						subjectIteratorMapped = ExceptionIterator.empty();
					} else {
						// create a subject iterator, mapped to the new IDs
						IteratorTripleID subjectIterator = hdt.getTriples().searchAll();
						subjectIterator.goTo(firstSubjectTripleId);
						subjectIteratorMapped = ExceptionIterator.of(
								new SharedOnlyIterator(
										new MapIterator<>(subjectIterator, (tid) -> {
											assert inHDT(tid, hdts[hdtIndex]);
											return merger.extractMapped(hdtIndex, tid);
										}),
										shared
								)
						);
					}

					if (shared == 0) {
						return subjectIteratorMapped;
					}

					Iterator<TripleID> sharedIterator = new SharedStopIterator(hdt.getTriples().searchAll(), hdt.getDictionary().getNshared());
					Iterator<TripleID> sharedIteratorMapped = new MapIterator<>(sharedIterator, (tid) -> {
						assert inHDT(tid, hdts[hdtIndex]);
						return merger.extractMapped(hdtIndex, tid);
					});

					return new MergeExceptionIterator<>(
							subjectIteratorMapped,
							ExceptionIterator.of(sharedIteratorMapped),
							Comparator.comparingLong(TripleID::getSubject)
					);
				}).collect(Collectors.toList());
		List<ExceptionIterator<TripleID, RuntimeException>> subjectIterators = IntStream.range(0, hdts.length)
				.mapToObj(hdtIndex -> {
					// extract hdt elements for this index
					HDT hdt = hdts[hdtIndex];

					// get the first subject triple id
					long firstSubjectTripleId = firstSubjectTripleId(hdt);

					if (firstSubjectTripleId == -1) {
						return ExceptionIterator.<TripleID, RuntimeException>empty();
					}

					// create a subject iterator, mapped to the new IDs
					IteratorTripleID subjectIterator = hdt.getTriples().searchAll();
					subjectIterator.goTo(firstSubjectTripleId);

					return ExceptionIterator.<TripleID, RuntimeException>of(
							new NoSharedIterator(
									new MapIterator<>(subjectIterator, (tid) -> merger.extractMapped(hdtIndex, tid)),
									shared
							)
					);
				}).collect(Collectors.toList());
		return new GroupBySubjectMapIterator(
				new NoDupeTripleIDIterator(
						CombinedIterator.combine(List.of(
								MergeExceptionIterator.buildOfTree(
										Function.identity(),
										Comparator.comparingLong(TripleID::getSubject),
										sharedSubjectIterators,
										0,
										sharedSubjectIterators.size()
								).asIterator(),
								MergeExceptionIterator.buildOfTree(
										Function.identity(),
										Comparator.comparingLong(TripleID::getSubject),
										subjectIterators,
										0,
										subjectIterators.size()
								).asIterator()
						))
				));
	}

	private static boolean inHDT(TripleID id, HDT hdt) {
		long s = id.getSubject();
		long p = id.getPredicate();
		long o = id.getObject();
		return s >= 1 && s <= hdt.getDictionary().getNsubjects()
				&& p >= 1 && p <= hdt.getDictionary().getNpredicates()
				&& o >= 1 && o <= hdt.getDictionary().getNobjects();
	}

	private static class NoDupeTripleIDIterator implements Iterator<TripleID> {
		private TripleID next;
		private final PeekIterator<TripleID> it;

		public NoDupeTripleIDIterator(Iterator<TripleID> it) {
			this.it = new PeekIterator<>(it);
		}

		@Override
		public boolean hasNext() {
			if (next != null) {
				return true;
			}
			if (!it.hasNext()) {
				return false;
			}

			next = it.next();

			assert next.isValid() : "Can't have empty tripleID";

			// pass all the duplicated fields
			while (it.hasNext() && it.peek().equals(next)) {
				it.next();
			}

			return true;
		}

		@Override
		public TripleID next() {
			if (!hasNext()) {
				return null;
			}
			try {
				return next;
			} finally {
				next = null;
			}
		}
	}

	private static class SharedStopIterator implements Iterator<TripleID> {
		private final Iterator<TripleID> it;
		private final long shared;
		private TripleID next;

		private SharedStopIterator(Iterator<TripleID> it, long shared) {
			this.it = it;
			this.shared = shared;
		}


		@Override
		public boolean hasNext() {
			if (next != null) {
				return next.getSubject() <= shared;
			}

			if (!it.hasNext()) {
				return false;
			}

			next = it.next();

			return next.getSubject() <= shared;
		}

		@Override
		public TripleID next() {
			if (!hasNext()) {
				return null;
			}
			try {
				return next;
			} finally {
				next = null;
			}
		}
	}

	private static class SharedOnlyIterator implements Iterator<TripleID> {
		private final Iterator<TripleID> it;
		private final long shared;
		private TripleID next;

		private SharedOnlyIterator(Iterator<TripleID> it, long shared) {
			this.it = it;
			this.shared = shared;
		}


		@Override
		public boolean hasNext() {
			if (next != null) {
				return true;
			}

			// search over the next results
			while (it.hasNext()) {
				TripleID next = it.next();

				// is this element a shared element?
				if (next.getSubject() <= shared) {
					this.next = next;
					return true;
				}
			}

			return false;
		}

		@Override
		public TripleID next() {
			if (!hasNext()) {
				return null;
			}
			try {
				return next;
			} finally {
				next = null;
			}
		}
	}

	private static class NoSharedIterator implements Iterator<TripleID> {
		private final Iterator<TripleID> it;
		private final long shared;
		private TripleID next;

		private NoSharedIterator(Iterator<TripleID> it, long shared) {
			this.it = it;
			this.shared = shared;
		}


		@Override
		public boolean hasNext() {
			if (next != null) {
				return true;
			}

			// search over the next results
			while (it.hasNext()) {
				TripleID next = it.next();

				// is this element a shared element?
				if (next.getSubject() > shared) {
					this.next = next;
					return true;
				}
			}

			return false;
		}

		@Override
		public TripleID next() {
			if (!hasNext()) {
				return null;
			}
			try {
				return next;
			} finally {
				next = null;
			}
		}
	}
}
