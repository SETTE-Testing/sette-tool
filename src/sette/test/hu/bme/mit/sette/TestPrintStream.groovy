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
package hu.bme.mit.sette

import groovy.transform.TypeChecked

/**
 * A {@link PrintStream} implementation for testing which writes the data to its internal byte array. 
 */
@TypeChecked
public class TestPrintStream extends PrintStream {
    private final ByteArrayOutputStream data

    public TestPrintStream(ByteArrayOutputStream data = new ByteArrayOutputStream()) {
        // a little bit ugly Groovy "trick" but simple ctor call is possible
        super(data)
        this.data = data
    }

    public List<String> getLines() {
        if (data.size()) {
            // preserves empty lines except the ones at the end of the data (String.split(String))
            return data.toString().replace('\r\n', '\n').split('\n') as List<String>
        } else {
            return []
        }
    }
}
