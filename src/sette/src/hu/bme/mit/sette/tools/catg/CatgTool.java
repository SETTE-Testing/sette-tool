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
package hu.bme.mit.sette.tools.catg;

import java.nio.file.Path;

import hu.bme.mit.sette.common.snippets.JavaVersion;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tool.Tool;
import hu.bme.mit.sette.core.tool.ToolOutputType;
import hu.bme.mit.sette.core.validator.ValidationException;

public final class CatgTool extends Tool {
    public CatgTool(String name, Path dir) throws ValidationException {
        super(name, dir);
    }

    @Override
    public ToolOutputType getOutputType() {
        return ToolOutputType.INPUT_VALUES;
    }

    @Override
    public JavaVersion getSupportedJavaVersion() {
        return JavaVersion.JAVA_6;
    }

    @Override
    public CatgGenerator createRunnerProjectGenerator(SnippetProject snippetProject,
            Path outputDir, String runnerProjectTag) {
        return new CatgGenerator(snippetProject, outputDir, this, runnerProjectTag);
    }

    @Override
    public CatgRunner createRunnerProjectRunner(SnippetProject snippetProject, Path outputDir,
            String runnerProjectTag) {
        return new CatgRunner(snippetProject, outputDir, this, runnerProjectTag);
    }

    @Override
    public CatgParser createRunResultParser(SnippetProject snippetProject, Path outputDir,
            String runnerProjectTag) {
        return new CatgParser(snippetProject, outputDir, this, runnerProjectTag);
    }
}
