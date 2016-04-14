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
package hu.bme.mit.sette.core.tasks;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Parameter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;

import com.google.common.primitives.Primitives;

import hu.bme.mit.sette.core.exceptions.RunResultParserException;
import hu.bme.mit.sette.core.model.parserxml.InputElement;
import hu.bme.mit.sette.core.model.parserxml.ParameterElement;
import hu.bme.mit.sette.core.model.parserxml.SnippetElement;
import hu.bme.mit.sette.core.model.parserxml.SnippetInputsXml;
import hu.bme.mit.sette.core.model.parserxml.SnippetProjectElement;
import hu.bme.mit.sette.core.model.runner.ParameterType;
import hu.bme.mit.sette.core.model.runner.ResultType;
import hu.bme.mit.sette.core.model.runner.RunnerProjectSettings;
import hu.bme.mit.sette.core.model.runner.RunnerProjectUtils;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetContainer;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tool.Tool;
import hu.bme.mit.sette.core.tool.ToolOutputType;
import hu.bme.mit.sette.core.util.io.PathUtils;
import hu.bme.mit.sette.core.validator.PathType;
import hu.bme.mit.sette.core.validator.PathValidator;

public abstract class RunResultParser<T extends Tool> extends EvaluationTask<T> {
    public RunResultParser(SnippetProject snippetProject, Path outputDir, T tool,
            String runnerProjectTag) {
        super(snippetProject, outputDir, tool, runnerProjectTag);
    }

    public final void parse() throws Exception {
        if (!RunnerProjectUtils.getRunnerLogFile(getRunnerProjectSettings()).exists()) {
            throw new RunResultParserException("Run the tool on the runner project first");
        }

        beforeParse();

        // foreach containers
        for (SnippetContainer container : getSnippetProject().getSnippetContainers()) {
            // foreach snippets
            for (Snippet snippet : container.getSnippets().values()) {
                // skip container with higher java version than supported
                if (container.getRequiredJavaVersion()
                        .compareTo(getTool().getSupportedJavaVersion()) > 0) {
                    // TODO error/warning handling
                    System.err.println("Skipping container: " + container.getJavaClass().getName()
                            + " (required Java version: " + container.getRequiredJavaVersion()
                            + ")");
                    SnippetInputsXml inputsXml = new SnippetInputsXml();
                    if (getTool().getOutputType() == ToolOutputType.INPUT_VALUES) {
                        inputsXml.setGeneratedInputs(new ArrayList<>());
                    } else {
                        inputsXml.setGeneratedInputCount(0);
                    }
                    inputsXml.setToolName(getTool().getName());
                    inputsXml.setSnippetProjectElement(new SnippetProjectElement(
                            getSnippetProject().getBaseDir().toFile().getCanonicalPath()));

                    inputsXml.setSnippetElement(
                            new SnippetElement(snippet.getContainer().getJavaClass().getName(),
                                    snippet.getMethod().getName()));
                    inputsXml.setResultType(ResultType.NA);
                    inputsXml.validate();

                    File inputsXmlFile = RunnerProjectUtils
                            .getSnippetInputsFile(getRunnerProjectSettings(), snippet);

                    PathUtils.createDir(inputsXmlFile.getParentFile().toPath());

                    PathUtils.deleteIfExists(inputsXmlFile.toPath());

                    Serializer serializer = new Persister(new AnnotationStrategy(),
                            new Format("<?xml version=\"1.0\" encoding= \"UTF-8\" ?>"));
                    serializer.write(inputsXml, inputsXmlFile);
                    continue;
                }

                SnippetInputsXml inputsXml = parseSnippet(snippet);
                // TODO further validation
                inputsXml.validate();

                File inputsXmlFile = RunnerProjectUtils
                        .getSnippetInputsFile(getRunnerProjectSettings(), snippet);

                PathUtils.createDir(inputsXmlFile.getParentFile().toPath());

                PathUtils.deleteIfExists(inputsXmlFile.toPath());

                Serializer serializer = new Persister(new AnnotationStrategy(),
                        new Format("<?xml version=\"1.0\" encoding= \"UTF-8\" ?>"));
                serializer.write(inputsXml, inputsXmlFile);
            }
        }

        afterParse();

        // NOTE check whether all inputs and info files are created
        // foreach containers
        for (SnippetContainer container : getSnippetProject().getSnippetContainers()) {
            // foreach snippets
            for (Snippet snippet : container.getSnippets().values()) {
                File inputsXmlFile = RunnerProjectUtils
                        .getSnippetInputsFile(getRunnerProjectSettings(), snippet);
                File infoFile = RunnerProjectUtils.getSnippetInfoFile(getRunnerProjectSettings(),
                        snippet);

                new PathValidator(inputsXmlFile.toPath()).type(PathType.REGULAR_FILE).validate();
                if (!infoFile.exists()) {
                    SnippetInputsXml inputsXml = new Persister(new AnnotationStrategy())
                            .read(SnippetInputsXml.class, inputsXmlFile);

                    Validate.isTrue(inputsXml.getResultType() == ResultType.NA,
                            "If there is no .info file, the result must be N/A: " + inputsXmlFile);

                }
            }
        }
    }

