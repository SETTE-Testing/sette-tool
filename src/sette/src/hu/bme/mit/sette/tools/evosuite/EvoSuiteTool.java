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
// TODO z revise this file
package hu.bme.mit.sette.tools.evosuite;

import hu.bme.mit.sette.common.Tool;
import hu.bme.mit.sette.common.ToolOutputType;
import hu.bme.mit.sette.common.exceptions.ConfigurationException;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;
import hu.bme.mit.sette.common.snippets.JavaVersion;
import hu.bme.mit.sette.common.validator.FileType;
import hu.bme.mit.sette.common.validator.FileValidator;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;

import java.io.File;

public final class EvoSuiteTool extends Tool {
    private final File toolJAR;
    private final File defaultBuildXml;

    public EvoSuiteTool(File toolJAR, File defaultBuildXml, String version)
            throws ConfigurationException {
        super("EvoSuite", "EvoSuite", version);
        this.toolJAR = toolJAR;
        this.defaultBuildXml = defaultBuildXml;

        // validate
        getToolJAR();
        getDefaultBuildXml();
    }

    public File getToolJAR() throws ConfigurationException {
        try {
            FileValidator v = new FileValidator(toolJAR);
            v.type(FileType.REGULAR_FILE).readable(true);
            v.validate();
        } catch (ValidatorException e) {
            throw new ConfigurationException("The EvoSuite JAR is invalid: " + toolJAR, e);
        }

        return toolJAR;
    }

    public File getDefaultBuildXml() throws ConfigurationException {
        try {
            FileValidator v = new FileValidator(defaultBuildXml);
            v.type(FileType.REGULAR_FILE).readable(true);
            v.validate();
        } catch (ValidatorException e) {
            throw new ConfigurationException(
                    "The default EvoSuite build.xml is invalid: " + defaultBuildXml, e);
        }

        return defaultBuildXml;
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
    public EvoSuiteGenerator createRunnerProjectGenerator(SnippetProject snippetProject,
            File outputDirectory) {
        return new EvoSuiteGenerator(snippetProject, outputDirectory, this);
    }

    @Override
    public EvoSuiteRunner createRunnerProjectRunner(SnippetProject snippetProject,
            File outputDirectory) {
        return new EvoSuiteRunner(snippetProject, outputDirectory, this);
    }

    @Override
    public EvoSuiteParser createRunResultParser(SnippetProject snippetProject,
            File outputDirectory) {
        return new EvoSuiteParser(snippetProject, outputDirectory, this);
    }
}
