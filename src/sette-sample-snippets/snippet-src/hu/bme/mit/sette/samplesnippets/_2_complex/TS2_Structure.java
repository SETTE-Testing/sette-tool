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
package hu.bme.mit.sette.samplesnippets._2_complex;

import hu.bme.mit.sette.common.annotations.SetteRequiredStatementCoverage;
import hu.bme.mit.sette.common.annotations.SetteSnippetContainer;
import hu.bme.mit.sette.samplesnippets._2_complex.dependencies.CoordinateStructure;
import hu.bme.mit.sette.samplesnippets._2_complex.dependencies.TS2_Structure_Inputs;

@SetteSnippetContainer(category = "TS2", goal = "Sample snippet container",
        inputFactoryContainer = TS2_Structure_Inputs.class)
public final class TS2_Structure {
    private TS2_Structure() {
        throw new UnsupportedOperationException("Static class");
    }

    @SetteRequiredStatementCoverage(value = 100)
    public static int guessParams(int x, int y) {
        CoordinateStructure c = new CoordinateStructure();

        c.x = x;
        c.y = y;

        if (c.x > 0 && c.y > 0) {
            return 1;
        } else if (c.x < 0 && c.y > 0) {
            return 2;
        } else if (c.x < 0 && c.y < 0) {
            return 3;
        } else if (c.x > 0 && c.y < 0) {
            return 4;
        } else {
            return -1;
        }
    }

    @SetteRequiredStatementCoverage(value = 100)
    public static int guess(CoordinateStructure c) {
        if (c == null) {
            return 0;
        }

        if (c.x > 0 && c.y > 0) {
            return 1;
        } else if (c.x < 0 && c.y > 0) {
            return 2;
        } else if (c.x < 0 && c.y < 0) {
            return 3;
        } else if (c.x > 0 && c.y < 0) {
            return 4;
        } else {
            return -1;
        }
    }
}
