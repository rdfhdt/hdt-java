package org.rdfhdt.hdtjena;

import static com.hp.hpl.jena.sparql.util.graph.GraphUtils.getStringValue;

import java.io.IOException;

import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTFactory;
import org.rdfhdt.hdt.hdt.HDTManager;

import com.hp.hpl.jena.assembler.Assembler;
import com.hp.hpl.jena.assembler.Mode;
import com.hp.hpl.jena.assembler.assemblers.AssemblerBase;
import com.hp.hpl.jena.assembler.exceptions.AssemblerException;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.impl.ModelCom;

public class HDTGraphAssembler extends AssemblerBase implements Assembler {
	private static final String NS = "http://www.rdfhdt.org/joseki#" ;
	public static final Resource tGraphHDT        = ResourceFactory.createResource(NS+"HDTGraph") ;
	public static final Property pFileName          = ResourceFactory.createProperty(NS+"fileName");

	private static boolean initialized = false;

	public static void init() {
		if(initialized) {
			return;
		}

		initialized = true;
		System.out.println("HDTGraphAssembler initialized");

		Assembler.general.implementWith(tGraphHDT, new HDTGraphAssembler());
	}

	@Override
	public Model open(Assembler a, Resource root, Mode mode)
	{
		String file = getStringValue(root, pFileName) ;
		try {
			HDT hdt = HDTManager.loadIndexedHDT(file, null);
			HDTGraph graph = new HDTGraph(hdt);
			return new ModelCom(graph);
		} catch (IOException e) {
			throw new AssemblerException(root, "Error reading HDT file: "+file);
		}
	}
}
