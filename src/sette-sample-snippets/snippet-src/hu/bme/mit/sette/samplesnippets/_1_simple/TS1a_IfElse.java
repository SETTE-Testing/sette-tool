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
package hu.bme.mit.sette.samplesnippets._1_simple;

import hu.bme.mit.sette.common.annotations.SetteRequiredStatementCoverage;
import hu.bme.mit.sette.common.annotations.SetteSnippetContainer;

/**
 * Sample snippet container with methods and inputs.
 */
@SetteSnippetContainer(category = "TS1", goal = "Snippets with if-else",
        inputFactoryContainer = TS1a_IfElse_Inputs.class)
public final class TS1a_IfElse {
    private TS1a_IfElse() {
        throw new UnsupportedOperationException("Static class");
    }

    @SetteRequiredStatementCoverage(value = 100)
    public static boolean linear(int x) {
        if (20 * x + 2 == 42) {
            return true;
        } else {
            return false;
        }
    }

    @SetteRequiredStatementCoverage(value = 50)
    public static boolean linearNoSolution(int x) {
        if (20 * x + 2 == 41) {
            return true;
        } else {
            return false;
        }
    }

    @SetteRequiredStatementCoverage(value = 100)
    public static int exception(int x) {
        if (x < 0) {
            throw new IndexOutOfBoundsException();
        } else {
            return x;
        }
    }

    @SetteRequiredStatementCoverage(value = 100)
    public static int array(int[] x) {
        if (x != null && x.length > 0) {
            if (x[0] == 1) {
                return 1;
            } else {
                return 0;
            }
        } else {
            return -1;
        }
    }
}