    private SnippetInputsXml parseSnippet(Snippet snippet) throws Exception {
        // TODO validation?
        SnippetInputsXml inputsXml = new SnippetInputsXml();
        if (getTool().getOutputType() == ToolOutputType.INPUT_VALUES) {
            inputsXml.setGeneratedInputs(new ArrayList<>());
        }
        inputsXml.setToolName(getTool().getName());
        inputsXml.setSnippetProjectElement(new SnippetProjectElement(
                getSnippetProject().getBaseDir().toFile().getCanonicalPath()));

        inputsXml.setSnippetElement(new SnippetElement(
                snippet.getContainer().getJavaClass().getName(), snippet.getMethod().getName()));

        SnippetOutFiles outFiles = new SnippetOutFiles(snippet, getRunnerProjectSettings());

        // detect N/A and T/M
        if (!PathUtils.exists(outFiles.infoFile)) {
            inputsXml.setResultType(ResultType.NA);
        } else {
            if (PathUtils.lines(outFiles.infoFile).anyMatch(s -> s.startsWith("Destroyed: yes"))) {
                inputsXml.setResultType(ResultType.TM);
            }
        }

        // if not detected, parse
        if (inputsXml.getResultType() == null) {
            parseSnippet(snippet, outFiles, inputsXml);

            if (inputsXml.getResultType() == null) {
                throw new RuntimeException("Result type must be set at this point");
            }
        }

        if ((inputsXml.getResultType() == ResultType.S || inputsXml.getResultType() == ResultType.NC
                || inputsXml.getResultType() == ResultType.C)
                && inputsXml.getGeneratedInputCount() == 0
                && inputsXml.getGeneratedInputs() != null) {
            // no inputs but S, add an empty one
            InputElement ie = new InputElement();
            inputsXml.getGeneratedInputs().add(ie);

            for (Parameter param : snippet.getMethod().getParameters()) {
                ie.getParameters().add(new ParameterElement(getParameterType(param.getType()),
                        getDefaultParameterValueString(param.getType())));
            }
            ie.validate();
        }
        // NOTE generated parameter count MUST match method parameter count
        if (inputsXml.getGeneratedInputs() != null) {
            for (InputElement ie : inputsXml.getGeneratedInputs()) {
                if (ie.getParameters().size() != snippet.getMethod().getParameterCount()) {
                    System.err.println("Parameter count mistmatch");
                    System.err.println("Snippet: " + snippet.getMethod().getName());
                    System.err.println("Generated cnt: " + ie.getParameters().size());
                    System.err.println("Expected: " + snippet.getMethod().getParameterCount());
                    throw new RuntimeException("Parameter count mistmatch");
                }
            }
        }

        return inputsXml;
    }

