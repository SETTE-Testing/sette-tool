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

import hu.bme.mit.sette.core.tasks.RunResultParser;
import hu.bme.mit.sette.core.validator.ValidationException;

public final class ParserUI implements BaseUI {
    @Override
    public void execute(ExecutionContext context) throws Exception {
        RunResultParser<?> parser = context.getTool().createRunResultParser(
                context.getSnippetProject(), context.getOutputDir(), context.getRunnerProjectTag());

        // directories
        File snippetProjectDir = parser.getSnippetProject().getBaseDir().toFile();
        File runnerProjectDir = parser.getRunnerProjectSettings().getBaseDir();

        context.getOutput().println("Snippet project: " + snippetProjectDir);
        context.getOutput().println("Runner project: " + runnerProjectDir);

        try {
            // TODO enhance this section
            parser.parse();
        } catch (Exception ex) {
            context.getOutput().println("Parse failed: " + ex.getMessage());

            if (ex instanceof ValidationException) {
                throw (ValidationException) ex;
            } else {
                ex.printStackTrace();
            }
        }
    }
}
