package org.rdfhdt.hdtjena.graph;

import org.apache.jena.graph.Graph;
import org.apache.jena.query.Dataset;
import org.apache.jena.sparql.graph.TestGraphsMem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rdfhdt.hdtjena.HDTGraph;
import org.rdfhdt.hdtjena.util.GraphConverter;

/**
 * Basic Jena {@link Graph} operations for the {@link HDTGraph}.
 */
public class TestGraphsHDT extends TestGraphsMem {
    private Dataset dataset;

    @Before
    public void setUp() throws Exception {
        dataset = null;
    }

    @After
    public void tearDown() throws Exception {
        if (dataset != null) {
            dataset.close();
        }
    }

    @Override
    protected Dataset getDataset() {
        if (dataset == null) {
            dataset = GraphConverter.toHDT(super.getDataset());
        }
        return dataset;
    }

    @Override
    @Test
    public void graph_count7() {}  // HDT graphs are immutable
}
