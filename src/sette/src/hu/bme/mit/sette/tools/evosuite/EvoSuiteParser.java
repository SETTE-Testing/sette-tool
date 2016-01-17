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
import java.nio.file.Files;
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
import hu.bme.mit.sette.core.model.runner.RunnerProjectUtils;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.random.JavaParserFixStringVisitor;
import hu.bme.mit.sette.core.tasks.RunResultParser;

public class EvoSuiteParser extends RunResultParser<EvoSuiteTool> {
    public EvoSuiteParser(SnippetProject snippetProject, Path outputDir, EvoSuiteTool tool,
            String runnerProjectTag) {
        super(snippetProject, outputDir, tool, runnerProjectTag);
    }

    @Override
    protected void parseSnippet(Snippet snippet, SnippetInputsXml inputsXml) throws Exception {
        // do not parse inputs
        inputsXml.setGeneratedInputs(null);

        // files
        File testDir = getRunnerProjectSettings().getTestDirectory();
        File outputFile = RunnerProjectUtils.getSnippetOutputFile(getRunnerProjectSettings(),
                snippet);
        File errorFile = RunnerProjectUtils.getSnippetErrorFile(getRunnerProjectSettings(),
                snippet);

        if (!outputFile.exists()) {
            // TODO
            throw new RuntimeException("TODO parser problem");
        }

        if (errorFile.exists()) {
            List<String> errLines = Files.readAllLines(errorFile.toPath());
            if (errLines.stream().anyMatch(
                    line -> line.contains("java.lang.OutOfMemoryError: Java heap space"))) {
                // not enough memory
                inputsXml.setResultType(ResultType.TM);
            } else {
                for (String line : errLines) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    if (line.contains("ClientNode")) {
                        // skip
                    } else if (line
                            .contains("[logback-2] ERROR JUnitAnalyzer - 1 test cases failed")) {
                        // skip
                    } else if (line.contains("ERROR ExternalProcessHandler - Class")) {
                        // skip (internal timeout, tool stops and dumps what is has)
                    } else if (line.contains("ERROR SearchStatistics")) {
                        // skip (internal timeout, tool stops and dumps what is has)
                    } else if (line.contains("ERROR TestGeneration - failed to write statistics")) {
                        // skip (internal timeout, tool stops and dumps what is has)
                    } else if (line.contains("ClientNode: MINIMIZATION")) {
                        // skip (internal timeout, tool stops and dumps what is has)
                    } else if (line.contains("ClientNode: WRITING_TESTS")) {
                        // skip (internal timeout, tool stops and dumps what is has)
                    } else if (line.contains("ERROR TestCaseExecutor - ExecutionException")) {
                        System.out.println("==========================================");
                        // "this is likely a serious error in the framework"
                        System.out.println(errorFile);
                        System.out.println(line);
                        System.out.println("==========================================");
                        System.out.println(new String(Files.readAllBytes(errorFile.toPath())));
                        System.out.println("==========================================");
                        System.out.println("==========================================");
                    } else {
                        System.out.println("==========================================");
                        System.out.println(errorFile);
                        System.out.println(line);
                        System.out.println("==========================================");
                        System.out.println(new String(Files.readAllBytes(errorFile.toPath())));
                        System.out.println("==========================================");
                        System.out.println("==========================================");
                        throw new RuntimeException("Problematic line: " + line);
                    }
                }
            }
        }

