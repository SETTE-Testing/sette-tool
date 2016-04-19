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
package hu.bme.mit.sette.tools.snippetinputchecker;

import java.nio.file.Path;

import hu.bme.mit.sette.common.snippets.JavaVersion;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tool.Tool;
import hu.bme.mit.sette.core.tool.ToolOutputType;
import hu.bme.mit.sette.core.validator.ValidationException;

/**
 * Tool for checking snippet inputs against required coverage descriptors.
 */
public final class SnippetInputCheckerTool extends Tool {

    public SnippetInputCheckerTool(String name, Path toolDir) throws ValidationException {
        super(name, toolDir);
    }

    @Override
    public ToolOutputType getOutputType() {
        return ToolOutputType.JUNIT4_TEST_CASES;
    }

    @Override
    public JavaVersion getSupportedJavaVersion() {
        return JavaVersion.JAVA_8;
    }

    @Override
    public SnippetInputCheckerGenerator createRunnerProjectGenerator(SnippetProject snippetProject,
            Path outputDir, String runnerProjectTag) {
        return new SnippetInputCheckerGenerator(snippetProject, outputDir, this, runnerProjectTag);
    }

    @Override
    public SnippetInputCheckerRunner createRunnerProjectRunner(SnippetProject snippetProject,
            Path outputDir, String runnerProjectTag) {
        return new SnippetInputCheckerRunner(snippetProject, outputDir, this, runnerProjectTag);
    }

    @Override
    public SnippetInputCheckerParser createRunResultParser(SnippetProject snippetProject,
            Path outputDir,
            String runnerProjectTag) {
        return new SnippetInputCheckerParser(snippetProject, outputDir, this, runnerProjectTag);
    }
}
