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
// NOTE revise this file
// NOTE revise this file
// NOTE revise this file
package hu.bme.mit.sette;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;

import org.apache.commons.lang3.Validate;

import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tasks.RunResultParser;
import hu.bme.mit.sette.core.tool.Tool;
import hu.bme.mit.sette.core.validator.ValidationException;
import hu.bme.mit.sette.run.Run;

public final class ParserUI implements BaseUI {
    private final RunResultParser<?> parser;

    public ParserUI(SnippetProject snippetProject, Tool tool, String runnerProjectTag) {
        Validate.notNull(snippetProject, "Snippet project settings must not be null");
        Validate.notNull(tool, "The tool must not be null");
        parser = tool.createRunResultParser(snippetProject, Run.OUTPUT_DIR, runnerProjectTag);
    }

    @Override
    public void run(BufferedReader in, PrintStream out) throws Exception {
        // directories
        File snippetProjectDir = parser.getSnippetProject().getBaseDir().toFile();
        File runnerProjectDir = parser.getRunnerProjectSettings().getBaseDir();

        out.println("Snippet project: " + snippetProjectDir);
        out.println("Runner project: " + runnerProjectDir);

        try {
            // TODO enhance this section
            parser.parse();
        } catch (Exception ex) {
            out.println("Parse failed: " + ex.getMessage());

            if (ex instanceof ValidationException) {
                throw (ValidationException) ex;
            } else {
                ex.printStackTrace();
            }
        }
    }
}
