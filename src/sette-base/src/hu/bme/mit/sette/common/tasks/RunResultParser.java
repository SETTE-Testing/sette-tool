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
package hu.bme.mit.sette.common.tasks;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;

import hu.bme.mit.sette.common.Tool;
import hu.bme.mit.sette.common.exceptions.RunResultParserException;
import hu.bme.mit.sette.common.model.parserxml.InputElement;
import hu.bme.mit.sette.common.model.parserxml.SnippetElement;
import hu.bme.mit.sette.common.model.parserxml.SnippetInputsXml;
import hu.bme.mit.sette.common.model.parserxml.SnippetProjectElement;
import hu.bme.mit.sette.common.model.runner.ResultType;
import hu.bme.mit.sette.common.model.runner.RunnerProjectUtils;
import hu.bme.mit.sette.common.model.snippet.Snippet;
import hu.bme.mit.sette.common.model.snippet.SnippetContainer;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;

public abstract class RunResultParser<T extends Tool> extends SetteTask<T> {
    public RunResultParser(SnippetProject snippetProject, File outputDirectory, T tool,
            String runnerProjectTag) {
        super(snippetProject, outputDirectory, tool, runnerProjectTag);
    }

    public final void parse() throws Exception {
        if (!RunnerProjectUtils.getRunnerLogFile(getRunnerProjectSettings()).exists()) {
            throw new RunResultParserException("Run the tool on the runner project first", this);
        }

        // foreach containers
        for (SnippetContainer container : getSnippetProject().getModel().getContainers()) {
            // skip container with higher java version than supported
            if (container.getRequiredJavaVersion()
                    .compareTo(getTool().getSupportedJavaVersion()) > 0) {
                // TODO error/warning handling
                System.err.println("Skipping container: " + container.getJavaClass().getName()
                        + " (required Java version: " + container.getRequiredJavaVersion() + ")");
                continue;
            }

            // foreach snippets
            for (Snippet snippet : container.getSnippets().values()) {
                SnippetInputsXml inputsXml = parseSnippet(snippet);
                try {
                    // TODO further validation
                    inputsXml.validate();

                    File inputsXmlFile = RunnerProjectUtils
                            .getSnippetInputsFile(getRunnerProjectSettings(), snippet);

                    FileUtils.forceMkdir(inputsXmlFile.getParentFile());

                    if (inputsXmlFile.exists()) {
                        FileUtils.forceDelete(inputsXmlFile);
                    }

                    Serializer serializer = new Persister(new AnnotationStrategy(),
                            new Format("<?xml version=\"1.0\" encoding= \"UTF-8\" ?>"));
                    serializer.write(inputsXml, inputsXmlFile);
                } catch (ValidatorException ex) {
                    System.err.println(ex.getFullMessage());
                }
            }
        }
    }

    private SnippetInputsXml parseSnippet(Snippet snippet) throws Exception {
        // TODO validation?
        SnippetInputsXml inputsXml = new SnippetInputsXml();
        inputsXml.setToolName(getTool().getName());
        inputsXml.setSnippetProjectElement(new SnippetProjectElement(
                getSnippetProjectSettings().getBaseDirectory().getCanonicalPath()));

        inputsXml.setSnippetElement(new SnippetElement(
                snippet.getContainer().getJavaClass().getName(), snippet.getMethod().getName()));

        // TODO needs more documentation
        File infoFile = RunnerProjectUtils.getSnippetInfoFile(getRunnerProjectSettings(), snippet);

        if (!infoFile.exists()) {
            inputsXml.setResultType(ResultType.NA);
        } else {
            List<String> lines = FileUtils.readLines(infoFile);

            if (lines.get(2).startsWith("Destroyed")) {
                if (lines.get(2).startsWith("Destroyed: yes")) {
                    inputsXml.setResultType(ResultType.TM);
                }
            } else {
                // TODO error handling
                System.err.println("FORMAT PROBLEM");
            }
        }

        if (inputsXml.getResultType() == null) {
            parseSnippet(snippet, inputsXml);

            if (inputsXml.getGeneratedInputs().isEmpty()
                    && (inputsXml.getResultType() == ResultType.S
                            || inputsXml.getResultType() == ResultType.NC
                            || inputsXml.getResultType() == ResultType.C)) {
                // no inputs but S, add an empty one
                inputsXml.getGeneratedInputs().add(new InputElement());

                // NOTE maybe parameters with default values can be added as well
            }
        }

        return inputsXml;
    }

    protected abstract void parseSnippet(Snippet snippet, SnippetInputsXml inputsXml)
            throws Exception;
}
