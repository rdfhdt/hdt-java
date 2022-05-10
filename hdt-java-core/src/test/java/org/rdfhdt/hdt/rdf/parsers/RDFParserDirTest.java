package org.rdfhdt.hdt.rdf.parsers;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.header.HeaderUtil;
import org.rdfhdt.hdt.rdf.RDFParserCallback;
import org.rdfhdt.hdt.rdf.RDFParserFactory;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.util.LargeFakeDataSetStreamSupplier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RDFParserDirTest {

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	@Test
	public void dirTest() throws IOException, ParserException {
		Path root = tempDir.newFolder().toPath();
		Files.createDirectories(root);

		Path testDir1 = root.resolve("testDir1");
		Path testDir2 = root.resolve("testDir2");
		Path testDir3 = root.resolve("testDir3");
		Path testDir4 = testDir3.resolve("testDir4");

		Files.createDirectories(testDir1);
		Files.createDirectories(testDir2);
		Files.createDirectories(testDir3);
		Files.createDirectories(testDir4);

		LargeFakeDataSetStreamSupplier supplier = LargeFakeDataSetStreamSupplier
				.createSupplierWithMaxTriples(20, 34);

		supplier.createNTFile(root.resolve("test.nt").toAbsolutePath().toString());
		supplier.createNTFile(testDir1.resolve("test1.nt").toAbsolutePath().toString());
		supplier.createNTFile(testDir2.resolve("test21.nt").toAbsolutePath().toString());
		supplier.createNTFile(testDir2.resolve("test22.nt").toAbsolutePath().toString());
		supplier.createNTFile(testDir3.resolve("test31.nt").toAbsolutePath().toString());
		supplier.createNTFile(testDir3.resolve("test32.nt").toAbsolutePath().toString());

		Files.writeString(testDir2.resolve("thing.txt"), "Not parsable RDF DATA");
		Files.writeString(root.resolve("thing.py"), "print('Not parsable RDF DATA')");
		Files.writeString(testDir4.resolve("thing.sh"), "echo \"Not Parsable RDF data\"");

		supplier.reset();

		List<TripleString> excepted = new ArrayList<>();
		// 6 for the 6 files
		for (int i = 0; i < 6; i++) {
			Iterator<TripleString> it = supplier.createTripleStringStream();
			while (it.hasNext()) {
				TripleString ts = it.next();
				TripleString e = new TripleString(
						HeaderUtil.cleanURI(ts.getSubject().toString()),
						HeaderUtil.cleanURI(ts.getPredicate().toString()),
						HeaderUtil.cleanURI(ts.getObject().toString())
				);
				excepted.add(e);
			}
		}

		String filename = root.toAbsolutePath().toString();
		RDFNotation dir = RDFNotation.guess(filename);
		Assert.assertEquals(dir, RDFNotation.DIR);
		RDFParserCallback callback = RDFParserFactory.getParserCallback(dir);
		Assert.assertTrue(callback instanceof RDFParserDir);

		callback.doParse(filename, "http://example.org/#", dir, true, (triple, pos) ->
				Assert.assertTrue("triple " + triple + " wasn't excepted", excepted.remove(triple))
		);
	}
}
