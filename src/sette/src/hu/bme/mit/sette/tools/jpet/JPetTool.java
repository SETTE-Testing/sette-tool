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
package hu.bme.mit.sette.tools.jpet;

import hu.bme.mit.sette.common.Tool;
import hu.bme.mit.sette.common.ToolOutputType;
import hu.bme.mit.sette.common.exceptions.ConfigurationException;
import hu.bme.mit.sette.common.model.runner.RunnerProjectSettings;
import hu.bme.mit.sette.common.model.runner.RunnerProjectUtils;
import hu.bme.mit.sette.common.model.snippet.Snippet;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;
import hu.bme.mit.sette.common.snippets.JavaVersion;
import hu.bme.mit.sette.common.util.JavaFileUtils;
import hu.bme.mit.sette.common.validator.FileType;
import hu.bme.mit.sette.common.validator.FileValidator;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;

import java.io.File;

public final class JPetTool extends Tool {
    public static final String TESTCASES_DIRNAME = "pet-testcases";
    private final File petExecutable;
    private final File defaultBuildXml;

    public JPetTool(File petExecutable, File defaultBuildXml, String version)
            throws ConfigurationException {
        super("jPET", null, version);
        this.petExecutable = petExecutable;
        this.defaultBuildXml = defaultBuildXml;

        // validate
        getPetExecutable();
        getDefaultBuildXml();
    }

    public File getPetExecutable() throws ConfigurationException {
        try {
            FileValidator v = new FileValidator(petExecutable);
            v.type(FileType.REGULAR_FILE).readable(true).executable(true);
            v.validate();
        } catch (ValidatorException ex) {
            throw new ConfigurationException("The jPET executable is invalid: " + petExecutable,
                    ex);
        }

        return petExecutable;
    }

    public File getDefaultBuildXml() throws ConfigurationException {
        try {
            FileValidator v = new FileValidator(defaultBuildXml);
            v.type(FileType.REGULAR_FILE).readable(true);
            v.validate();
        } catch (ValidatorException ex) {
            throw new ConfigurationException("The jPET build.xml is invalid: " + defaultBuildXml,
                    ex);
        }

        return defaultBuildXml;
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
            File outputDirectory, String runnerProjectTag) {
        return new JPetGenerator(snippetProject, outputDirectory, this, runnerProjectTag);
    }

    @Override
    public JPetRunner createRunnerProjectRunner(SnippetProject snippetProject, File outputDirectory,
            String runnerProjectTag) {
        return new JPetRunner(snippetProject, outputDirectory, this, runnerProjectTag);
    }

    public static File getTestCasesDirectory(
            RunnerProjectSettings<JPetTool> runnerProjectSettings) {
        return new File(runnerProjectSettings.getBaseDirectory(), TESTCASES_DIRNAME);
    }

    public static File getTestCaseXmlFile(RunnerProjectSettings<JPetTool> runnerProjectSettings,
            Snippet snippet) {
        return new File(getTestCasesDirectory(runnerProjectSettings),
                RunnerProjectUtils.getSnippetBaseFilename(snippet)
                        + JavaFileUtils.FILE_EXTENSION_SEPARATOR + "xml");
    }

    @Override
    public JPetParser createRunResultParser(SnippetProject snippetProject, File outputDirectory,
            String runnerProjectTag) {
        return new JPetParser(snippetProject, outputDirectory, this, runnerProjectTag);
    }

}
