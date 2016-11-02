package org.rdfhdt.hdtjena.graph;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.TestDatasetGraphBaseFindPattern_Mem;
import org.apache.jena.sparql.sse.SSE;
import org.junit.Test;
import org.rdfhdt.hdtjena.util.GraphConverter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.apache.jena.atlas.junit.BaseTest.assertEqualsUnordered;

public class TestDatasetGraphBaseFindPattern_HDT extends TestDatasetGraphBaseFindPattern_Mem {

    @Override
    protected DatasetGraph create(Collection<Quad> data) {
        return GraphConverter.toHDT(super.create(data));
    }

    @Test
    public void find_pattern_p_nomatch() {
        DatasetGraph dsg = create(Collections.singletonList(SSE.parseQuad("(:g :s :p :o)")));
        List<Quad> quads1 = Iter.toList(dsg.find(null, null, SSE.parseNode(":px"), null));
        assertEqualsUnordered("find(p)", quads1, Collections.emptyList());
    }
}
