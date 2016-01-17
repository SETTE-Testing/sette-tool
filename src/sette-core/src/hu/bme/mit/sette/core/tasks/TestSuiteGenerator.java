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
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Primitives;

import hu.bme.mit.sette.core.exceptions.TestSuiteGeneratorException;
import hu.bme.mit.sette.core.model.parserxml.AbstractParameterElement;
import hu.bme.mit.sette.core.model.parserxml.InputElement;
import hu.bme.mit.sette.core.model.parserxml.ParameterElement;
import hu.bme.mit.sette.core.model.parserxml.SnippetInputsXml;
import hu.bme.mit.sette.core.model.runner.ParameterType;
import hu.bme.mit.sette.core.model.runner.ResultType;
import hu.bme.mit.sette.core.model.runner.RunnerProjectUtils;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetContainer;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tool.Tool;
import hu.bme.mit.sette.core.tool.ToolOutputType;
import hu.bme.mit.sette.core.util.io.DeleteFileVisitor;

public final class TestSuiteGenerator extends EvaluationTask<Tool> {
    public static final String ANT_BUILD_TEST_FILENAME;
    private static final String ANT_BUILD_TEST_DATA;

    static {
        ANT_BUILD_TEST_FILENAME = "build-test.xml";

        List<String> lines = new ArrayList<>();
        lines.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        lines.add("<!-- Build file for compiling tests -->");
        lines.add("<project default=\"compile-test\">");
        lines.add("    <import file=\"build.xml\" />");
        lines.add("");
        lines.add("    <target name=\"compile-test\">");
        lines.add("        <mkdir dir=\"build\" />");
        lines.add(
                "        <javac destdir=\"build\" includeantruntime=\"false\" source=\"${source}\" target=\"${target}\" debug=\"on\" nowarn=\"on\">");
        lines.add("            <compilerarg value=\"-Xlint:none\" />");
        lines.add("            <compilerarg value=\"-encoding\" />");
        lines.add("            <compilerarg value=\"UTF8\" />");
        lines.add("            <classpath>");
        lines.add("                <pathelement path=\"junit.jar\" />");
        lines.add("                <fileset dir=\"snippet-libs\">");
        lines.add("                    <include name=\"**/*.jar\" />");
        lines.add("                </fileset>");
        lines.add("            </classpath>");
        lines.add("            <src path=\"snippet-src\" />");
        lines.add("            <src path=\"test\" />");
        lines.add("        </javac>");
        lines.add("    </target>");
        lines.add("</project>");
        lines.add("");

        ANT_BUILD_TEST_DATA = String.join("\n", lines);
    }

    public TestSuiteGenerator(SnippetProject snippetProject, Path outputDir, Tool tool,
            String runnerProjectTag) {
        super(snippetProject, outputDir, tool, runnerProjectTag);
    }

