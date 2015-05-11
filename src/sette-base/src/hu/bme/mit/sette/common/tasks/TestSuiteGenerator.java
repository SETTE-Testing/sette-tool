/*
 * SETTE - Symbolic Execution based Test Tool Evaluator
 *
 * SETTE is a tool to help the evaluation and comparison of symbolic execution
 * based test input generator tools.
 *
 * Budapest University of Technology and Economics (BME)
 *
 * Authors: Lajos Cseppentő <lajos.cseppento@inf.mit.bme.hu>, Zoltán Micskei
 * <micskeiz@mit.bme.hu>
 *
 * Copyright 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package hu.bme.mit.sette.common.tasks;

import hu.bme.mit.sette.common.Tool;
import hu.bme.mit.sette.common.exceptions.SetteGeneralException;
import hu.bme.mit.sette.common.model.runner.ParameterType;
import hu.bme.mit.sette.common.model.runner.ResultType;
import hu.bme.mit.sette.common.model.runner.RunnerProjectUtils;
import hu.bme.mit.sette.common.model.runner.xml.AbstractParameterElement;
import hu.bme.mit.sette.common.model.runner.xml.InputElement;
import hu.bme.mit.sette.common.model.runner.xml.ParameterElement;
import hu.bme.mit.sette.common.model.runner.xml.SnippetInputsXml;
import hu.bme.mit.sette.common.model.snippet.Snippet;
import hu.bme.mit.sette.common.model.snippet.SnippetContainer;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;
import hu.bme.mit.sette.common.util.JavaFileUtils;

import java.io.File;
import java.lang.reflect.Method;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;

public final class TestSuiteGenerator extends SetteTask<Tool> {
    public TestSuiteGenerator(final SnippetProject snippetProject,
            final File outputDirectory, final Tool tool) {
        super(snippetProject, outputDirectory, tool);
    }

    public void generate() throws Exception {
        if (!RunnerProjectUtils.getRunnerLogFile(
                getRunnerProjectSettings()).exists()) {
            throw new SetteGeneralException(
                    "Run the tool on the runner project first (and then parse)");
        }

        File testsDir = getRunnerProjectSettings().getTestsDirectory();
        if (testsDir.exists()) {
            System.out.println("Removing tests dir");
            FileUtils.forceDelete(testsDir);
        }

        FileUtils.forceMkdir(testsDir);

        Serializer serializer = new Persister(new AnnotationStrategy());

        // foreach containers
        for (SnippetContainer container : getSnippetProject()
                .getModel().getContainers()) {
            // skip container with higher java version than supported
            if (container.getRequiredJavaVersion().compareTo(
                    getTool().getSupportedJavaVersion()) > 0) {
                // TODO error handling
                System.err.println("Skipping container: "
                        + container.getJavaClass().getName()
                        + " (required Java version: "
                        + container.getRequiredJavaVersion() + ")");
                continue;
            }

            // foreach snippets
            for (Snippet snippet : container.getSnippets().values()) {
                File inputsXmlFile = RunnerProjectUtils
                        .getSnippetInputsFile(
                                getRunnerProjectSettings(), snippet);

                if (!inputsXmlFile.exists()) {
                    System.err.println("Missing: " + inputsXmlFile);
                    continue;
                }

                // save current class loader
                ClassLoader originalClassLoader = Thread
                        .currentThread().getContextClassLoader();

                // set snippet project class loader
                Thread.currentThread().setContextClassLoader(
                        getSnippetProject().getClassLoader());

                // read data
                SnippetInputsXml inputsXml = serializer.read(
                        SnippetInputsXml.class, inputsXmlFile);

                // set back the original class loader
                Thread.currentThread().setContextClassLoader(
                        originalClassLoader);

                if (inputsXml.getResultType() != ResultType.S
                        && inputsXml.getResultType() != ResultType.C
                        && inputsXml.getResultType() != ResultType.NC) {
                    // skip!
                    continue;
                }

                if (inputsXml.getGeneratedInputs().size() == 0) {
                    System.err.println("No inputs: " + inputsXmlFile);
                }

                Class<?> javaClass = container.getJavaClass();
                Package pkg = javaClass.getPackage();
                Method method = snippet.getMethod();

                StringBuilder java = new StringBuilder();

                String classSimpleName = javaClass.getSimpleName()
                        + '_' + method.getName() + "_Tests";
                String className = pkg.getName() + "."
                        + classSimpleName;

                java.append("package ").append(pkg.getName())
                        .append(";\n");
                java.append("\n");
                java.append("import junit.framework.TestCase;\n");
                java.append("import ")
                        .append(container.getJavaClass().getName())
                        .append(";\n");
                java.append("\n");
                java.append("public final class ")
                        .append(classSimpleName)
                        .append(" extends TestCase {\n");

                int i = 0;
                for (InputElement inputElement : inputsXml
                        .getGeneratedInputs()) {
                    i++;

                    java.append("    public void test_").append(i)
                            .append("() throws Exception {\n");

                    // heap
                    if (StringUtils.isNotBlank(inputElement.getHeap())) {
                        for (String heapLine : inputElement.getHeap()
                                .split("\\r?\\n")) {
                            java.append("        ").append(heapLine)
                                    .append('\n');
                        }

                        java.append("        \n");
                    }

                    // try-catch for exception if any expected
                    if (inputElement.getExpected() != null) {
                        java.append("        try {\n");
                        java.append("            ");

                        appendMethodCall(java, javaClass, method,
                                inputElement);
                        java.append("            fail();\n");
                        java.append("        } catch (")
                                .append(inputElement.getExpected())
                                .append(" e) {\n");
                        java.append("        }\n");
                    } else {
                        java.append("        ");
                        appendMethodCall(java, javaClass, method,
                                inputElement);
                    }

                    java.append("    }\n\n");
                }

                java.append("}\n");

                File testsFile = new File(testsDir,
                        JavaFileUtils
                                .classNameToSourceFilename(className));
                FileUtils.write(testsFile, java.toString());

                // import junit.framework.TestCase;
                // import
                // hu.bme.mit.sette.snippets._1_basic.B2_conditionals.B2a_IfElse;
                //
                // public final class B2a_IfElse_oneParamInt_Tests extends
                // TestCase {
                // public void test_1() {
                // B2a_IfElse.oneParamInt(1);
                // }
                //
                // public void test_2() {
                // B2a_IfElse.oneParamInt(0);
                // }
                //
                // public void test_3() {
                // B2a_IfElse.oneParamInt(-1);
                // }
                //
                // public void test_4() {
                // B2a_IfElse.oneParamInt(12345);
                // }
                // }

            }
        }

    }

    // public final void generate() throws Exception {
    // if (!RunnerProjectUtils.getRunnerLogFile(getRunnerProjectSettings())
    // .exists()) {
    // throw new SetteGeneralException(
    // "Run the tool on the runner project first (and then parse)");
    // }
    //
    // // foreach containers
    // for (SnippetContainer container : this.getSnippetProject().getModel()
    // .getContainers()) {
    // // skip container with higher java version than supported
    // if (container.getRequiredJavaVersion().compareTo(
    // getTool().supportedJavaVersion()) > 0) {
    // // TODO error handling
    // System.err.println("Skipping container: "
    // + container.getJavaClass().getName()
    // + " (required Java version: "
    // + container.getRequiredJavaVersion() + ")");
    // continue;
    // }
    //
    // // foreach snippets
    // for (Snippet snippet : container.getSnippets().values()) {
    // SnippetInputsXml inputsXml = parseSnippet(snippet);
    // try {
    // // TODO further validation
    // inputsXml.validate();
    //
    // File inputsXmlFile = RunnerProjectUtils
    // .getSnippetInputsFile(getRunnerProjectSettings(),
    // snippet);
    //
    // FileUtils.forceMkdir(inputsXmlFile.getParentFile());
    //
    // } catch (ValidatorException e) {
    // System.err.println(e.getFullMessage());
    // }
    // }
    // }
    // }
    //
    // private SnippetInputsXml parseSnippet(Snippet snippet) throws Exception {
    // // TODO explain, make clear
    // SnippetInputsXml inputsXml = new SnippetInputsXml();
    // inputsXml.setToolName(getTool().getName());
    // inputsXml.setSnippetProjectElement(new SnippetProjectElement(
    // getSnippetProjectSettings().getBaseDirectory()
    // .getCanonicalPath()));
    //
    // inputsXml.setSnippetElement(new SnippetElement(snippet.getContainer()
    // .getJavaClass().getName(), snippet.getMethod().getName()));
    //
    // // TODO more doc is needed
    // File infoFile = RunnerProjectUtils.getSnippetInfoFile(
    // getRunnerProjectSettings(), snippet);
    //
    // if (!infoFile.exists()) {
    // inputsXml.setResultType(ResultType.NA);
    // } else {
    // List<String> lines = FileUtils.readLines(infoFile);
    //
    // if (lines.get(2).startsWith("Destroyed")) {
    // if (lines.get(2).startsWith("Destroyed: yes")) {
    // inputsXml.setResultType(ResultType.TM);
    // }
    // } else {
    // // TODO error handling
    // System.err.println("FORMAT PROBLEM");
    // }
    // }
    //
    // if (inputsXml.getResultType() == null) {
    // parseSnippet(snippet, inputsXml);
    // }
    //
    // return inputsXml;
    // }

    private void appendMethodCall(final StringBuilder sb,
            final Class<?> javaClass, final Method method,
            final InputElement inputElement) {
        sb.append(javaClass.getSimpleName()).append('.')
                .append(method.getName());
        sb.append("(");

        for (AbstractParameterElement parameter : inputElement
                .getParameters()) {
            if (parameter instanceof ParameterElement) {
                ParameterElement p = (ParameterElement) parameter;
                String value = p.getValue();

                if (p.getType() == ParameterType.CHAR) {
                    sb.append('\'').append(value).append('\'');
                } else if (p.getType() == ParameterType.BYTE) {
                    sb.append("(byte) ").append(value);
                } else if (p.getType() == ParameterType.SHORT) {
                    sb.append("(short) ").append(value);
                } else if (p.getType() == ParameterType.LONG) {
                    value = value.toUpperCase();
                    sb.append(value);
                    if (!value.endsWith("L")) {
                        sb.append("L");
                    }
                } else if (p.getType() == ParameterType.FLOAT) {
                    value = value.toLowerCase();
                    sb.append(value);
                    if (!value.endsWith("f")) {
                        sb.append("f");
                    }
                } else if (p.getType() == ParameterType.DOUBLE) {
                    sb.append(value);
                    if (value.indexOf('.') < 0) {
                        sb.append(".0");
                    }
                } else {
                    sb.append(value);
                }
                sb.append(", ");
            } else {
                System.err.println("Unhandled type: "
                        + parameter.getClass());
            }
        }

        if (inputElement.getParameters().size() > 0) {
            sb.delete(sb.length() - 2, sb.length());
        }

        sb.append(");\n");
    }
}
