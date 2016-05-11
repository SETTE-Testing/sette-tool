/*
 * SETTE - Symbolic Execution based Test Tool Evaluator
 *
 * SETTE is a tool to help the evaluation and comparison of symbolic execution based test input 
 * generator tools.
 *
 * Budapest University of Technology and Economics (BME)
 *
 * Authors: Lajos Cseppentő <lajos.cseppento@inf.mit.bme.hu>, Zoltán Micskei <micskeiz@mit.bme.hu>
 *
 * Copyright 2014-2016
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except 
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the 
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either 
 * express or implied. See the License for the specific language governing permissions and 
 * limitations under the License.
 */
package hu.bme.mit.sette;

import hu.bme.mit.sette.core.tasks.testsuiterunner.TestSuiteRunnerForkAgent;

public class TestRunnerAgentTest {
    public static void main(String[] args) {
        String a = "D:/SETTE/sette-snippets/java/sette-snippets-core ";
        a += "D:/SETTE/sette-results ";
        a += "hu.bme.mit.sette.tools.randoop.RandoopTool|Randoop|D:/SETTE/sette-tool/test-generator-tools/randoop ";
        a += "run-01-30sec ";
        a += "O1_fullCoverage ";
        a += "hu.bme.mit.sette.snippets._3_objects.O1_Simple_fullCoverage_Test.RegressionTest2 ";
        a += "test003";

        a = "D:/SETTE/sette-snippets/java/sette-snippets-extra D:/SETTE/sette-results/sette-snippets-extra___randoop___run-01-30sec hu.bme.mit.sette.tools.randoop.RandoopTool|Randoop|D:/SETTE/sette-tool/test-generator-tools/randoop run-01-30sec Env2_createDir hu.bme.mit.sette.snippets._7_environment.Env2_FileIO_createDir_Test.RegressionTest0 test1";
        
        TestSuiteRunnerForkAgent.main(a.split(" "));
    }
}
