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
package hu.bme.mit.sette.tools.randoop;

import hu.bme.mit.sette.common.model.parserxml.SnippetInputsXml;
import hu.bme.mit.sette.common.model.runner.ResultType;
import hu.bme.mit.sette.common.model.runner.RunnerProjectUtils;
import hu.bme.mit.sette.common.model.snippet.Snippet;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;
import hu.bme.mit.sette.common.tasks.RunResultParser;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class RandoopParser extends RunResultParser<RandoopTool> {
    public RandoopParser(SnippetProject snippetProject, File outputDirectory, RandoopTool tool,
            String runnerProjectTag) {
        super(snippetProject, outputDirectory, tool, runnerProjectTag);
    }

    @Override
    protected void parseSnippet(Snippet snippet, SnippetInputsXml inputsXml) throws Exception {
        File outputFile = RunnerProjectUtils.getSnippetOutputFile(getRunnerProjectSettings(),
                snippet);
        File errorFile = RunnerProjectUtils.getSnippetErrorFile(getRunnerProjectSettings(),
                snippet);

        if (!outputFile.exists()) {
            // TODO
            throw new RuntimeException("TODO parser problem");
        }

        if (errorFile.exists()) {
            List<String> lines = FileUtils.readLines(errorFile);
            String firstLine = lines.get(0);

            if (firstLine.startsWith("java.io.FileNotFoundException:")
                    && firstLine.endsWith("_Test/Test.java (No such file or directory)")) {
                // this means that no input was generated but the generation
                // was successful

                if (snippet.getRequiredStatementCoverage() == 0
                        || snippet.getMethod().getParameterCount() == 0) {
                    // C only if the required statement coverage is 0% or
                    // the method takes no parameters
                    inputsXml.setResultType(ResultType.C);
                } else {
                    inputsXml.setResultType(ResultType.NC);
                }
            } else if (firstLine.startsWith("java.lang.Error: classForName")) {
                // exception, no output that not supported -> EX
                inputsXml.setResultType(ResultType.EX);
            } else {
                // TODO
                throw new RuntimeException("TODO parser problem");
            }
        }

        if (inputsXml.getResultType() == null) {
            // always S for Randoop
            inputsXml.setResultType(ResultType.S);
        }

        // do not parse inputs
        inputsXml.setGeneratedInputs(null);
        inputsXml.validate();
    }
}
