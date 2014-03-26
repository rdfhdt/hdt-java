package org.rdfhdt.hdtjena;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class NodeDictionaryTest {

	@Test
	public void testLang() {
		RDFNode object = ResourceFactory.createLangLiteral("Hello", "en");
		String s = NodeDictionary.nodeToStr(object.asNode());
		System.out.println(s);
		assertEquals("\"Hello\"@en", s);
	}

	@Test
	public void testNormal() {
		RDFNode object = ResourceFactory.createPlainLiteral("Hello");
		String s = NodeDictionary.nodeToStr(object.asNode());
		System.out.println(s);
		assertEquals("\"Hello\"", s);
	}

	@Test
	public void testTyped() {
		System.out.println();
		XSDDatatype type = XSDDatatype.XSDnonNegativeInteger;
		RDFNode object = ResourceFactory.createTypedLiteral("2", type);
		String s = NodeDictionary.nodeToStr(object.asNode());
		System.out.println(s);
		assertEquals("\"2\"^^<http://www.w3.org/2001/XMLSchema#nonNegativeInteger>", s);
	}

}
