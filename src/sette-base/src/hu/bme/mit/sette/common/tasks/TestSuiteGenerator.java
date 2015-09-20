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
import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;

import hu.bme.mit.sette.common.Tool;
import hu.bme.mit.sette.common.exceptions.TestSuiteGeneratorException;
import hu.bme.mit.sette.common.model.parserxml.AbstractParameterElement;
import hu.bme.mit.sette.common.model.parserxml.InputElement;
import hu.bme.mit.sette.common.model.parserxml.ParameterElement;
import hu.bme.mit.sette.common.model.parserxml.SnippetInputsXml;
import hu.bme.mit.sette.common.model.runner.ParameterType;
import hu.bme.mit.sette.common.model.runner.ResultType;
import hu.bme.mit.sette.common.model.runner.RunnerProjectUtils;
import hu.bme.mit.sette.common.model.snippet.Snippet;
import hu.bme.mit.sette.common.model.snippet.SnippetContainer;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;
import hu.bme.mit.sette.common.util.JavaFileUtils;

public final class TestSuiteGenerator extends SetteTask<Tool> {
    public TestSuiteGenerator(SnippetProject snippetProject, File outputDirectory, Tool tool,
            String runnerProjectTag) {
        super(snippetProject, outputDirectory, tool, runnerProjectTag);
    }