    public void generate() throws Exception {
        if (!RunnerProjectUtils.getRunnerLogFile(getRunnerProjectSettings()).exists()) {
            throw new TestSuiteGeneratorException(
                    "Run the tool on the runner project first (and then parse)", this);
        }

        File testDir = getRunnerProjectSettings().getTestDirectory();

        // FIXME
        if (getTool().getOutputType() == ToolOutputType.INPUT_VALUES) {
            if (testDir.exists()) {
                System.out.println("Removing test dir");
                Files.walkFileTree(testDir.toPath(), new DeleteFileVisitor());
            }
        }

        Files.createDirectories(testDir.toPath());

        Serializer serializer = new Persister(new AnnotationStrategy());

        //
        // Generate test classes
        //

        // foreach containers
        for (SnippetContainer container : getSnippetProject().getSnippetContainers()) {
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

                // skip N/A, EX, T/M
                if (inputsXml.getResultType() != ResultType.S) {
                    if (inputsXml.getResultType() == ResultType.NC
                            || inputsXml.getResultType() == ResultType.C) {
                        System.err.println("Skipping " + inputsXml.getResultType() + " file: "
                                + inputsXmlFile.getName());
                    }

                    continue;
                }

                if (inputsXml.getGeneratedInputCount() == 0
                        && getTool().getOutputType() == ToolOutputType.INPUT_VALUES) {
                    System.err.println("No inputs: " + inputsXmlFile.getName());
                }

                Class<?> javaClass = container.getJavaClass();
                Package pkg = javaClass.getPackage();
                Method method = snippet.getMethod();

                // FIXME
                if (getTool().getOutputType() == ToolOutputType.INPUT_VALUES) {
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

                        CharSequence javaMethod = generateTestCaseMethod(snippet, i, inputElement);
                        if (javaMethod != null) {
                            java.append(javaMethod);
                        }
                    }

                    java.append("}\n");

                    File testFile = new File(testDir, className.replace('.', '/') + ".java");
                    Files.write(testFile.toPath(), java.toString().getBytes());

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

        //
        // Generate ant build file and copy junit.jar
        //
        // FIXME
        File antBuildTestFile = new File(getRunnerProjectSettings().getBaseDir(),
                ANT_BUILD_TEST_FILENAME);
        File jUnitJar = new File(getRunnerProjectSettings().getBaseDir(), "junit.jar");
        if (antBuildTestFile.exists()) {
            // NOTE better ex type
            Preconditions.checkState(antBuildTestFile.delete());
        }
        if (jUnitJar.exists()) {
            // NOTE better ex type
            Preconditions.checkState(jUnitJar.delete());
        }

        Files.write(antBuildTestFile.toPath(), ANT_BUILD_TEST_DATA.getBytes());

        Files.copy(getSetteJUnitJarInputStream(), jUnitJar.toPath());
    }

    private static CharSequence generateTestCaseMethod(Snippet snippet, int i,
            InputElement inputElement) {
        Class<?> javaClass = snippet.getContainer().getJavaClass();
        Method method = snippet.getMethod();
        StringBuilder methodCode = new StringBuilder();

        Class<?>[] snippetParameterTypes = snippet.getMethod().getParameterTypes();
        Class<?> snippetReturnType = snippet.getMethod().getReturnType();

        for (int j = 0; j < snippetParameterTypes.length; j++) {
            Class<?> spt = javaClass = Primitives.wrap(snippetParameterTypes[j]);

            // NOTE array parameter element is not handled since it is not used now
            ParameterElement ipt = (ParameterElement) inputElement.getParameters().get(j);

            if (ipt.getType() != ParameterType.EXPRESSION) {
                Class<?> generated = Primitives.wrap(ipt.getType().getJavaClass());
                if (spt != generated && !spt.isAssignableFrom(generated)) {
                    System.err.println("Incompatible generated parameter type");
                    System.err.println("Snippet: " + method.getName());
                    System.err.println("Expected: " + spt.getName());
                    System.err.println("Generated: " + generated.getName());
                    System.err.println("Skipping test case " + i);
                    return null;
                }
            } else {
                // NOTE spf generates int for strings
                if (spt.equals(String.class) && ipt.getType() == ParameterType.EXPRESSION) {
                    try {
                        Long.parseLong(ipt.getValue());
                        // NOTE parsed as number, however, if it is String it should start with "
                        // (like "mystring") (or it van be variable which must not start with
                        // number)

                        System.err.println("Number cannot be Stirng");
                        System.err.println("Snippet: " + method.getName());
                        System.err.println("Expected: " + spt.getName());
                        System.err.println("Generated Java expression: " + ipt.getValue());
                        System.err.println("Skipping test case " + i);
                        return null;
                    } catch (NumberFormatException ex) {
                        // NOTE not long, assume it is ok
                    }
                }
            }
        }

        // throws if required to prevent compile errors
        methodCode.append("    public void test_").append(i).append("() throws Exception {\n");

        // heap
        if (StringUtils.isNotBlank(inputElement.getHeap())) {
            for (String heapLine : inputElement.getHeap().split("\\r?\\n")) {
                methodCode.append("        ").append(heapLine).append('\n');
            }

            methodCode.append("        \n");
        }
        methodCode.append("        \n");

        // try-catch for exception if any expected
        if (inputElement.getExpected() != null) {
            methodCode.append("        try {\n");
            methodCode.append("            ")
                    .append(createMethodCallString(javaClass, method, inputElement)).append(";\n");
            methodCode.append("            fail();\n");
            methodCode.append("        } catch (").append(inputElement.getExpected())
                    .append(" ex) {\n");
            // empty catch
            methodCode.append("        }\n");
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

                Object returnValue;
                if (snippet.getMethod().getName().startsWith("infinite")) {
                    // infinite method, do not call
                    returnValue = RunResultParser.getDefaultParameterValue(snippetReturnType);
                } else {
                    returnValue = snippet.getMethod().invoke(null, methodParams);
                }

                if (snippetReturnType == Void.class || (returnValue != null
                        && returnValue.toString().startsWith(snippetReturnType.getName() + "@"))) {
                    // no return value or object return type written as tostring()
                    // NOTE fix in the future
                    // no assert, only method call
                    methodCode.append("        ")
                            .append(createMethodCallString(javaClass, method, inputElement))
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
                        Class<?> asPrimitive = ClassUtils.wrapperToPrimitive(snippetReturnType);
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
                    methodCode
                            .append(String.format("        assertEquals(%s, %s);", returnValueStr,
                                    createMethodCallString(javaClass, method, inputElement)))
                            .append("\n");
                }
            } catch (Exception ex) {
                // the test might fail, the tool has not recorded the exception
                // (or just heap)
                methodCode.append("        ")
                        .append(createMethodCallString(javaClass, method, inputElement))
                        .append(";\n");
            }
        }

        methodCode.append("        \n");
        methodCode.append("    }\n\n");

        return methodCode;
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
    // } catch (ValidationException ex) {
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
    // getSnippetProjectSettings().getBaseDir()
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
    // List<String> lines = Files.readAllLines(infoFile);
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

    // NOTE should move somewhere else
    public static InputStream getSetteJUnitJarInputStream() {
        // search with the classloader
        URL url = TestSuiteGenerator.class.getClassLoader().getResource("junit.jar.res");

        if (url == null) {
            // search on classpath
            for (URL u : ((URLClassLoader) (Thread.currentThread().getContextClassLoader()))
                    .getURLs()) {
                // search for a valid JUnit jar
                // examples: junit.jar, junit-junit.jar, junit-4.12.jar
                if (u.toString().replace('\\', '/').matches("^.*junit[0-9.-]*[.]jar$")) {
                    url = u;
                }
            }
        }

        if (url == null) {
            // NOTE make it better
            throw new RuntimeException("JUnit was not found");
        } else {
            try {
                return url.openStream();
            } catch (IOException ex) {
                // NOTE make it better
                throw new RuntimeException(ex);
            }
        }
    }
}
