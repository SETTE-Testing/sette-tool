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

import hu.bme.mit.sette.common.annotations.SetteIncludeCoverage;
import hu.bme.mit.sette.common.annotations.SetteNotSnippet;
import hu.bme.mit.sette.common.annotations.SetteRequiredStatementCoverage;
import hu.bme.mit.sette.common.annotations.SetteSnippetContainer;
import hu.bme.mit.sette.common.snippets.JavaVersion;

/**
 * Sample snippet container with Java 7 restriction and with snippets having included coverage, but
 * no inputs.
 */
@SetteSnippetContainer(category = "TS2", goal = "Snippets with function call",
        inputFactoryContainer = Void.class, requiredJavaVersion = JavaVersion.JAVA_7)
public final class TS1b_FunctionCallJava7 {
    private TS1b_FunctionCallJava7() {
        throw new UnsupportedOperationException("Static class");
    }

    @SetteRequiredStatementCoverage(value = 100)
    @SetteIncludeCoverage(classes = { TS1b_FunctionCallJava7.class },
            methods = { "notSnippet(int, int)" })
    public static int simpleCall(int x, int y) {
        return notSnippet(x, y);
    }

    @SetteRequiredStatementCoverage(value = 100)
    @SetteIncludeCoverage(classes = { TS1b_FunctionCallJava7.class },
            methods = { "notSnippet(int, int)" })
    public static int ifElseCall(int x, int y, boolean b) {
        if (b) {
            return notSnippet(x, y);
        } else {
            return -1;
        }
    }

    @SetteNotSnippet
    public static int notSnippet(int x, int y) {
        if (x > 0 && y > 0) {
            return 1;
        } else if (x < 0 && y > 0) {
            return 2;
        } else if (x < 0 && y < 0) {
            return 3;
        } else if (x > 0 && y < 0) {
            return 4;
        } else {
            return -1;
        }
    }
}
