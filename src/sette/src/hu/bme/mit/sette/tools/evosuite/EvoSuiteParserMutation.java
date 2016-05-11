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
package hu.bme.mit.sette.tools.evosuite;

import static hu.bme.mit.sette.core.util.io.PathUtils.exists;
import static java.util.stream.Collectors.toList;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

import hu.bme.mit.sette.core.model.runner.ParameterType;
import hu.bme.mit.sette.core.model.runner.ResultType;
import hu.bme.mit.sette.core.model.runner.RunnerProject;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetContainer;
import hu.bme.mit.sette.core.model.xml.SnippetInputsXml;
import hu.bme.mit.sette.core.tasks.EvaluationTaskBase;
import hu.bme.mit.sette.core.tasks.RunResultParserBase;
import hu.bme.mit.sette.core.util.EscapeSpecialCharactersVisitor;
import hu.bme.mit.sette.core.util.io.PathUtils;
import hu.bme.mit.sette.core.util.xml.XmlUtils;
import hu.bme.mit.sette.core.validator.PathValidator;
import hu.bme.mit.sette.core.validator.ValidationException;

public class EvoSuiteParserMutation extends EvaluationTaskBase<EvoSuiteTool> {
    public EvoSuiteParserMutation(RunnerProject runnerProject, EvoSuiteTool tool) {
        super(runnerProject, tool);
    }

    public void parse() throws Exception {
        beforeParse();

        for (SnippetContainer snippetContainer : getSnippetProject().getSnippetContainers()) {
            for (Snippet snippet : snippetContainer.getSnippets().values()) {
                Path inputsXmlFile = runnerProject.snippet(snippet).getInputsXmlFile();

                SnippetInputsXml inputsXml = XmlUtils.deserializeFromXml(SnippetInputsXml.class,
                        inputsXmlFile);

                if (inputsXml.getResultType() == ResultType.NA
                        || inputsXml.getResultType() == ResultType.EX
                        || inputsXml.getResultType() == ResultType.TM) {
                    log.info("Skipping {}: {}", snippet.getId(), inputsXml.getResultType());
                    return;
                }

                parseOne(snippet);
            }
        }
    }

    private void beforeParse() throws ValidationException {
        Path testDir = runnerProject.getTestDir();
        Path testDirBackup = runnerProject.getBaseDir().resolve("test-original");

        // require both test and test-original (test-mutation generated from formerly parsed tests)
        PathValidator.forDirectory(testDir, true, true, true).validate();
        PathValidator.forDirectory(testDirBackup, true, true, true).validate();

        Path testDirMutation = runnerProject.getBaseDir().resolve("test-mutation");

        if (PathUtils.exists(testDirMutation)) {
            PathUtils.delete(testDirMutation);
        }
        PathUtils.copy(testDir, testDirMutation);
    }

