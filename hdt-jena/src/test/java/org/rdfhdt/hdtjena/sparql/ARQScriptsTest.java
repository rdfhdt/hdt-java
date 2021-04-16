package org.rdfhdt.hdtjena.sparql;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rdfhdt.hdtjena.junit.ScriptTest;
import org.rdfhdt.hdtjena.junit.ScriptTestFactory;

import java.util.List;

/**
 * ARQ script-based tests copied from Apache Jena.
 */
@RunWith(JUnitParamsRunner.class)
public class ARQScriptsTest {
    private static final String testDir = "testing/ARQ";

    public List<ScriptTest> parametersForScriptTest() {
        ScriptTestFactory stf = new ScriptTestFactory("ARQ");
        List<ScriptTest> tests = stf.load(testDir + "/manifest-arq.ttl");

        if ( tests.isEmpty() ) {
            ScriptTest dummyTest = new ScriptTest() {
                @Override
                public void runTest() throws Throwable {}
            };
            tests.add(dummyTest);
        }
        return tests;

    }

    @Test
    @Parameters
    public void scriptTest(ScriptTest test) throws Throwable {
        test.runTest();
    }

    // At least one test!
    @Test
    public void dummy() {}
}
