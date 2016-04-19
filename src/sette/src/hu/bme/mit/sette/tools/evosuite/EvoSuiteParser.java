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

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import hu.bme.mit.sette.core.model.parserxml.SnippetInputsXml;
import hu.bme.mit.sette.core.model.runner.ResultType;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tasks.RunResultParserBase;
import hu.bme.mit.sette.core.util.EscapeSpecialCharactersVisitor;
import hu.bme.mit.sette.core.util.io.PathUtils;

public class EvoSuiteParser extends RunResultParserBase<EvoSuiteTool> {
    public EvoSuiteParser(SnippetProject snippetProject, Path outputDir, EvoSuiteTool tool,
            String runnerProjectTag) {
        super(snippetProject, outputDir, tool, runnerProjectTag);
    }

    @Override
    protected void beforeParse() {
        Path testDir = getRunnerProjectSettings().getTestDirectory().toPath();

        if (!PathUtils.exists(testDir)) {
            return;
        }

        Path testDirBackup = getRunnerProjectSettings().getBaseDir().toPath()
                .resolve("test-original");

        if (PathUtils.exists(testDirBackup)) {
            PathUtils.deleteIfExists(testDir);
            PathUtils.copy(testDirBackup, testDir);
        } else {
            PathUtils.copy(testDir, testDirBackup);
        }
    }

