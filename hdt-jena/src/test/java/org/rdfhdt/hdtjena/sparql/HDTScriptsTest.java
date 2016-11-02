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

import java.util.List;

/**
 * HDT-specific script-based tests.
 */
@RunWith(JUnitParamsRunner.class)
public class HDTScriptsTest {
    private static final String testDir = "testing/HDT";

    public List<ScriptTest> parametersForScriptTest() {
        ScriptTestFactory stf = new ScriptTestFactory("HDT");
        return stf.load(testDir + "/Query/manifest.ttl");
    }

    @Test
    @Parameters
    public void scriptTest(ScriptTest test) throws Throwable {
        test.runTest();
    }
}