        if (inputsXml.getResultType() == null) {
            List<String> outLines = Files.readAllLines(outputFile.toPath());
            boolean computationFinished = isComputationFinished(outLines);

            if (!computationFinished) {
                throw new RuntimeException("Not finished: " + outputFile.toString());
            }

            // evo: my/snippet/MySnippet_method_method
            // normal: my/snippet/MySnippet_method
            String testFileBasePathEvo = String.format("%s_%s_%s_Test",
                    snippet.getContainer().getJavaClass().getName().replace('.', '/'),
                    snippet.getMethod().getName(), snippet.getMethod().getName());
            String testFileBasePathNormal = String.format("%s_%s_Test",
                    snippet.getContainer().getJavaClass().getName().replace('.', '/'),
                    snippet.getMethod().getName());
            File testCasesFileEvo = new File(testDir, testFileBasePathEvo + ".java");
            File testScaffoldingFile = new File(testDir, testFileBasePathEvo + "_scaffolding.java");
            File testCasesFile = new File(testDir, testFileBasePathNormal + ".java");

            // delete scaffolding file
            if (testScaffoldingFile.exists()) {
                Files.delete(testScaffoldingFile.toPath());
            }

            if (!testCasesFileEvo.exists() && !testCasesFile.exists()) {
                // NOTE no test case, but the tool stopped properly
                inputsXml.setResultType(ResultType.S);
                inputsXml.setGeneratedInputCount(0);
            } else {
                // handle testCases file
                inputsXml.setResultType(ResultType.S);

                //
                // rename file if needed
                //
                if (testCasesFileEvo.exists() && testCasesFile.exists()) {
                    System.err.println(testCasesFileEvo);
                    System.err.println(testCasesFile);
                    throw new RuntimeException("Both evo and normal files exists");
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
                    compilationUnit = JavaParser.parse(testCasesFile);
                } catch (Throwable t) {
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

                // distict imports
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
                compilationUnit.accept(new JavaParserFixStringVisitor(), null);
                String testCasesFileString = compilationUnit.toString();

                // this can happen in some cases, e.g. infinite
                testCasesFileString = testCasesFileString.replace(
                        "fail(\"Expecting exception: TooManyResourcesException\");",
                        "/* EvoSuite: f a i l(\"Expecting exception: TooManyResourcesException\"); */");
                testCasesFileString = testCasesFileString.replace(
                        "} catch (TooManyResourcesException e) {",
                        "} catch (Throwable e) { throw e;");

                // NOTE one of the most important things, since evosuite has splitted snippets
                String badCall = String.format("%s_%s.%s",
                        snippet.getContainer().getJavaClass().getSimpleName(),
                        snippet.getMethod().getName(), snippet.getMethod().getName());
                String goodCall = String.format("%s.%s",
                        snippet.getContainer().getJavaClass().getSimpleName(),
                        snippet.getMethod().getName());

                testCasesFileString = testCasesFileString.replace(badCall, goodCall);

                // sometimes EvoSuite generates tests with executor
                testCasesFileString = testCasesFileString.replace(
                        "Future<?> future = executor.submit(new Runnable() {",
                        "Future<?> future = java.util.concurrent.Executors.newCachedThreadPool().submit(new Runnable() {");

                // save file
                Files.write(testCasesFile.toPath(), testCasesFileString.getBytes());

                // set gen input count
                inputsXml.setGeneratedInputCount(testMethodCnt);
            }
        }
        //

        // if (errorFile.exists()) {
        // List<String> lines = Files.readAllLines(errorFile);
        // String firstLine = lines.get(0);
        //
        // if (firstLine.startsWith("java.io.FileNotFoundException:")
        // && firstLine.endsWith("_Test/Test.java (No such file or directory)")) {
        // // this means that no input was generated but the generation
        // // was successful
        // inputsXml.setGeneratedInputCount(0);
        //
        // if (snippet.getRequiredStatementCoverage() <= Double.MIN_VALUE
        // || snippet.getMethod().getParameterCount() == 0) {
        // // C only if the required statement coverage is 0% or
        // // the method takes no parameters
        // inputsXml.setResultType(ResultType.C);
        // } else {
        // inputsXml.setResultType(ResultType.NC);
        // }
        // } else if (firstLine.startsWith("java.lang.Error: classForName")) {
        // // exception, no output that not supported -> EX
        // inputsXml.setResultType(ResultType.EX);
        // } else {
        // // TODO
        // throw new RuntimeException("TODO parser problem");
        // }
        // }
        //
        // if (inputsXml.getResultType() == null) {
        // // always S for Randoop
        // inputsXml.setResultType(ResultType.S);
        //
        // // get how many tests were generated
        // List<String> outputFileLines = Files.readAllLines(outputFile);
        // int generatedInputCount = outputFileLines.stream()
        // .map(line -> TEST_COUNT_LINE_PATTERN.matcher(line.trim()))
        // .filter(m -> m.matches()).map(m -> Integer.parseInt(m.group(1))).findAny()
        // .orElse(-1);
        //
        // Validate.isTrue(generatedInputCount >= 0, "Output file:" + outputFile);
        // inputsXml.setGeneratedInputCount(generatedInputCount);
        //
        // // create input placeholders
        // }
        //
        // inputsXml.validate();
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