    private void parseOne(Snippet snippet) throws Exception {
        // test files
        Path testDirMutation = runnerProject.getBaseDir().resolve("test-mutation");
        String classNameWithSlashes = snippet.getContainer().getJavaClass().getName()
                .replace('.', '/');
        String snippetName = snippet.getName();

        {
            // evo: my/snippet/MySnippet_method_method
            String testFileBasePathEvo = String.format("%s_%s_%s_Test", classNameWithSlashes,
                    snippetName, snippetName);
            Path testCasesFileEvo = testDirMutation.resolve(testFileBasePathEvo + ".java");
            Path testScaffoldingFile = testDirMutation
                    .resolve(testFileBasePathEvo + "_scaffolding.java");
            if (exists(testCasesFileEvo)) {
                throw new RuntimeException(
                        "Original test case file should not exist: " + testScaffoldingFile);
            }
            if (exists(testScaffoldingFile)) {
                throw new RuntimeException(
                        "Scaffolding file should not exist: " + testScaffoldingFile);
            }
        }

        // normal: my/snippet/MySnippet_method
        String testFileBasePathNormal = String.format("%s_%s_Test", classNameWithSlashes,
                snippetName);
        Path testCasesFile = testDirMutation.resolve(testFileBasePathNormal + ".java");
        if (!exists(testCasesFile)) {
            throw new RuntimeException("Missing parsed test case file: " + testCasesFile);
        }

        // what to do:
        // - remove extra imports, e.g.,
        // import hu.bme.mit.sette.snippets._1_basic.B5_functions.B5a2_CallPrivate_conditionalCall
        // - replace calls (only for public calledFunction), e.g.,
        // "B5a1_CallPublic_conditionalCall.calledFunction" => "B5a1_CallPublic.calledFunction"
        // - generate asserts

        CompilationUnit compilationUnit;
        try {
            log.debug("Parsing with JavaParser: {}", testCasesFile);
            compilationUnit = JavaParser.parse(testCasesFile.toFile());
            log.debug("Parsed with JavaParser: {}", testCasesFile);
        } catch (Exception t) {
            throw new RuntimeException("Cannot parse: " + testCasesFile, t);
        }

        // remove import
        String removeImport = String.format("%s_%s", snippet.getContainer().getName(),
                snippet.getName());
        compilationUnit.getImports().removeIf(importDecl -> {
            if (importDecl.getName().getName().equals(removeImport)) {
                return true;
            } else {
                return false;
            }
        });
        compilationUnit.accept(new EscapeSpecialCharactersVisitor(), null);
        String testCasesFileString = compilationUnit.toString();

        // FIXME: public calledFunction calls
        String badCall = String.format("%s_%s.%s",
                snippet.getContainer().getJavaClass().getSimpleName(),
                snippetName, "calledFunction");

        String goodCall = String.format("%s.%s",
                snippet.getContainer().getJavaClass().getSimpleName(),
                "calledFunction");
        testCasesFileString = testCasesFileString.replace(badCall, goodCall);

        // save file
        PathUtils.write(testCasesFile, testCasesFileString.getBytes());

        // read again
        try {
            log.debug("Parsing with JavaParser: {}", testCasesFile);
            compilationUnit = JavaParser.parse(testCasesFile.toFile());
            log.debug("Parsed with JavaParser: {}", testCasesFile);
        } catch (Exception t) {
            throw new RuntimeException("Cannot parse: " + testCasesFile, t);
        }

        // generate assert
        List<MethodDeclaration> methodDecls = compilationUnit.getTypes().get(0).getMembers()
                .stream()
                .filter(member -> member instanceof MethodDeclaration)
                .map(member -> (MethodDeclaration) member)
                .filter(method -> method.getName().startsWith("test"))
                .collect(toList());

        for (MethodDeclaration methodDecl : methodDecls) {
            addAssert(snippet, methodDecl.getBody().getStmts());
        }

        compilationUnit.accept(new EscapeSpecialCharactersVisitor(), null);
        testCasesFileString = compilationUnit.toString();

        // save file
        PathUtils.write(testCasesFile, testCasesFileString.getBytes());
    }

    private static final String EXPR_PATTERN_STRING = "^"
            + "(?<retType>[A-Za-z0-9_\\[\\]]+)"
            + "\\s+"
            + "(?<retVar>[A-Za-z0-9_]+)"
            + "\\s*=\\s*"
            + "(?<className>[A-Za-z0-9_]+)"
            + "\\."
            + "(?<methodName>[A-Za-z0-9_]+)"
            + "\\("
            + "(?<params>.*)"
            + "\\)"
            + "$";
    private static final Pattern EXPR_PATTERN = Pattern.compile(EXPR_PATTERN_STRING);

    // used within one test case to store, e.g., variable declarations
    private List<ExpressionStmt> otherExprStatements;

    private void addAssert(Snippet snippet, List<Statement> statements) throws Exception {
        if (snippet.getMethod().getReturnType() == Void.class
                || snippet.getMethod().getReturnType() == void.class) {
            // no asserts for void return values
            return;
        }

        otherExprStatements = new ArrayList<>();
        List<Statement> originalStatements = new ArrayList<>(statements);

        // interate on originalStatements, edit statements
        for (int idx = 0; idx < originalStatements.size(); idx++) {
            Statement stmt = originalStatements.get(idx);
            if (stmt instanceof ExpressionStmt) {
                addAssertForExpressionLine(snippet, statements, idx);
            } else if (stmt instanceof TryStmt) {
                // skip
            } else {
                System.out.println("Not expression statement: " + snippet.getId());
                System.out.println("Statement class: " + stmt.getClass());
                throw new RuntimeException("What to do?");
            }
        }
        //
        // if (statements.size() == 1) {
        // if (statements.get(0) instanceof ExpressionStmt) {
        // } else {
        // // FIXME
        // // throw new RuntimeException("Not expression statement: " + snippet.getId());
        // }
        // } else {
        // throw new RuntimeException("More than one statement: " + snippet.getId());
        // }

    }

