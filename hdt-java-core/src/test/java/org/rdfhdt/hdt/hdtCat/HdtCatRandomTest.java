package org.rdfhdt.hdt.hdtCat;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.hdtDiff.HdtDiffTest;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.util.LargeFakeDataSetStreamSupplier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class HdtCatRandomTest {
	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> genParam() {
		List<Object[]> list = new ArrayList<>();
		for (HdtDiffTest.DictionaryTestData data : HdtDiffTest.DICTIONARY_TEST_DATA) {
			list.add(new Object[]{data.dictionaryType, data.dictionaryTempType});
		}
		return list;
	}


	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();
	private final HDTSpecification spec;

	public HdtCatRandomTest(String dictionaryType, String tempDictionaryImpl) {
		spec = new HDTSpecification();
		spec.set("dictionary.type", dictionaryType);
		spec.set("tempDictionary.impl", tempDictionaryImpl);
	}

	@Test
	@Ignore("large")
	public void largeFakeTest() throws ParserException, IOException {
		File root = tempDir.newFolder();
		String location = new File(root, "catHdt").getAbsolutePath();
		String hdt1F = new File(root, "hdt1").getAbsolutePath();
		String hdt2F = new File(root, "hdt2").getAbsolutePath();
		String catOutput = new File(root, "catResult").getAbsolutePath();

		LargeFakeDataSetStreamSupplier supplier = LargeFakeDataSetStreamSupplier.createSupplierWithMaxTriples(1_000_000, 484);
		supplier.maxFakeType = 4;
		supplier.maxElementSplit = 1000;
		supplier.createAndSaveFakeHDT(spec, hdt1F);
		supplier.createAndSaveFakeHDT(spec, hdt2F);

		HDT cat = HDTManager.catHDT(location, hdt1F, hdt2F, spec, null);
		cat.saveToHDT(catOutput, null);
		cat.close();

		HDT loadedHDT = HDTManager.loadIndexedHDT(catOutput, null, spec);
		loadedHDT.close();

		HDT mappedHDT = HDTManager.mapIndexedHDT(catOutput, spec, null);
		mappedHDT.close();
	}

}