    @Override
    protected void parseSnippet(Snippet snippet, SnippetOutFiles outFiles,
            SnippetInputsXml inputsXml) throws Exception {
        // do not parse inputs
        inputsXml.setGeneratedInputs(null);

        // out files
        List<String> outputLines = outFiles.readOutputLines();
        List<String> errorLines = outFiles.readErrorOutputLines();

        // test files
        File testDir = getRunnerProjectSettings().getTestDirectory();
        String classNameWithSlashes = snippet.getContainer().getJavaClass().getName()
                .replace('.', '/');
        String snippetName = snippet.getName();

        // evo: my/snippet/MySnippet_method_method
        // normal: my/snippet/MySnippet_method
        String testFileBasePathEvo = String.format("%s_%s_%s_Test", classNameWithSlashes,
                snippetName, snippetName);
        String testFileBasePathNormal = String.format("%s_%s_Test", classNameWithSlashes,
                snippetName);
        File testCasesFileEvo = new File(testDir, testFileBasePathEvo + ".java");
        File testScaffoldingFile = new File(testDir, testFileBasePathEvo + "_scaffolding.java");
        File testCasesFile = new File(testDir, testFileBasePathNormal + ".java");

        if (outputLines.isEmpty()) {
            throw new RuntimeException(
                    "EvoSuite did not write anything to SDTOUT for " + snippet.getId());
        } else if (PathUtils.exists(testScaffoldingFile.toPath())) {
            // generated some tests -> S (but set later)
        } else if (errorLines.isEmpty()) {
            throw new RuntimeException(
                    "EvoSuite did not generate any error output nor test file for "
                            + snippet.getId());
        } else {
            // parse error output
            List<String> skipLines = Arrays.asList(
                    "ClientNode",
                    "ERROR JUnitAnalyzer - 1 test cases failed",
                    "ERROR ExternalProcessHandler - Class",
                    // internal timeouts:
                    "ERROR SearchStatistics",
                    "ClientNode: MINIMIZATION",
                    "ClientNode: WRITING_TESTS",
                    "ERROR JUnitAnalyzer - Ran out of time while checking tests");

            List<String> failLines = Arrays.asList(
                    "ERROR TestCaseExecutor - ExecutionException"

            );

            List<String> exLines = Arrays.asList(
                    "ERROR ClientNodeImpl - Error when connecting to master via RMI");

            List<String> tmLines = Arrays.asList(
                    "java.lang.OutOfMemoryError: Java heap space");

            for (String line : errorLines) {
                if (inputsXml.getResultType() == null) {
                    break;
                } else if (line.trim().isEmpty()) {
                    continue;
                }

                if (containsAny(line, failLines)) {
                    System.out.println("==========================================");
                    System.out.println(outFiles.errorOutputFile);
                    System.out.println(line);
                    System.out.println("==========================================");
                    System.out.println(
                            new String(PathUtils.readAllBytes(outFiles.errorOutputFile)));
                    System.out.println("==========================================");
                    System.out.println("==========================================");
                    throw new RuntimeException("Problematic line: " + line);
                } else if (containsAny(line, tmLines)) {
                    inputsXml.setResultType(ResultType.TM);
                } else if (containsAny(line, exLines)) {
                    inputsXml.setResultType(ResultType.EX);
                } else if (containsAny(line, skipLines)) {
                    // skip
                } else {
                    System.out.println("==========================================");
                    System.out.println(outFiles.errorOutputFile);
                    System.out.println(line);
                    System.out.println("==========================================");
                    System.out.println(
                            new String(PathUtils.readAllBytes(outFiles.errorOutputFile)));
                    System.out.println("==========================================");
                    System.out.println("==========================================");
                    throw new RuntimeException("Problematic line: " + line);
                }
            }
        }

        // FIXME these are manual evaluation results for extra snippets
        boolean deleteTestFileForExtra = false; // delete files which does not compile without
                                                // evosuite.jar
        if (inputsXml.getResultType() == null && getSnippetProject().getName().endsWith("-extra")) {
            ResultType result = null;

            switch (snippet.getId()) {
                // mock sysout & syserr
                case "Env1_writesEofToStdin":
                case "Env1_writesOutputBack":
                case "Env1_writesErrorOutputBack":
                case "Env1_writesOneLineToStdin":

                    // mocks network
                    // some snippets are related to guess protocol
                    // since they are usually NC, the easiest is to leave them
                    // (the snippets automatically create the socket, no host and port have to be
                    // specified)
                case "Env3_deadlock":
                case "Env3_guessHost":
                case "Env3_guessPort":
                case "Env3_guessHostAndPort":

                    // mocks system
                case "Env4_manipulatesClock":
                case "Env4_manipulatesRandom":

                    // Env4: props & and is not mocked
                    // and nothing speacial for threading & reflection
                    result = ResultType.C;
                    deleteTestFileForExtra = true;
                    break;

                default:
                    // nothing happends
                    break;
            }

            if (result != null) {
                log.warn("Manual result for {}: {}", snippet.getId(), result);
                inputsXml.setResultType(result);
            }
        }
        // FIXME end of manual result section

        if (inputsXml.getResultType() == null) {

            // no error detected, assume S
            if (snippet.getRequiredStatementCoverage() == 0) {
                inputsXml.setResultType(ResultType.C);
            } else {
                inputsXml.setResultType(ResultType.S);
            }
        }

        if (inputsXml.getResultType() == ResultType.S
                || inputsXml.getResultType() == ResultType.C) {
            boolean computationFinished = isComputationFinished(outputLines);

            if (!computationFinished) {
                throw new RuntimeException("Not finished: " + outFiles.outputFile.toString());
            }

            // delete scaffolding file
            PathUtils.deleteIfExists(testScaffoldingFile.toPath());

            if (!testCasesFileEvo.exists() && !testCasesFile.exists()) {
                // no test case, but the tool stopped properly
                inputsXml.setGeneratedInputCount(0);
            } else {
                // handle testCases file

                //
                // rename file if needed
                //
                if (testCasesFileEvo.exists() && testCasesFile.exists()) {
                    System.err.println(testCasesFileEvo);
                    System.err.println(testCasesFile);
                    throw new RuntimeException("Both EvoSuite and normal files exists");
                }

                if (testCasesFileEvo.exists() && !testCasesFile.exists()) {
                    testCasesFileEvo.renameTo(testCasesFile);
                }

                testCasesFileEvo = null;

                // last check whether rename was successful
                if (!testCasesFile.exists()) {
                    System.err.println(testCasesFile);
                    throw new RuntimeException("SETTE RUNTIME ERROR");
                }

                //
                // clean file
                //
                CompilationUnit compilationUnit;
                try {
                    log.debug("Parsing with JavaParser: {}", testCasesFile);
                    compilationUnit = JavaParser.parse(testCasesFile);
                    log.debug("Parsed with JavaParser: {}", testCasesFile);
                } catch (Exception t) {
                    throw new RuntimeException("Cannot parse: " + testCasesFile, t);
                }

                if (compilationUnit.getImports() == null) {
                    throw new RuntimeException("No imports in: " + testCasesFile);
                }

                // clean imports
                for (Iterator<ImportDeclaration> it = compilationUnit.getImports().iterator(); it
                        .hasNext();) {
                    String id = it.next().toString().trim();
                    Validate.isTrue(id.endsWith(";"));

                    id = StringUtils.substring(id, 0, -1);
                    id = id.replaceFirst("import\\s+", "");
                    id = id.replaceFirst("static\\s+", "");

                    Validate.isTrue(id.indexOf(' ') < 0);

                    // remove EvoSuite and JUnit 4
                    if (id.startsWith("org.evosuite") || id.startsWith("org.junit")) {
                        it.remove();
                    }
                }

                // add JUnit 3
                compilationUnit.getImports().add(new ImportDeclaration(
                        new NameExpr("junit.framework.TestCase"), false, false));

                // distinct imports
                Map<String, ImportDeclaration> newImports = new HashMap<>();
                for (ImportDeclaration id : compilationUnit.getImports()) {
                    if (!newImports.containsKey(id.toString())) {
                        newImports.put(id.toString(), id);
                    }
                }
                compilationUnit.setImports(new ArrayList<>(newImports.values()));

                //
                // handle class
                //
                ClassOrInterfaceDeclaration testClass = (ClassOrInterfaceDeclaration) compilationUnit
                        .getTypes().get(0);

                // remove annotations from class and members
                testClass.setAnnotations(new ArrayList<>());
                testClass.getMembers().forEach(member -> {
                    member.setAnnotations(new ArrayList<>());
                });

                // rename to appropriate name
                String nm = testCasesFile.getName();
                testClass.setName(nm.substring(0, nm.lastIndexOf('.')));

                // set appropriate super class
                testClass.getExtends().clear();
                testClass.getExtends().add(new ClassOrInterfaceType("TestCase"));

                //
                // handle and count methods
                //
                int testMethodCnt = 0;
                for (BodyDeclaration bd : testClass.getMembers()) {
                    if (bd instanceof MethodDeclaration) {
                        // method
                        MethodDeclaration md = (MethodDeclaration) bd;

                        if (md.getName().startsWith("test")) {
                            testMethodCnt++;
                            List<Statement> stmts = md.getBody().getStmts();
                            for (int i = 0; i < stmts.size(); i++) {
                                Statement stmt = stmts.get(i);

                                if (stmt.toString().contains("assertArrayEquals")) {
                                    // assertArrayEquals is not present in JUnit 3
                                    //
                                    // convert
                                    // from: assertArrayEquals(new int[] {}, intArray0);
                                    // to: assertTrue(java.util.Arrays.Arrays.equals(new int[] {
                                    // (-25)
                                    // }, intArray1));

                                    ExpressionStmt exprStmt = (ExpressionStmt) stmt;
                                    MethodCallExpr expr = (MethodCallExpr) exprStmt.getExpression();
                                    expr.setName("java.util.Arrays.equals");
                                    MethodCallExpr newExpr = new MethodCallExpr(null, "assertTrue",
                                            Arrays.asList(expr));
                                    exprStmt.setExpression(newExpr);
                                }
                            }
                        }
                    }
                }

                //
                // update test case file
                //
                // FIXME
                // String testCasesFileString = compilationUnit.toStringWithoutComments();
                compilationUnit.accept(new EscapeSpecialCharactersVisitor(), null);
                String testCasesFileString = compilationUnit.toString();

                // this can happen in some cases, e.g. infinite
                testCasesFileString = testCasesFileString.replace(
                        "fail(\"Expecting exception: TooManyResourcesException\");",
                        "/* EvoSuite: f a i l(\"Expecting exception: TooManyResourcesException\"); */");
                testCasesFileString = testCasesFileString.replace(
                        "} catch (TooManyResourcesException e) {",
                        "} catch (Throwable e) { throw e;");
                testCasesFileString = testCasesFileString.replace(
                        "assertThrownBy(",
                        "// assertThrownBy(");

                // NOTE one of the most important things, since evosuite has splitted snippets
                String badCall = String.format("%s_%s.%s",
                        snippet.getContainer().getJavaClass().getSimpleName(),
                        snippetName, snippetName);
                String goodCall = String.format("%s.%s",
                        snippet.getContainer().getJavaClass().getSimpleName(),
                        snippetName);

                testCasesFileString = testCasesFileString.replace(badCall, goodCall);

                // sometimes EvoSuite generates tests with executor
                testCasesFileString = testCasesFileString.replace(
                        "Future<?> future = executor.submit(new Runnable() {",
                        "Future<?> future = java.util.concurrent.Executors.newCachedThreadPool().submit(new Runnable() {");

                // comment out things like this:
                // "EvoSuiteRemoteAddress evoSuiteRemoteAddress0 = new
                // EvoSuiteRemoteAddress("200.42.42.0", 4444);"
                // "boolean boolean0 = NetworkHandling.openRemoteTcpServer(evoSuiteRemoteAddress0);"
                testCasesFileString = testCasesFileString.replace(
                        "EvoSuiteRemoteAddress evoSuiteRemoteAddress0 = new",
                        "// EvoSuiteRemoteAddress evoSuiteRemoteAddress0 = new");
                testCasesFileString = testCasesFileString.replace(
                        "boolean boolean0 = NetworkHandling.openRemoteTcpServer(evoSuiteRemoteAddress0);",
                        "// boolean boolean0 = NetworkHandling.openRemoteTcpServer(evoSuiteRemoteAddress0);");

                // NOTE stupid, but makes sure that inputsXml initialized properly
                if (deleteTestFileForExtra) {
                    PathUtils.deleteIfExists(testCasesFile.toPath());
                } else {
                    // save file
                    PathUtils.write(testCasesFile.toPath(), testCasesFileString.getBytes());
                }
                // set gen input count
                inputsXml.setGeneratedInputCount(testMethodCnt);
            }
        }
    }

    private static boolean containsAny(String line, List<String> search) {
        return search.stream().anyMatch(s -> line.contains(s));
    }

    private static boolean isComputationFinished(List<String> outLines) {
        for (int i = outLines.size() - 1; i >= 0; i--) {
            if (outLines.contains("* Computation finished")) {
                return true;
            }
        }
        return false;
    }
}