    private void addAssertForExpressionLine(Snippet snippet, List<Statement> statements, int idx)
            throws Exception {
        ExpressionStmt stmtLine = (ExpressionStmt) statements.get(idx);
        String expr = stmtLine.getExpression().toString().trim();

        Matcher matcher = EXPR_PATTERN.matcher(expr);
        if (!matcher.matches()) {
            otherExprStatements.add(stmtLine);
            return;

            // log.error("No expr match in tests for {}: {}", snippet.getId(), expr);
            // throw new RuntimeException("No match for: " + EXPR_PATTERN_STRING);
        }

        // String retTypeStr = matcher.group("retType");
        String retVarStr = matcher.group("retVar");
        String className = matcher.group("className");
        String methodName = matcher.group("methodName");
        String paramsListStr = matcher.group("params");

        if (!className.equals(snippet.getContainer().getName())) {
            // FIXME skip
            return;
            // throw new RuntimeException(
            // "Invalid classname " + className + " in tests for " + snippet.getId());
        }

        // needs this workaround since method can be public calledFunction too
        Method method;
        try {
            method = Stream.of(snippet.getContainer().getJavaClass().getDeclaredMethods())
                    .filter(m -> m.getName().equals(methodName)).findAny().get();
        } catch (NoSuchElementException ex) {
            throw new RuntimeException(
                    "Cannot find method " + methodName + " for snippet " + snippet.getId());
        }

        Class<?>[] paramTypes = method.getParameterTypes();
        List<String> paramsStr = Splitter.on(", ").trimResults().splitToList(paramsListStr);
        if (paramTypes.length == 0 && !paramsStr.isEmpty() && paramsStr.get(0).isEmpty()) {
            paramsStr = ImmutableList.of();
        }

        if (paramTypes.length != paramsStr.size()) {
            log.error("paramTypes.length: " + paramTypes.length);
            log.error("paramsStr.size(): " + paramsStr.size());
            log.error("stmts: " + statements);
            log.error("params: " + paramsListStr);
            throw new RuntimeException(
                    "Parameter mismatch for snippet " + snippet.getId());
        }

        Object[] paramValues = new Object[paramTypes.length];

        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> cls = paramTypes[i];
            String str = paramsStr.get(i);
            paramValues[i] = stringToObject(cls, str);
            if (cls.isPrimitive() && paramValues[i] == null) {
                paramValues[i] = RunResultParserBase.getDefaultParameterValue(cls);
            }
        }

        // extract method return value
        Object expectedReturnValue = null;
        Throwable expectedThrownException = null;
        try {
            expectedReturnValue = method.invoke(null, paramValues);
        } catch (InvocationTargetException ex) {
            expectedThrownException = ex.getTargetException();
        } catch (IllegalAccessException | IllegalArgumentException ex) {
            System.out.println("Method: " + method);
            System.out.println("Params: " + Arrays.asList(paramValues));
            throw new RuntimeException(ex);
        }

