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

package org.rdfhdt.hdtjena.sparql;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rdfhdt.hdtjena.junit.ScriptTest;
import org.rdfhdt.hdtjena.junit.ScriptTestFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * W3C Data Access Working Group tests (via Jena)
 */
@RunWith(JUnitParamsRunner.class)
public class DAWGScriptsTest {
    private static final String testDirDAWG = "testing/DAWG";
    private static final String testDirWGApproved = "testing/DAWG-Final";

    public List<ScriptTest> parametersForScriptTest() {
        List<ScriptTest> tests = new ArrayList<>();

        ScriptTestFactory stf1 = new ScriptTestFactory("Approved");
        tests.addAll(stf1.load(testDirWGApproved + "/manifest-evaluation.ttl"));

        ScriptTestFactory stf2 = new ScriptTestFactory("Misc");
        tests.addAll(stf2.load(testDirDAWG + "/Misc/manifest.n3"));
        tests.addAll(stf2.load(testDirDAWG + "/Syntax/manifest.n3"));
        tests.addAll(stf2.load(testDirDAWG + "/regex/manifest.n3"));
        tests.addAll(stf2.load(testDirDAWG + "/examples/manifest.n3"));

        return tests;
    }

    @Test
    @Parameters
    public void scriptTest(ScriptTest test) throws Throwable {
        test.runTest();
    }
}
