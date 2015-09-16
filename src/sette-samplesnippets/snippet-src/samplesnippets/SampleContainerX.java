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
 * Copyright 2014-2015
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
// TODO revise this file
// TODO revise this file
package samplesnippets;

import hu.bme.mit.sette.annotations.SetteRequiredStatementCoverage;
import hu.bme.mit.sette.annotations.SetteSnippetContainer;
import samplesnippets.inputs.SampleContainerX_Inputs;

@SetteSnippetContainer(category = "X2",
        goal = "Sample snippet container 1",
        inputFactoryContainer = SampleContainerX_Inputs.class)
public final class SampleContainerX {
    private SampleContainerX() {
        throw new UnsupportedOperationException("Static class");
    }

    @SetteRequiredStatementCoverage(value = 100)
    public static int snippet1(int x) {
        if (x == 12345) {
            return 2;
        }

        if (x > 0) {
            return 1;
        } else if (x < 0) {
            return -1;
        } else {
            return 0;
        }
    }

    @SetteRequiredStatementCoverage(value = 8.0 / 12.0 * 100)
    public static int snippet2(int x, int y) {
        if (x > 0 && y > 0) {
            return 1;
        } else if (x < 0 && y > 0) {
            return 2;
        } else if (x < 0 && y < 0) {
            return 3;
        } else if (x > 0 && y < 0) {
            return 4;
        } else if (x > 0 && y < 0) {
            // impossible branch (the previous is the same)
            return -1;
        } else if (x == 0 || y == 0) {
            return 0;
        } else {
            // impossible branch
            return -2;
        }
    }

}