        // create assert
        if (expectedThrownException == null) {
            if (expectedReturnValue != null
                    && expectedReturnValue.getClass().getSimpleName()
                            .equals("CoordinateStructure")) {
                Class<?> cls = expectedReturnValue.getClass();
                Field fldx = cls.getDeclaredField("x");
                Field fldy = cls.getDeclaredField("y");
                fldx.setAccessible(true);
                fldy.setAccessible(true);

                int x = (int) fldx.get(expectedReturnValue);
                int y = (int) fldy.get(expectedReturnValue);

                String assertLine1 = String.format(
                        "junit.framework.Assert.assertEquals(%s, %s);",
                        x, retVarStr + ".x");
                String assertLine2 = String.format(
                        "junit.framework.Assert.assertEquals(%s, %s);",
                        y, retVarStr + ".y");
                try {
                    statements.add(JavaParser.parseStatement(assertLine1));
                } catch (Exception ex) {
                    log.error("Cannot parse line: " + assertLine1);
                    throw ex;
                }
                try {
                    statements.add(JavaParser.parseStatement(assertLine2));
                } catch (Exception ex) {
                    log.error("Cannot parse line: " + assertLine2);
                    throw ex;
                }
            } else {
                String assertMethod = expectedReturnValue != null
                        && expectedReturnValue.getClass().isArray()
                                // JUnit 3 does not have asserts for arrays but JUnit 4 is
                                // on the classpath (only static method call)
                                ? "org.junit.Assert.assertArrayEquals"
                                : "junit.framework.Assert.assertEquals";
                String assertLine = String.format(
                        "%s(%s, %s);",
                        assertMethod,
                        wrapForCode(expectedReturnValue, method.getReturnType()),
                        retVarStr);
                try {
                    statements.add(JavaParser.parseStatement(assertLine));
                } catch (Exception ex) {
                    log.error("Cannot parse line: " + assertLine);
                    throw ex;
                }
            }
        } else {
            List<String> assertLines = new ArrayList<>();
            assertLines.add("try {");
            assertLines.add(stmtLine.toString());
            assertLines.add("junit.framework.Assert.fail(); // no exception");
            assertLines.add(
                    "} catch (" + expectedThrownException.getClass().getName() + " ex) {");
            assertLines.add("// ok");
            assertLines.add("} catch (Throwable ex) {");
            assertLines.add("junit.framework.Assert.fail(); // bad exception");
            assertLines.add("}");

            try {
                statements.set(idx,
                        JavaParser.parseStatement(String.join("\n", assertLines)));
            } catch (Exception ex) {
                log.error("Cannot parse lines: " + assertLines);
                throw ex;
            }
        }
    }

    private String wrapForCode(Object value, Class<?> returnType) {
        String valueStr = wrapForCode(value);
        if (returnType == Integer.class) {
            valueStr = "Integer.valueOf(" + valueStr + ")";
        }
        return valueStr;
    }

    private String wrapForCode(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + String.valueOf(value) + "\"";
        } else if (value instanceof Character) {
            return "'" + String.valueOf(value) + "'";
        } else if (value instanceof Byte) {
            return "(byte) " + String.valueOf(value);
        } else if (value instanceof Short) {
            return "(short) " + String.valueOf(value);
        } else if (value instanceof Float) {
            return value + "f";
        } else if (value instanceof Long) {
            return value + "L";
        } else if (value.getClass().isArray()) {
            if (value instanceof int[]) {
                int[] arr = (int[]) value;
                String arrStr = Arrays.stream(arr).mapToObj(v -> wrapForCode(v))
                        .collect(Collectors.joining(", "));
                return String.format("new int [] {%s}", arrStr);
            } else {
                throw new RuntimeException("Array!!!: " + value.getClass());
            }
        } else
            if (value instanceof Integer || value instanceof Double || value instanceof Boolean) {
            return String.valueOf(value);
        } else {
            throw new RuntimeException("Conversion unknown for: " + value.getClass());
        }
    }

    // FIXME see ParameterElement too
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object stringToObject(Class<?> cls, String value) throws Exception {
        String originalValue = value;
        try {
            // primitives
            ParameterType type = ParameterType.primitiveFromJavaClass(cls);

            if (value.startsWith("(byte") || value.startsWith("(short")) {
                // cast detected
                int idx = value.indexOf(")");
                value = value.substring(idx + 1).trim();
            }

            if (value.matches("\\("
                    + "-?\\d+\\.?\\d*[FL]?"
                    + "\\)")) {
                value = value.substring(1, value.length() - 1);
            }

            switch (type) {
                case BYTE:
                    return Byte.valueOf(value);

                case SHORT:
                    return Short.valueOf(value);

                case INT:
                    return Integer.valueOf(value);

                case LONG:
                    if (value.endsWith("L") || value.endsWith("F")) {
                        value = value.substring(0, value.length() - 1);
                    }
                    return Long.valueOf(value);

                case FLOAT:
                    if (value.endsWith("L") || value.endsWith("F")) {
                        value = value.substring(0, value.length() - 1);
                    }
                    return Float.valueOf(value);

                case DOUBLE:
                    return Double.valueOf(value);

                case BOOLEAN:
                    return Boolean.valueOf(value);

                case CHAR:
                    if (!value.startsWith("'") || !value.endsWith("'")) {
                        throw new RuntimeException("Invalid char: " + value);
                    }

                    value = value.substring(1, value.length() - 1);
                    if (value.length() != 1) {
                        if (value.equals("\\\\")) {
                            return '\\';
                        } else {
                            throw new RuntimeException("Invalid char: " + value);
                        }
                    } else {
                        return value.charAt(0);
                    }
                default:
                    // TODO error handling
                    throw new RuntimeException("Unhandled parameter type: " + type);
            }
        } catch (IllegalArgumentException ex) {
            value = originalValue; // restore if changed in try

            if (ex instanceof NumberFormatException) {
                // ok
            } else if (!ex.getMessage().startsWith("The represented type is not primitive")) {
                throw ex;
            }

            if (value.matches("^(\\(.*\\)\\s*)?\".*\"$")) {
                if (value.startsWith("(")) {
                    // remove cast
                    value = value.split("\\)", 2)[1].trim();
                }
                value = value.substring(1, value.length() - 1);
                return value;
            } else if (value.equals("(int[]) null")
                    || value.equals("(Double) null")
                    || value.equals("(Integer) null")
                    || value.equals("(" + cls.getSimpleName() + ") null")) {
                return null;
            } else if (value.matches("^(\\(.*\\)\\s*)?[A-Za-z][A-Za-z0-9]+$")) {
                if (value.contains(" ")) {
                    // remove cast
                    value = value.split("\\s+", 2)[1];
                }
                String varName = value;

                List<String> lines = otherExprStatements.stream()
                        .map(e -> e.getExpression().toString().trim())
                        .filter(e -> e.contains(varName))
                        .collect(Collectors.toList());

                if (lines.isEmpty()) {
                    return null;
                }

                if (cls.isEnum()) {
                    // e.g.: State state0 = State.PAUSED;
                    String stateStr = lines.get(0);
                    stateStr = stateStr.split("=", 2)[1].trim();
                    stateStr = stateStr.split("\\.", 2)[1].trim();
                    return Enum.valueOf((Class<Enum>) cls, stateStr);
                } else if (cls == int[].class) {
                    // int[] intArray0 = new int[3]
                    int length = Integer.parseInt(
                            lines.get(0).split("=", 2)[1].split("\\[", 2)[1].replace("]", ""));
                    int[] ret = new int[length];

                    for (int i = 1; i < lines.size(); i++) {
                        // intArray0[1] = 3
                        String l = lines.get(i);

                        // intArray0, 1, , 3
                        String[] parts = l.split("\\[|\\]|=");
                        int idx = Integer.parseInt(parts[1].trim());
                        int v = Integer
                                .parseInt(parts[3].replace('(', ' ').replace(')', ' ').trim());

                        ret[idx] = v;
                    }

                    return ret;
                } else if (cls == Integer.class) {
                    String i = lines.get(0).replace("((", "(").split("=")[1].trim();
                    i = i.replace("))", ")");
                    i = i.replace("new Integer(", "");
                    i = i.replace(")", "");
                    return Integer.parseInt(i);
                } else if (cls == Double.class) {
                    String d = lines.get(0).replace("((", "(").split("=")[1].trim();
                    d = d.replace("))", ")");
                    d = d.replace("new Double(", "");
                    d = d.replace(")", "");
                    return Double.parseDouble(d);
                }

                if (lines.size() == 1) {
                    String firstLine = lines.get(0);

                    // e.g., Vector<Integer> vector0 = new Vector<Integer>()
                    if (firstLine.contains("new Vector")) {
                        return new Vector();
                    } else if (firstLine.contains("new LinkedList")) {
                        return new LinkedList();
                    } else if (cls.isArray()) {
                        return Array.newInstance(cls.getComponentType(), 0);
                    } else {
                        try {
                            return cls.newInstance();
                        } catch (InstantiationException exx) {
                            return null;
                        }
                    }
                } else {
                    // FIXME null is ok
                    return null;
                }
            }

            // TODO error handling
            throw new RuntimeException("Unhandled parameter type: " + cls + ": " + value);
        }
    }
}
