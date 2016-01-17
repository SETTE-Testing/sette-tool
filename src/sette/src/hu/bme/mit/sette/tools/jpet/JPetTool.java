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
package hu.bme.mit.sette.tools.jpet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import hu.bme.mit.sette.common.snippets.JavaVersion;
import hu.bme.mit.sette.core.model.runner.RunnerProjectSettings;
import hu.bme.mit.sette.core.model.runner.RunnerProjectUtils;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tool.Tool;
import hu.bme.mit.sette.core.tool.ToolOutputType;
import hu.bme.mit.sette.core.validator.PathValidator;
import hu.bme.mit.sette.core.validator.ValidationException;
import lombok.Getter;

public final class JPetTool extends Tool {
    public static final String TESTCASES_DIRNAME = "pet-testcases";
    @Getter
    private final Path petExecutable;
    @Getter
    private final Path defaultBuildXml;

    public JPetTool(String name, Path dir) throws IOException, ValidationException {
        super(name, dir);

        petExecutable = dir.resolve("pet");
        defaultBuildXml = dir.resolve("sette-build.xml.default");

        PathValidator.forRegularFile(petExecutable, true, null, null, null);
        PathValidator.forRegularFile(defaultBuildXml, true, null, null, "default");
    }

    @Override
    public ToolOutputType getOutputType() {
        return ToolOutputType.INPUT_VALUES;
    }

    @Override
    public JavaVersion getSupportedJavaVersion() {
        return JavaVersion.JAVA_7;
    }

    @Override
    public JPetGenerator createRunnerProjectGenerator(SnippetProject snippetProject,
            Path outputDir, String runnerProjectTag) {
        return new JPetGenerator(snippetProject, outputDir, this, runnerProjectTag);
    }

    @Override
    public JPetRunner createRunnerProjectRunner(SnippetProject snippetProject, Path outputDir,
            String runnerProjectTag) {
        return new JPetRunner(snippetProject, outputDir, this, runnerProjectTag);
    }

    public static File getTestCasesDirectory(
            RunnerProjectSettings<JPetTool> runnerProjectSettings) {
        return new File(runnerProjectSettings.getBaseDir(), TESTCASES_DIRNAME);
    }

    public static File getTestCaseXmlFile(RunnerProjectSettings<JPetTool> runnerProjectSettings,
            Snippet snippet) {
        return new File(getTestCasesDirectory(runnerProjectSettings),
                RunnerProjectUtils.getSnippetBaseFilename(snippet) + ".xml");
    }

    @Override
    public JPetParser createRunResultParser(SnippetProject snippetProject, Path outputDir,
            String runnerProjectTag) {
        return new JPetParser(snippetProject, outputDir, this, runnerProjectTag);
    }
}
