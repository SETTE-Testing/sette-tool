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
// NOTE revise this file
// NOTE revise this file
package hu.bme.mit.sette.application;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hu.bme.mit.sette.core.tasks.RunnerProjectRunner;

public final class RunnerUI implements BaseUI {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void execute(ExecutionContext context) throws Exception {
        RunnerProjectRunner<?> runner = context.getTool().createRunnerProjectRunner(
                context.getSnippetProject(), context.getOutputDir(),
                context.getRunnerProjectTag());
        runner.setTimeoutInMs(context.getRunnerTimeoutInMs());
        log.info("Created {} for {} @ {} ms timeout", runner.getClass().getSimpleName(),
                runner.getRunnerProjectSettings().getProjectName(), runner.getTimeoutInMs());

        // directories
        File snippetProjectDir = runner.getSnippetProject().getBaseDir().toFile();
        File runnerProjectDir = runner.getRunnerProjectSettings().getBaseDir();

        context.getOutput().println("Snippet project: " + snippetProjectDir);
        context.getOutput().println("Runner project: " + runnerProjectDir);

        try {
            // run tool on code snippets
            context.getOutput().println("Clean up");
            runner.cleanUp();

            // TODO fixme
            // out.println("Press [Enter] to start execution");
            // in.readLine();

            context.getOutput().println("Clean up");
            runner.cleanUp();

            context.getOutput().println("Starting run");
            runner.run(context.getOutput());
            context.getOutput().println("Run successful");
        } catch (Exception ex) {
            context.getOutput().println("Run failed: " + ex.getMessage());
            ex.printStackTrace();
        }

    }
}
