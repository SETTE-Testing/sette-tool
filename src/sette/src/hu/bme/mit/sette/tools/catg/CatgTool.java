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
package hu.bme.mit.sette.tools.catg;

import java.io.File;

import hu.bme.mit.sette.common.Tool;
import hu.bme.mit.sette.common.ToolOutputType;
import hu.bme.mit.sette.common.exceptions.ConfigurationException;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;
import hu.bme.mit.sette.common.snippets.JavaVersion;
import hu.bme.mit.sette.common.validator.FileType;
import hu.bme.mit.sette.common.validator.FileValidator;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;

public final class CatgTool extends Tool {
    public final File toolDirectory;

    public CatgTool(File toolDirectory, String version) throws ConfigurationException {
        super("CATG", null, version);
        this.toolDirectory = toolDirectory;

        // validate
        getToolDirectory();
    }

    public File getToolDirectory() throws ConfigurationException {
        try {
            FileValidator v = new FileValidator(toolDirectory);
            v.type(FileType.DIRECTORY).readable(true).executable(true);
            v.validate();
        } catch (ValidatorException e) {
            throw new ConfigurationException("The CATG tool directory is invalid: " + toolDirectory,
                    e);
        }

        return toolDirectory;
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
            File outputDirectory) {
        return new CatgGenerator(snippetProject, outputDirectory, this);
    }

    @Override
    public CatgRunner createRunnerProjectRunner(SnippetProject snippetProject,
            File outputDirectory) {
        return new CatgRunner(snippetProject, outputDirectory, this);
    }

    @Override
    public CatgParser createRunResultParser(SnippetProject snippetProject, File outputDirectory) {
        return new CatgParser(snippetProject, outputDirectory, this);
    }
}
