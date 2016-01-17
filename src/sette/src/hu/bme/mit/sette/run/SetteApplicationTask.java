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
package hu.bme.mit.sette.run;

/**
 * Tasks (commands) for the SETTE.  
 */
public enum SetteApplicationTask {
    EXIT("exit"),
    
    // all requires snippet-project
    GENERATOR("generator"), // requires tool & tag
    RUNNER("runner"), // requires tool & tag
    PARSER("parser"), // requires tool & tag
    TEST_GENERATOR("test-generator"), // requires tool & tag
    TEST_RUNNER("test-runner"), // requires tool & tag
    SNIPPET_BROWSER("snippet-browser"),
    EXPORT_CSV("export-csv"), // requires tool & tag
    EXPORT_CSV_BATCH("export-csv-batch");

    private final String name;

    private SetteApplicationTask(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
