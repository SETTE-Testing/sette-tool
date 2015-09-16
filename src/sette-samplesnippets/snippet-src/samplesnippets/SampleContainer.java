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

import hu.bme.mit.sette.annotations.SetteIncludeCoverage;
import hu.bme.mit.sette.annotations.SetteNotSnippet;
import hu.bme.mit.sette.annotations.SetteRequiredStatementCoverage;
import hu.bme.mit.sette.annotations.SetteSnippetContainer;
import samplesnippets.inputs.SampleContainer_Inputs;

@SetteSnippetContainer(category = "X1", goal = "Sample snippet container",
        inputFactoryContainer = SampleContainer_Inputs.class)
public final class SampleContainer {
    private SampleContainer() {
        throw new UnsupportedOperationException("Static class");
    }

    @SetteRequiredStatementCoverage(value = 100)
    public static boolean snippet1(int x) {
        if (20 * x + 2 == 42) {
            return true;
        } else {
            return false;
        }
    }

    @SetteRequiredStatementCoverage(value = 50)
    public static boolean snippet2(int x) {
        if (20 * x + 2 == 41) {
            return true;
        } else {
            return false;
        }
    }

    @SetteRequiredStatementCoverage(value = 100)
    @SetteIncludeCoverage(classes = { SampleContainer.class }, methods = { "notSnippet(int, int)" })
    public static int snippet3(int x, int y) {
        return SampleContainer.notSnippet(x, y);
    }

    @SetteRequiredStatementCoverage(value = 100)
    public static int snippet4(int x) {
        if (x < 0) {
            throw new IndexOutOfBoundsException();
        } else {
            return x;
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

    @SetteRequiredStatementCoverage(value = 100)
    public static void timeout(int x) {
        while (true) {
        }
    }

    @SetteRequiredStatementCoverage(value = 100)
    public static int array(int[] x) {
        if (x != null && x.length > 0) {
            if (x[0] == 1) {
                return 123;
            } else {
                return 0;
            }
        } else {
            return -1;
        }
    }

    @SetteRequiredStatementCoverage(value = 100)
    public static int oneStructureParams(int x, int y) {
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
    public static int oneStructure(CoordinateStructure c) {
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
