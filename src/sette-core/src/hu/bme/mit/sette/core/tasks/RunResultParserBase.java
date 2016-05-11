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

import static com.google.common.base.Preconditions.checkState;
import static hu.bme.mit.sette.core.util.io.PathUtils.exists;

import java.lang.reflect.Parameter;
import java.util.ArrayList;

import com.google.common.primitives.Primitives;

import hu.bme.mit.sette.core.exceptions.RunResultParserException;
import hu.bme.mit.sette.core.model.runner.ParameterType;
import hu.bme.mit.sette.core.model.runner.ResultType;
import hu.bme.mit.sette.core.model.runner.RunnerProject;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetContainer;
import hu.bme.mit.sette.core.model.xml.InputElement;
import hu.bme.mit.sette.core.model.xml.ParameterElement;
import hu.bme.mit.sette.core.model.xml.SnippetElement;
import hu.bme.mit.sette.core.model.xml.SnippetInfoXml;
import hu.bme.mit.sette.core.model.xml.SnippetInputsXml;
import hu.bme.mit.sette.core.model.xml.SnippetProjectElement;
import hu.bme.mit.sette.core.tool.Tool;
import hu.bme.mit.sette.core.tool.ToolOutputType;

public abstract class RunResultParserBase<T extends Tool> extends EvaluationTaskBase<T>
        implements RunResultParser {
    public RunResultParserBase(RunnerProject runnerProject, T tool) {
        super(runnerProject, tool);
    }

    @Override
    public final void parse() throws Exception {
        if (!exists(runnerProject.getRunnerLogFile())) {
            throw new RunResultParserException(
                    "Run the tool on the runner project first (missing log file)");
        }

        beforeParse();

        // foreach containers
        for (SnippetContainer container : getSnippetProject().getSnippetContainers()) {
            // foreach snippets
            for (Snippet snippet : container.getSnippets().values()) {
                // skip container with higher java version than supported
                if (!tool.supportsJavaVersion(container.getRequiredJavaVersion())) {
                    // TODO error/warning handling
                    System.err.println("Skipping container: " + container.getJavaClass().getName()
                            + " (required Java version: " + container.getRequiredJavaVersion()
                            + ")");
                    SnippetInputsXml inputsXml = new SnippetInputsXml();
                    if (tool.getOutputType() == ToolOutputType.INPUT_VALUES) {
                        inputsXml.setGeneratedInputs(new ArrayList<>());
                    } else {
                        inputsXml.setGeneratedInputCount(0);
                    }
                    inputsXml.setToolName(tool.getName());
                    inputsXml.setSnippetProjectElement(new SnippetProjectElement(
                            getSnippetProject().getBaseDir().toFile().getCanonicalPath()));

                    inputsXml.setSnippetElement(
                            new SnippetElement(snippet.getContainer().getJavaClass().getName(),
                                    snippet.getMethod().getName()));
                    inputsXml.setResultType(ResultType.NA);
                    inputsXml.validate();
                    runnerProject.snippet(snippet).writeInputsXml(inputsXml);
                    continue;
                }

                SnippetInputsXml inputsXml = parseSnippet(snippet);
                runnerProject.snippet(snippet).writeInputsXml(inputsXml);
            }
        }

        afterParse();

        // NOTE check whether all inputs and info files are created
        for (Snippet snippet : getSnippetProject().getSnippets()) {
            SnippetInfoXml infoXml = runnerProject.snippet(snippet).readInfoXml();
            SnippetInputsXml inputsXml = runnerProject.snippet(snippet).readInputsXml();

            checkState(inputsXml != null, "Missing info XML for %s", snippet.getId());
            if (infoXml == null) {
                checkState(inputsXml.getResultType() == ResultType.NA,
                        "If there is no .info file, the result must be N/A: %s", snippet.getId());
            }
        }
    }

    private SnippetInputsXml parseSnippet(Snippet snippet) throws Exception {
        // TODO validation?
        SnippetInputsXml inputsXml = new SnippetInputsXml();
        if (tool.getOutputType() == ToolOutputType.INPUT_VALUES) {
            inputsXml.setGeneratedInputs(new ArrayList<>());
        }
        inputsXml.setToolName(tool.getName());
        inputsXml.setSnippetProjectElement(new SnippetProjectElement(
                getSnippetProject().getBaseDir().toFile().getCanonicalPath()));

        inputsXml.setSnippetElement(new SnippetElement(
                snippet.getContainer().getJavaClass().getName(), snippet.getMethod().getName()));

        SnippetInfoXml infoXml = runnerProject.snippet(snippet).readInfoXml();

        // detect N/A and T/M
        if (infoXml == null) {
            inputsXml.setResultType(ResultType.NA);
        } else if (infoXml.isDestroyed()) {
            inputsXml.setResultType(ResultType.TM);
        } else {
            // if not detected, parse
            parseSnippet(snippet, inputsXml);

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

    protected abstract void parseSnippet(Snippet snippet, SnippetInputsXml inputsXml)
            throws Exception;

    protected void beforeParse() {
        // can be overridden by the children

    }

    protected void afterParse() {
        // can be overridden by the children
    }
}