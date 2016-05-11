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
package hu.bme.mit.sette.tools.randoop;

import java.nio.file.Path;

import hu.bme.mit.sette.common.snippets.JavaVersion;
import hu.bme.mit.sette.core.model.runner.RunnerProject;
import hu.bme.mit.sette.core.tool.Tool;
import hu.bme.mit.sette.core.tool.ToolOutputType;
import hu.bme.mit.sette.core.validator.PathValidator;
import hu.bme.mit.sette.core.validator.ValidationException;
import lombok.Getter;

public final class RandoopTool extends Tool {
    @Getter
    private final Path toolJar;
    @Getter
    private final Path defaultBuildXml;

    public RandoopTool(String name, Path dir) throws ValidationException {
        super(name, dir);

        toolJar = dir.resolve("randoop.jar");
        defaultBuildXml = dir.resolve("sette-build.xml.default");

        PathValidator.forRegularFile(toolJar, true, null, null, "jar");
        PathValidator.forRegularFile(defaultBuildXml, true, null, null, "default");
    }

    @Override
    public ToolOutputType getOutputType() {
        return ToolOutputType.JUNIT3_TEST_CASES;
    }

    @Override
    public JavaVersion getSupportedJavaVersion() {
        return JavaVersion.JAVA_7;
    }

    @Override
    public RandoopGenerator createRunnerProjectGenerator(RunnerProject runnerProject) {
        return new RandoopGenerator(runnerProject, this);
    }

    @Override
    public RandoopRunner createRunnerProjectRunner(RunnerProject runnerProject) {
        return new RandoopRunner(runnerProject, this);
    }

    @Override
    public RandoopParser createRunResultParser(RunnerProject runnerProject) {
        return new RandoopParser(runnerProject, this);
    }
}
