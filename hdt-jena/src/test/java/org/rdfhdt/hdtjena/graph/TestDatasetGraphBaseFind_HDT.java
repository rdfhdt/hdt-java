package org.rdfhdt.hdtjena.graph;

import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.TestDatasetGraphBaseFind_Mem;
import org.rdfhdt.hdtjena.util.GraphConverter;

import java.util.Collection;

public class TestDatasetGraphBaseFind_HDT extends TestDatasetGraphBaseFind_Mem {

    @Override
    protected DatasetGraph create(Collection<Quad> data) {
        return GraphConverter.toHDT(super.create(data));
    }
}