    public final static class SnippetOutFiles {
        public final Path infoFile;
        public final Path outputFile;
        public final Path errorOutputFile;

        public SnippetOutFiles(Snippet snippet, RunnerProjectSettings<?> runnerProjectSettings) {
            infoFile = RunnerProjectUtils.getSnippetInfoFile(runnerProjectSettings,
                    snippet).toPath();
            outputFile = RunnerProjectUtils.getSnippetOutputFile(runnerProjectSettings,
                    snippet).toPath();
            errorOutputFile = RunnerProjectUtils.getSnippetErrorFile(runnerProjectSettings,
                    snippet).toPath();
        }

        public List<String> readInfoLines() throws IOException {
            return PathUtils.readAllLinesOrEmpty(infoFile);
        }

        public List<String> readOutputLines() throws IOException {
            return PathUtils.readAllLinesOrEmpty(outputFile);
        }

        public List<String> readErrorOutputLines() throws IOException {
            return PathUtils.readAllLinesOrEmpty(errorOutputFile);
        }
    }

    // TODO visibility or refactor to other place
    protected static ParameterType getParameterType(Class<?> javaClass) {
        javaClass = Primitives.wrap(javaClass);

        if (javaClass.equals(Byte.class)) {
            return ParameterType.BYTE;
        } else if (javaClass.equals(Short.class)) {
            return ParameterType.SHORT;
        } else if (javaClass.equals(Integer.class)) {
            return ParameterType.INT;
        } else if (javaClass.equals(Long.class)) {
            return ParameterType.LONG;
        } else if (javaClass.equals(Float.class)) {
            return ParameterType.FLOAT;
        } else if (javaClass.equals(Double.class)) {
            return ParameterType.DOUBLE;
        } else if (javaClass.equals(Boolean.class)) {
            return ParameterType.BOOLEAN;
        } else if (javaClass.equals(Character.class)) {
            return ParameterType.CHAR;
        } else {
            // string or null
            return ParameterType.EXPRESSION;
        }
    }

    // TODO visibility or refactor to other place
    public static Object getDefaultParameterValue(Class<?> javaClass) {
        javaClass = Primitives.wrap(javaClass);

        if (javaClass.equals(Byte.class)) {
            return (byte) 1;
        } else if (javaClass.equals(Short.class)) {
            return (short) 1;
        } else if (javaClass.equals(Integer.class)) {
            return (int) 1;
        } else if (javaClass.equals(Long.class)) {
            return 1L;
        } else if (javaClass.equals(Float.class)) {
            return 1.0f;
        } else if (javaClass.equals(Double.class)) {
            return 1.0;
        } else if (javaClass.equals(Boolean.class)) {
            return false;
        } else if (javaClass.equals(Character.class)) {
            return ' ';
        } else {
            // string or null
            return null;
        }
    }

    // TODO visibility or refactor to other place
    public static String getDefaultParameterValueString(Class<?> javaClass) {
        javaClass = Primitives.wrap(javaClass);

        if (javaClass.equals(Byte.class)) {
            return "1";
        } else if (javaClass.equals(Short.class)) {
            return "1";
        } else if (javaClass.equals(Integer.class)) {
            return "1";
        } else if (javaClass.equals(Long.class)) {
            return "1";
        } else if (javaClass.equals(Float.class)) {
            return "1.0";
        } else if (javaClass.equals(Double.class)) {
            return "1.0";
        } else if (javaClass.equals(Boolean.class)) {
            return "false";
        } else if (javaClass.equals(Character.class)) {
            return " ";
        } else {
            // string or null
            return "null";
        }
    }

    protected abstract void parseSnippet(Snippet snippet, SnippetOutFiles outFiles,
            SnippetInputsXml inputsXml) throws Exception;

    protected void beforeParse() {
        // can be overridden by the children

    }

    protected void afterParse() {
        // can be overridden by the children
    }
}
