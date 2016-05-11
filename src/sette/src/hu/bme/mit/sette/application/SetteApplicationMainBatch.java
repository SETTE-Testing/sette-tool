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
// NOTE revise this file
package hu.bme.mit.sette.application;

import java.util.ArrayList;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

public final class SetteApplicationMainBatch {
    public static void main(String... args) {
        // FIXME perftime
        // runBatch("evosuite", "parser", "sette-snippets/java/sette-snippets-core",
        // new int[] { 30 }, 10);
        // runBatch("evosuite", "parser", "sette-snippets/java/sette-snippets-performance-time",
        // new int[] { 15, 60 }, 10);

        // FIXME extra
        int cnt = 10;
        runBatch("evosuite", "parser", "sette-snippets/java/sette-snippets-extra",
                new int[] { 30 }, cnt);
        runBatch("evosuite", "test-generator", "sette-snippets/java/sette-snippets-extra",
                new int[] { 30 }, cnt);
        runBatch("evosuite", "test-runner", "sette-snippets/java/sette-snippets-extra",
                new int[] { 30 }, cnt);
        runBatch("evosuite", "export-csv", "sette-snippets/java/sette-snippets-extra",
                new int[] { 30 }, cnt);

        // FIXME normal
        // runBatch("evosuite", "parser", "sette-snippets/java/sette-snippets-core",
        // new int[] { 30 }, 10);
        // runBatch("evosuite", "test-generator", "sette-snippets/java/sette-snippets-core",
        // new int[] { 30 }, 10);
        // runBatch("evosuite", "test-runner", "sette-snippets/java/sette-snippets-core",
        // new int[] { 30 }, 10);
        // runBatch("evosuite", "export-csv", "sette-snippets/java/sette-snippets-core",
        // new int[] { 30 }, 10);

        // FIXME EVO MUT PARSER
        // runBatch("evosuite", "parser-evosuite-mutation", "sette-snippets/java/sette-snippets-core",
        // new int[] { 30 }, 10);
    }

    private static void runBatch(String tool, String task, String project, int[] timeouts,
            int runs) {
        String argsBaseStr = "--tool " + tool + " "
                + "--snippet-project-dir " + project + " "
                + "--task " + task + " "
                // + "--snippet-selector Env4_manipulatesRandom "
                + "--runner-project-tag";

        ImmutableList<String> argsBase = ImmutableList.copyOf(Splitter.on(' ').split(argsBaseStr));

        for (int t : timeouts) {
            for (int run = 1; run <= runs; run++) {
                ArrayList<String> args = new ArrayList<>(argsBase);
                args.add(String.format("run-%02d-%02dsec", run, t));
                SetteApplicationMain.main(args.toArray(new String[0]));
            }
        }
    }

    private SetteApplicationMainBatch() {
        throw new UnsupportedOperationException("Static class");
    }
}
