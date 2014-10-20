/*
 * SETTE - Symbolic Execution based Test Tool Evaluator
 *
 * SETTE is a tool to help the evaluation and comparison of symbolic execution
 * based test input generator tools.
 *
 * Budapest University of Technology and Economics (BME)
 *
 * Authors: Lajos Cseppentő <lajos.cseppento@inf.mit.bme.hu>, Zoltán Micskei
 * <micskeiz@mit.bme.hu>
 *
 * Copyright 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package hu.bme.mit.sette;

import hu.bme.mit.sette.common.Tool;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;
import hu.bme.mit.sette.common.tasks.RunnerProjectRunner;
import hu.bme.mit.sette.run.Run;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;

import org.apache.commons.lang3.Validate;

public final class RunnerUI {
    private final RunnerProjectRunner<?> runner;

    public RunnerUI(SnippetProject snippetProject, Tool tool) {
        Validate.notNull(snippetProject,
                "Snippet project settings must not be null");
        Validate.notNull(tool, "The tool must not be null");
        runner = tool.createRunnerProjectRunner(snippetProject,
                Run.OUTPUT_DIR);
    }

    public void run(BufferedReader in, PrintStream out)
            throws Exception {
        // directories
        File snippetProjectDir = runner.getSnippetProjectSettings()
                .getBaseDirectory();
        File runnerProjectDir = runner.getRunnerProjectSettings()
                .getBaseDirectory();

        out.println("Snippet project: " + snippetProjectDir);
        out.println("Runner project: " + runnerProjectDir);

        try {
            // run tool on code snippets
            out.println("Clean up");
            runner.cleanUp();

            out.println("Press [Enter] to start execution");
            in.readLine();

            out.println("Clean up");
            runner.cleanUp();

            out.println("Starting run");
            runner.run(out);
            out.println("Run successful");
        } catch (Exception e) {
            out.println("Run failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
