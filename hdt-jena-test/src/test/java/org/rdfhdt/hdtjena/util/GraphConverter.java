package org.rdfhdt.hdtjena.util;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.rdf.parsers.JenaModelIterator;
import org.rdfhdt.hdtjena.HDTGraph;

import java.io.IOException;
import java.util.Iterator;

public class GraphConverter {

    public static Dataset toHDT(Dataset dataset) {
        return DatasetFactory.wrap(toHDT(dataset.asDatasetGraph()));
    }

    public static DatasetGraph toHDT(DatasetGraph dsg) {
        DatasetGraph result = DatasetGraphFactory.create(toHDT(dsg.getDefaultGraph()));
        for (Iterator<Node> names = dsg.listGraphNodes(); names.hasNext(); ) {
            Node name = names.next();
            result.addGraph(name, toHDT(dsg.getGraph(name)));
        }
        return result;
    }

    private static Model toHDT(Model model) {
        HDT hdt;
        try {
            hdt = HDTManager.generateHDT(new JenaModelIterator(model), "http://example.com", new HDTSpecification(), null);
        } catch (IOException | ParserException e) {
            throw new RuntimeException(e);
        }
        return ModelFactory.createModelForGraph(new HDTGraph(hdt, true));
    }

    private static Graph toHDT(Graph graph) {
        return toHDT(ModelFactory.createModelForGraph(graph)).getGraph();
    }
}
