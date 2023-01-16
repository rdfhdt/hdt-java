package org.rdfhdt.hdt.util;

import org.junit.Test;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTOptionsKeys;
import org.rdfhdt.hdt.rdf.RDFParserCallback;
import org.rdfhdt.hdt.rdf.RDFParserFactory;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.triples.impl.utils.HDTTestUtils;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class UnicodeEscapeTest {
	@Test
	public void encodeTest() throws ParserException {
		String file = Objects.requireNonNull(UnicodeEscapeTest.class.getClassLoader().getResource("unicodeTest.nt"), "can't find file").getFile();

		RDFParserCallback factory = RDFParserFactory.getParserCallback(RDFNotation.NTRIPLES, HDTOptions.of(
				Map.of(HDTOptionsKeys.NT_SIMPLE_PARSER_KEY, "true")
		));
		RDFParserCallback factory2 = RDFParserFactory.getParserCallback(RDFNotation.NTRIPLES, HDTOptions.of(
				Map.of(HDTOptionsKeys.NT_SIMPLE_PARSER_KEY, "false")
		));


		Set<TripleString> ts1 = new TreeSet<>(Comparator.comparing(t -> {
			try {
				return t.asNtriple().toString();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}));
		Set<TripleString> ts2 = new TreeSet<>(Comparator.comparing(t -> {
			try {
				return t.asNtriple().toString();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}));
		factory.doParse(file, HDTTestUtils.BASE_URI, RDFNotation.NTRIPLES, true, (t, i) -> ts1.add(t.tripleToString()));
		factory2.doParse(file, HDTTestUtils.BASE_URI, RDFNotation.NTRIPLES, true, (t, i) -> ts2.add(t.tripleToString()));

		Iterator<TripleString> it1 = ts1.iterator();
		Iterator<TripleString> it2 = ts2.iterator();

		HDTTestUtils.CoIterator<TripleString, TripleString> it = new HDTTestUtils.CoIterator<>(it1, it2);

		while (it.hasNext()) {
			HDTTestUtils.Tuple<TripleString, TripleString> e = it.next();
			System.out.println(e);
			assertEquals(e.t1, e.t2);
		}
	}
	@Test
	public void decodeTest() {
		assertEquals(
				new String(Character.toChars(0x0002dd76)),
				UnicodeEscape.unescapeString("\\U0002dd76")
		);

	}
}
