/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.rdfhdt.hdtjena.junit;

import org.apache.jena.atlas.logging.Log;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.checker.CheckerLiterals;
import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.SystemARQ;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingMap;
import org.apache.jena.sparql.engine.iterator.QueryIterPlainWrapper;
import org.apache.jena.sparql.expr.E_Function;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeFunctions;
import org.apache.jena.sparql.junit.QueryTestException;
import org.apache.jena.sparql.junit.TestItem;
import org.apache.jena.sparql.resultset.ResultSetCompare;
import org.apache.jena.sparql.resultset.SPARQLResult;
import org.apache.jena.sparql.util.DatasetUtils;
import org.apache.jena.sparql.util.IsoMatcher;
import org.apache.jena.sparql.vocabulary.ResultSetGraphVocab;
import org.apache.jena.sparql.vocabulary.TestManifest;
import org.apache.jena.sparql.vocabulary.TestManifestX;
import org.apache.jena.util.FileUtils;
import org.apache.jena.vocabulary.RDF;
import org.rdfhdt.hdtjena.util.GraphConverter;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HDTQueryTest implements ScriptTest {
    private static int testCounter = 1;

    private final int testNumber = testCounter++;
    private final String testName;
    private final TestItem testItem;

    private SPARQLResult results;    // Maybe null if no testing of results

    // If supplied with a model, the test will load that model with data from the source
    // If no model is supplied one is created or attached (e.g. a database)

    public static boolean accepts(Resource testType) {
        return testType.equals(TestManifest.QueryEvaluationTest)
                || testType.equals(TestManifestX.TestQuery)
                || testType.equals(TestManifest.ReducedCardinalityTest);
    }

    public HDTQueryTest(String testName, TestItem testItem) {
        this.testName = Objects.requireNonNull(testName);
        this.testItem = Objects.requireNonNull(testItem);
    }

    @Override
    public void runTest() throws Throwable {
        setUp();
        try {
            Query query;
            try {
                query = queryFromTestItem(testItem);
            } catch (QueryException qEx) {
                qEx.printStackTrace(System.err);
                fail("Parse failure: " + qEx.getMessage());
                throw qEx;
            }

            Dataset dataset = setUpDataset(query, testItem);
            if (dataset == null && !query.hasDatasetDescription()) {
                fail("No dataset for query");
            }

            try (QueryExecution qe = (dataset == null)
                    ? QueryExecutionFactory.create(query)
                    : QueryExecutionFactory.create(query, dataset)) {
                if (query.isSelectType()) {
                    runTestSelect(query, qe);
                } else if (query.isConstructType()) {
                    runTestConstruct(query, qe);
                } else if (query.isDescribeType()) {
                    runTestDescribe(query, qe);
                } else if (query.isAskType()) {
                    runTestAsk(qe);
                }
            }
        } catch (IOException ioEx) {
            //log.debug("IOException: ",ioEx) ;
            fail("IOException: " + ioEx.getMessage());
            throw ioEx;
        } catch (NullPointerException ex) {
            throw ex;
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            fail("Exception: " + ex.getClass().getName() + ": " + ex.getMessage());
        } finally {
            tearDown();
        }
    }

    private void setUp() {
        // Switch warnings off for things that do occur in the scripted test suites
        NodeValue.VerboseWarnings = false;
        E_Function.WarnOnUnknownFunction = false;
        CheckerLiterals.WarnOnBadLiterals = false;

        // SPARQL and ARQ tests are done with no value matching (for query execution and results testing)
        SystemARQ.UsePlainGraph = true;

        // Sort results.
        results = testItem.getResults();
    }

    private void tearDown() {
        // Restore default settings
        NodeValue.VerboseWarnings = true;
        E_Function.WarnOnUnknownFunction = true;
        CheckerLiterals.WarnOnBadLiterals = true;
        SystemARQ.UsePlainGraph = false;
    }

    private Dataset setUpDataset(Query query, TestItem testItem) {
        try {
            if (query.hasDatasetDescription() && doesTestItemHaveDataset(testItem)) {
                // Only warn if there are results to test
                // Syntax tests may have FROM etc and a manifest data file.
                if (testItem.getResultFile() != null) {
                    Log.warn(this, testItem.getName() + " : query data source and also in test file");
                }
            }

            // In test file?
            if (doesTestItemHaveDataset(testItem)) {
                // Not specified in the query - get from test item and load
                return createDataset(testItem.getDefaultGraphURIs(), testItem.getNamedGraphURIs());
            }

            if (!query.hasDatasetDescription()) {
                fail("No dataset");
            }

            // Left to query
            return null;

        } catch (JenaException jEx) {
            fail("JenaException creating data source: " + jEx.getMessage());
            return null;
        }
    }

    private Query queryFromTestItem(TestItem testItem) {
        if (testItem.getQueryFile() == null) {
            fail("Query test file is null");
        }

        return QueryFactory.read(testItem.getQueryFile(), null, testItem.getFileSyntax());
    }

    private static boolean doesTestItemHaveDataset(TestItem testItem) {
        return (testItem.getDefaultGraphURIs() != null && testItem.getDefaultGraphURIs().size() > 0)
                ||
                (testItem.getNamedGraphURIs() != null && testItem.getNamedGraphURIs().size() > 0);
    }

    private static Dataset createDataset(List<String> defaultGraphURIs, List<String> namedGraphURIs) {
        return GraphConverter.toHDT(DatasetUtils.createDataset(defaultGraphURIs, namedGraphURIs, null));
    }

    private void runTestSelect(Query query, QueryExecution qe) {
        // Do the query!
        ResultSetRewindable resultsActual = ResultSetFactory.makeRewindable(qe.execSelect());

        qe.close();

        if (results == null) {
            return;
        }

        // Assumes resultSetCompare can cope with full isomorphism possibilities.
        ResultSetRewindable resultsExpected;
        if (results.isResultSet()) {
            resultsExpected = ResultSetFactory.makeRewindable(results.getResultSet());
        } else if (results.isModel()) {
            resultsExpected = ResultSetFactory.makeRewindable(results.getModel());
        } else {
            fail("Wrong result type for SELECT query");
            resultsExpected = null; // Keep the compiler happy
        }

        if (query.isReduced()) {
            // Reduced - best we can do is DISTINCT
            resultsExpected = unique(resultsExpected);
            resultsActual = unique(resultsActual);
        }

        // Hack for CSV : tests involving bNodes need manually checking.
        if (testItem.getResultFile().endsWith(".csv")) {
            resultsActual = convertToStrings(resultsActual);
            resultsActual.reset();

            int nActual = ResultSetFormatter.consume(resultsActual);
            int nExpected = ResultSetFormatter.consume(resultsExpected);
            resultsActual.reset();
            resultsExpected.reset();
            assertEquals("CSV: Different number of rows", nExpected, nActual);
            boolean b = resultSetEquivalent(query, resultsExpected, resultsActual);
            if (!b) {
                System.out.println("Manual check of CSV results required: " + testItem.getName());
            }
            return;
        }

        boolean b = resultSetEquivalent(query, resultsExpected, resultsActual);

        if (!b) {
            resultsExpected.reset();
            resultsActual.reset();
            resultSetEquivalent(query, resultsExpected, resultsActual);
            printFailedResultSetTest(query, resultsExpected, resultsActual);
        }
        assertTrue("Results do not match: " + testItem.getName(), b);
    }

    private ResultSetRewindable convertToStrings(ResultSetRewindable resultsActual) {
        List<Binding> bindings = new ArrayList<>();
        while (resultsActual.hasNext()) {
            Binding b = resultsActual.nextBinding();
            BindingMap b2 = BindingFactory.create();

            for (String vn : resultsActual.getResultVars()) {
                Var v = Var.alloc(vn);
                Node n = b.get(v);
                String s;
                if (n == null) {
                    s = "";
                } else if (n.isBlank()) {
                    s = "_:" + n.getBlankNodeLabel();
                } else {
                    s = NodeFunctions.str(n);
                }
                b2.add(v, NodeFactory.createLiteral(s));
            }
            bindings.add(b2);
        }
        ResultSet rs = new ResultSetStream(resultsActual.getResultVars(), null, new QueryIterPlainWrapper(bindings.iterator()));
        return ResultSetFactory.makeRewindable(rs);
    }

    private static ResultSetRewindable unique(ResultSetRewindable results) {
        // VERY crude.  Utilises the fact that bindings have value equality.
        List<Binding> x = new ArrayList<>();
        Set<Binding> seen = new HashSet<>();

        while (results.hasNext()) {
            Binding b = results.nextBinding();
            if (seen.contains(b)) {
                continue;
            }
            seen.add(b);
            x.add(b);
        }
        QueryIterator qIter = new QueryIterPlainWrapper(x.iterator());
        ResultSet rs = new ResultSetStream(results.getResultVars(), ModelFactory.createDefaultModel(), qIter);
        return ResultSetFactory.makeRewindable(rs);
    }

    private static boolean resultSetEquivalent(Query query, ResultSetRewindable resultsExpected, ResultSetRewindable resultsActual) {
        if (query.isOrdered()) {
            return ResultSetCompare.equalsByValueAndOrder(resultsExpected, resultsActual);
        } else {
            return ResultSetCompare.equalsByValue(resultsExpected, resultsActual);
        }
    }

    private void runTestConstruct(Query query, QueryExecution qe) {
        // Do the query!
        if (query.isConstructQuad()) {
            Dataset resultActual = qe.execConstructDataset();
            compareDatasetResults(resultActual, query);
        } else {
            Model resultsActual = qe.execConstruct();
            compareGraphResults(resultsActual, query);
        }
    }

    private void compareGraphResults(Model resultsActual, Query query) {
        if (results != null) {
            try {
                if (!results.isGraph()) {
                    fail("Expected results are not a graph: " + testItem.getName());
                }

                Model resultsExpected = results.getModel();
                if (!resultsExpected.isIsomorphicWith(resultsActual)) {
                    printFailedModelTest(resultsExpected, resultsActual);
                    fail("Results do not match: " + testItem.getName());
                }
            } catch (Exception ex) {
                String typeName = (query.isConstructType() ? "construct" : "describe");
                fail("Exception in result testing (" + typeName + "): " + ex);
            }
        }
    }

    private void compareDatasetResults(Dataset resultsActual, Query query) {
        if (results != null) {
            try {
                if (!results.isDataset()) {
                    fail("Expected results are not a graph: " + testItem.getName());
                }

                Dataset resultsExpected = results.getDataset();
                if (!IsoMatcher.isomorphic(resultsExpected.asDatasetGraph(), resultsActual.asDatasetGraph())) {
                    printFailedDatasetTest(resultsExpected, resultsActual);
                    fail("Results do not match: " + testItem.getName());
                }
            } catch (Exception ex) {
                String typeName = (query.isConstructType() ? "construct" : "describe");
                fail("Exception in result testing (" + typeName + "): " + ex);
            }
        }
    }

    private void runTestDescribe(Query query, QueryExecution qe) {
        Model resultsActual = qe.execDescribe();
        compareGraphResults(resultsActual, query);
    }

    private void runTestAsk(QueryExecution qe) throws Exception {
        boolean result = qe.execAsk();
        if (results != null) {
            if (results.isBoolean()) {
                boolean b = results.getBooleanResult();
                assertEquals("ASK test results do not match", b, result);
            } else {
                Model resultsAsModel = results.getModel();
                StmtIterator sIter = results.getModel().listStatements(null, RDF.type, ResultSetGraphVocab.ResultSet);
                if (!sIter.hasNext()) {
                    throw new QueryTestException("Can't find the ASK result");
                }
                Statement s = sIter.nextStatement();
                if (sIter.hasNext()) {
                    throw new QueryTestException("Too many result sets in ASK result");
                }
                Resource r = s.getSubject();
                Property p = resultsAsModel.createProperty(ResultSetGraphVocab.getURI() + "boolean");

                boolean x = r.getRequiredProperty(p).getBoolean();
                if (x != result) {
                    assertEquals("ASK test results do not match", x, result);
                }
            }
        }
    }

    private void printFailedResultSetTest(Query query, ResultSetRewindable qrExpected, ResultSetRewindable qrActual) {
        PrintStream out = System.out;
        out.println();
        out.println("=======================================");
        out.println("Failure: " + description());
        out.println("Query: \n" + query);
        out.println("Got: " + qrActual.size() + " --------------------------------");
        qrActual.reset();
        ResultSetFormatter.out(out, qrActual, query.getPrefixMapping());
        qrActual.reset();
        out.flush();


        out.println("Expected: " + qrExpected.size() + " -----------------------------");
        qrExpected.reset();
        ResultSetFormatter.out(out, qrExpected, query.getPrefixMapping());
        qrExpected.reset();

        out.println();
        out.flush();
    }

    private void printFailedModelTest(Model expected, Model results) {
        PrintWriter out = FileUtils.asPrintWriterUTF8(System.out);
        out.println("=======================================");
        out.println("Failure: " + description());
        results.write(out, "TTL");
        out.println("---------------------------------------");
        expected.write(out, "TTL");
        out.println();
    }

    private void printFailedDatasetTest(Dataset expected, Dataset results) {
        System.out.println("=======================================");
        System.out.println("Failure: " + description());
        RDFDataMgr.write(System.out, results, Lang.TRIG);
        System.out.println("---------------------------------------");
        RDFDataMgr.write(System.out, expected, Lang.TRIG);
        System.out.println();
    }

    @Override
    public String toString() {
        return testName;
    }

    // Cache
    private String description = null;

    private String description() {
        if (description == null) {
            description = makeDescription();
        }
        return description;
    }

    private String makeDescription() {
        String tmp = "";
        if (testItem.getDefaultGraphURIs() != null) {
            for (String s : testItem.getDefaultGraphURIs()) {
                tmp = tmp + s;
            }
        }
        if (testItem.getNamedGraphURIs() != null) {
            for (String s : testItem.getNamedGraphURIs()) {
                tmp = tmp + s;
            }
        }

        return "Test " + testNumber + " :: " + testItem.getName();
    }
}