    public void generate() throws Exception {
        if (!RunnerProjectUtils.getRunnerLogFile(getRunnerProjectSettings()).exists()) {
            throw new TestSuiteGeneratorException(
                    "Run the tool on the runner project first (and then parse)", this);
        }

        File testDir = getRunnerProjectSettings().getTestDirectory();
        if (testDir.exists()) {
            System.out.println("Removing test dir");
            FileUtils.forceDelete(testDir);
        }

        FileUtils.forceMkdir(testDir);

        Serializer serializer = new Persister(new AnnotationStrategy());

        // foreach containers
        for (SnippetContainer container : getSnippetProject().getModel().getContainers()) {
            // skip container with higher java version than supported
            if (container.getRequiredJavaVersion()
                    .compareTo(getTool().getSupportedJavaVersion()) > 0) {
                // TODO error handling
                System.err.println("Skipping container: " + container.getJavaClass().getName()
                        + " (required Java version: " + container.getRequiredJavaVersion() + ")");
                continue;
            }

            // foreach snippets
            for (Snippet snippet : container.getSnippets().values()) {
                File inputsXmlFile = RunnerProjectUtils
                        .getSnippetInputsFile(getRunnerProjectSettings(), snippet);

                if (!inputsXmlFile.exists()) {
                    System.err.println("Missing: " + inputsXmlFile);
                    continue;
                }

                // save current class loader
                ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

                // set snippet project class loader
                Thread.currentThread().setContextClassLoader(getSnippetProject().getClassLoader());

                // read data
                SnippetInputsXml inputsXml = serializer.read(SnippetInputsXml.class, inputsXmlFile);

                // set back the original class loader
                Thread.currentThread().setContextClassLoader(originalClassLoader);

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

                String classSimpleName = javaClass.getSimpleName() + '_' + method.getName()
                        + "_Test";
                String className = pkg.getName() + "." + classSimpleName;

                java.append("package ").append(pkg.getName()).append(";\n");
                java.append("\n");
                java.append("import junit.framework.TestCase;\n");
                java.append("import ").append(container.getJavaClass().getName()).append(";\n");
                java.append("\n");
                java.append("public final class ").append(classSimpleName)
                        .append(" extends TestCase {\n");

                int i = 0;
                for (InputElement inputElement : inputsXml.getGeneratedInputs()) {
                    i++;

                    // throws if required to prevent compile errors
                    java.append("    public void test_").append(i)
                            .append("() throws Exception {\n");

                    // heap
                    if (StringUtils.isNotBlank(inputElement.getHeap())) {
                        for (String heapLine : inputElement.getHeap().split("\\r?\\n")) {
                            java.append("        ").append(heapLine).append('\n');
                        }

                        java.append("        \n");
                    }
                    java.append("        \n");

                    // try-catch for exception if any expected
                    if (inputElement.getExpected() != null) {
                        java.append("        try {\n");
                        java.append("            ")
                                .append(createMethodCallString(javaClass, method, inputElement))
                                .append(";\n");
                        java.append("            fail();\n");
                        java.append("        } catch (").append(inputElement.getExpected())
                                .append(" ex) {\n");
                        // empty catch
                        java.append("        }\n");
                    } else {
                        // append method call and assert
                        List<AbstractParameterElement> params = inputElement.getParameters();
                        Object[] methodParams = new Object[params.size()];

                        try {
                            int ii = 0;
                            for (AbstractParameterElement ape : params) {
                                methodParams[ii] = ape.getValueAsObject();
                                ii++;
                            }

                            Class<?> snippetReturnType = snippet.getMethod().getReturnType();
                            Object returnValue = snippet.getMethod().invoke(null, methodParams);
                            if (snippetReturnType == Void.class
                                    || (returnValue != null && returnValue.toString()
                                            .startsWith(snippetReturnType.getName() + "@"))) {
                                // no return value or object return type written as tostring()
                                // NOTE fix in the future
                                // no assert, only method call
                                java.append("        ").append(
                                        createMethodCallString(javaClass, method, inputElement))
                                        .append(";\n");
                            } else {
                                String returnValueStr;
                                if (returnValue == null) {
                                    returnValueStr = "null";
                                } else {
                                    returnValueStr = getParamValueAsString(snippetReturnType,
                                            returnValue.toString());
                                }

                                // add cast if primitive value and boxed return type
                                if (!snippetReturnType.isPrimitive()) {
                                    Class<?> asPrimitive = ClassUtils
                                            .wrapperToPrimitive(snippetReturnType);
                                    if (asPrimitive != null) {
                                        // boxed type
                                        returnValueStr = String.format("(%s) (%s)",
                                                snippetReturnType.getSimpleName(), returnValueStr);
                                    }
                                }

                                // NOTE this code casts both ret and call to primitive, if not
                                // casted -> revise
                                //
                                // String castRet = "";
                                // String castCall = "";
                                // if (snippet.getMethod().getReturnType().isPrimitive()) {
                                // String tn = snippet.getMethod().getReturnType().getName();
                                // String cast = String.format("(%s)", tn);
                                //
                                // if (returnValueStr.trim().startsWith(cast)) {
                                // // only if not casted yet
                                // castRet = cast;
                                // }
                                // castCall = cast;
                                // } else if (returnValue.get) {
                                //
                                // }

                                // append line
                                java.append(String.format("        assertEquals(%s, %s);",
                                        returnValueStr,
                                        createMethodCallString(javaClass, method, inputElement)))
                                        .append("\n");
                            }
                        } catch (Exception ex) {
                            // the test might fail, the tool has not recorded the exception
                            // (or just heap)
                            java.append("        ")
                                    .append(createMethodCallString(javaClass, method, inputElement))
                                    .append(";\n");
                        }
                    }

                    java.append("        \n");
                    java.append("    }\n\n");
                }

                java.append("}\n");

                File testFile = new File(testDir,
                        JavaFileUtils.classNameToSourceFilename(className));
                FileUtils.write(testFile, java.toString());

                // import junit.framework.TestCase;
                // import
                // hu.bme.mit.sette.snippets._1_basic.B2_conditionals.B2a_IfElse;
                //
                // public final class B2a_IfElse_oneParamInt_Test extends
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
    // } catch (ValidatorException ex) {
    // System.err.println(ex.getFullMessage());
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

    private static String createMethodCallString(Class<?> javaClass, Method method,
            InputElement inputElement) {
        Object[] paramValuesAsString = new String[inputElement.getParameters().size()];

        int i = 0;
        for (AbstractParameterElement parameter : inputElement.getParameters()) {
            if (parameter instanceof ParameterElement) {
                ParameterElement p = (ParameterElement) parameter;
                String value = p.getValue();
                paramValuesAsString[i] = getParamValueAsString(p.getType().getJavaClass(), value);
            } else {
                System.err.println("Unhandled type: " + parameter.getClass());
                throw new RuntimeException("Not supported");
            }

            i++;
        }

        String parametersString = String.format(
                StringUtils.repeat("%s", ", ", inputElement.getParameters().size()),
                paramValuesAsString);

        String methodCallString = String.format("%s.%s(%s)", javaClass.getSimpleName(),
                method.getName(), parametersString);

        return methodCallString;
    }

    private static String getParamValueAsString(Class<?> javaClass, String value) {
        javaClass = ClassUtils.primitiveToWrapper(javaClass);
        String ret;

        if (javaClass == ParameterType.CHAR.getJavaClass()) {
            ret = String.format("'%s'", value);
        } else if (javaClass == ParameterType.BYTE.getJavaClass()) {
            // (byte) (-1)
            ret = "(byte) (" + value + ")";
        } else if (javaClass == ParameterType.SHORT.getJavaClass()) {
            // (short) (-1)
            ret = "(short) (" + value + ")";
        } else if (javaClass == ParameterType.LONG.getJavaClass()) {
            ret = value.toUpperCase();
            if (!value.endsWith("L")) {
                ret += "L";
            }
        } else if (javaClass == ParameterType.FLOAT.getJavaClass()) {
            ret = value.toLowerCase();
            if (!value.endsWith("f")) {
                ret += "f";
            }
        } else if (javaClass == ParameterType.DOUBLE.getJavaClass()) {
            ret = value;
            if (value.indexOf('.') < 0) {
                ret += ".0";
            }
        } else if (javaClass == String.class) {
            ret = String.format("\"%s\"", value);
        } else {
            ret = value;
        }

        return ret;
    }
}
