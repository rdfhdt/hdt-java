package org.rdfhdt.hdt.rdf.parsers;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.rdf.RDFParserCallback;
import org.rdfhdt.hdt.rdf.RDFParserFactory;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.util.LargeFakeDataSetStreamSupplier;

import java.io.IOException;
import java.nio.file.Path;

public class RDFParserHDTTest {


	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	@Test
	public void hdtTest() throws IOException, ParserException, NotFoundException {
		Path root = tempDir.newFile("test.hdt").toPath();

		LargeFakeDataSetStreamSupplier supplier = LargeFakeDataSetStreamSupplier
				.createSupplierWithMaxTriples(20, 34);

		HDT hdt = HDTManager.generateHDT(
				supplier.createTripleStringStream(),
				"http://example.org/#",
				new HDTSpecification(),
				null
		);
		hdt.saveToHDT(root.toAbsolutePath().toString(), null);

		supplier.reset();

		String filename = root.toAbsolutePath().toString();
		RDFNotation dir = RDFNotation.guess(filename);
		Assert.assertEquals(dir, RDFNotation.HDT);
		RDFParserCallback callback = RDFParserFactory.getParserCallback(dir);
		Assert.assertTrue(callback instanceof RDFParserHDT);

		IteratorTripleString it = hdt.search("", "", "");

		callback.doParse(filename, "http://example.org/#", dir, true, (triple, pos) ->
				Assert.assertEquals(it.next(), triple)
		);

		hdt.close();
	}
}
