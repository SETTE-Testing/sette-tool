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
package hu.bme.mit.sette.core.tasks.testsuiterunner2;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.google.common.base.Preconditions;

import hu.bme.mit.sette.core.model.runner.ResultType;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.tasks.testsuiterunner.CoverageInfo;
import hu.bme.mit.sette.core.tasks.testsuiterunner.JaCoCoClassLoader;
import hu.bme.mit.sette.core.tasks.testsuiterunner.LineStatus;
import hu.bme.mit.sette.core.tasks.testsuiterunner.LineStatuses;

public final class TestSuiteRunner2Helper {
    private static Logger log = LoggerFactory.getLogger(TestSuiteRunner2Helper.class);

    private TestSuiteRunner2Helper() {
        throw new UnsupportedOperationException("Static class");
    }

    static Pair<ResultType, Double> decideResultType(Snippet snippet, CoverageInfo coverageInfo)
            throws Exception {
        int linesToCover = 0;
        int linesCovered = 0;

        // iterate through files
        for (String relJavaFile : coverageInfo.data.keySet()) {
            // relJavaFile: hu/bme/mit/sette/snippets/_1_basic/B3_loops/B3c_DoWhile.java
            Path javaFile = snippet.getContainer().getSnippetProject().getSourceDir()
                    .resolve(relJavaFile);

            // parse file
            log.debug("Parsing with JavaParser: {}", javaFile);
            CompilationUnit compilationUnit = JavaParser.parse(javaFile.toFile());
            log.debug("Parsed with JavaParser: {}", javaFile);
            int beginLine = compilationUnit.getBeginLine();
            int endLine = compilationUnit.getEndLine();

            // get "line colours" to variables
            int[] full = coverageInfo.data.get(relJavaFile).getLeft().stream().sorted()
                    .mapToInt(i -> (int) i).sorted().toArray();
            int[] partial = coverageInfo.data.get(relJavaFile).getMiddle().stream().sorted()
                    .mapToInt(i -> (int) i).sorted().toArray();
            int[] not = coverageInfo.data.get(relJavaFile).getRight().stream().sorted()
                    .mapToInt(i -> (int) i).sorted().toArray();

            // validations
            Preconditions.checkState(beginLine >= 1, relJavaFile + " begin line: " + beginLine);
            Preconditions.checkState(endLine > beginLine, relJavaFile + " end line: " + endLine);

            // lines store, set order is very important!
            LineStatuses lines = new LineStatuses(beginLine, endLine);
            lines.setStatus(not, LineStatus.NOT_COVERED);
            lines.setStatus(partial, LineStatus.PARTLY_COVERED);
            lines.setStatus(full, LineStatus.FULLY_COVERED);

            // extract method
            try {
                // if does not fail, it is the source file corresponding to the snippet
                MethodDeclaration methodDecl = getCuMethodDecl(compilationUnit,
                        snippet.getMethod().getName());
                if (methodDecl == null) {
                    throw new NoSuchElementException();
                }

                for (int lineNumber = methodDecl.getBeginLine(); lineNumber <= methodDecl
                        .getEndLine(); lineNumber++) {
                    LineStatus s = lines.getStatus(lineNumber);

                    if (s != LineStatus.EMPTY) {
                        linesToCover++;
                        if (s.countsForStatementCoverage()) {
                            linesCovered++;
                        }
                    }
                }
            } catch (NoSuchElementException ex) {
                // snippet method was not found in the file => included method in dependency
            }

            if (snippet.getIncludedConstructors().isEmpty()
                    && snippet.getIncludedMethods().isEmpty()) {
                // nothing to do
            } else {
                // handle included coverage if:
                // a) method was not found in the file (dependency file)
                // b) there is included method in the same file as the snippet
                List<BodyDeclaration> inclDecls = new ArrayList<>();

                // NOTE this might be not working (ctor)
                for (Constructor<?> ctor : snippet.getIncludedConstructors()) {
                    if (!ctor.getDeclaringClass().getSimpleName()
                            .equals(compilationUnit.getTypes().get(0).getName())) {
                        continue;
                    }

                    BodyDeclaration decl = getCuConstructorDecl(compilationUnit,
                            ctor.getName());
                    // maybe default ctor not present in source
                    if (decl != null) {
                        inclDecls.add(decl);
                    }
                }

                for (Method method : snippet.getIncludedMethods()) {
                    if (!method.getDeclaringClass().getSimpleName()
                            .equals(compilationUnit.getTypes().get(0).getName())) {
                        continue;
                    }

                    BodyDeclaration decl = getCuMethodDecl(compilationUnit, method.getName());
                    // maybe in superclass
                    if (decl != null) {
                        inclDecls.add(decl);
                    }
                }

                for (BodyDeclaration methodDecl : inclDecls) {
                    for (int lineNumber = methodDecl.getBeginLine(); lineNumber <= methodDecl
                            .getEndLine(); lineNumber++) {
                        LineStatus s = lines.getStatus(lineNumber);

                        if (s != LineStatus.EMPTY) {
                            linesToCover++;
                            if (s.countsForStatementCoverage()) {
                                linesCovered++;
                            }
                        }
                    }
                }
            }
        }

        double coverage = (double) 100 * linesCovered / linesToCover;

        if (snippet.getRequiredStatementCoverage() <= coverage + 0.1) {
            return Pair.of(ResultType.C, coverage);
        } else {
            // NOTE only for debug
            // if (snippet.getRequiredStatementCoverage() < 100) {
            // System.out.println(
            // "NOT COVERED: " + snippet.getContainer().getJavaClass().getSimpleName()
            // + " _ " + snippet.getMethod().getName());
            // System.out.println("Covered: " + linesCovered);
            // System.out.println("ToCover: " + linesToCover);
            // System.out.println("Coverage: " + coverage);
            // System.out.println("ReqCoverage: " + snippet.getRequiredStatementCoverage());
            // File htmlFile = RunnerProjectUtils.getSnippetHtmlFile(runnerProject,
            // snippet);
            // System.out.println("file:///" + htmlFile.getAbsolutePath().replace('\\', '/'));
            // System.out.println("=============================================================");
            // }

            return Pair.of(ResultType.NC, coverage);
        }
    }

    private static MethodDeclaration getCuMethodDecl(CompilationUnit compilationUnit, String name) {
        try {
            return compilationUnit.getTypes().get(0).getMembers().stream()
                    .filter(bd -> bd instanceof MethodDeclaration).map(bd -> (MethodDeclaration) bd)
                    .filter(md -> md.getName().equals(name)).findAny().get();
        } catch (NoSuchElementException ex) {
            // NOTE maybe super class, skip now
            return null;
        }
    }

    private static ConstructorDeclaration getCuConstructorDecl(CompilationUnit compilationUnit,
            String name) {
        // maybe default ctor not present in source
        ConstructorDeclaration[] ctorDecls = compilationUnit.getTypes().get(0).getMembers().stream()
                .filter(bd -> bd instanceof ConstructorDeclaration)
                .map(bd -> (ConstructorDeclaration) bd).toArray(ConstructorDeclaration[]::new);

        if (ctorDecls.length == 0) {
            return null;
        } else {
            for (ConstructorDeclaration ctorDecl : ctorDecls) {
                // FIXME test this part
                if (ctorDecl.getName().equals(name)) {
                    return ctorDecl;
                }
                throw new RuntimeException("SETTE RUNTIME ERROR");
            }
            return null;
        }
    }

    static List<Class<?>> loadTestClasses(JaCoCoClassLoader classLoader,
            String testClassName) {
        List<Class<?>> testClasses = new ArrayList<>();

        Class<?> testClass = classLoader.tryLoadClass(testClassName);

        if (testClass != null) {
            // one class containing the test cases
            testClasses.add(testClass);
        } else {
            // one package containing the test cases

            testClasses.addAll(loadTestClassesForPrefix(classLoader, testClassName, "Test"));

            // randoop
            testClasses
                    .addAll(loadTestClassesForPrefix(classLoader, testClassName, "RegressionTest"));
            testClasses.addAll(loadTestClassesForPrefix(classLoader, testClassName, "ErrorTest"));
        }
        return testClasses;
    }

    private static List<Class<?>> loadTestClassesForPrefix(JaCoCoClassLoader classLoader,
            String packageName, String prefix) {
        List<Class<?>> testClasses = new ArrayList<>();

        String clsBaseName = packageName + "." + prefix;

        // try to load the class for the test suite (it is not used by SETTE)
        classLoader.tryLoadClass(clsBaseName);

        int i = 0;
        while (true) {
            // load the i-th test class (i starts at zero)
            Class<?> testClass = classLoader.tryLoadClass(clsBaseName + i);

            if (testClass != null) {
                testClasses.add(testClass);
            } else {
                // the i-th test class does not exists
                if (classLoader.tryLoadClass(clsBaseName + (i + 1)) != null) {
                    // but the (i+1)-th test class exists -> problem
                    throw new RuntimeException(
                            "i-th does not, but (i+1)-th exists! i=" + i + ": " + testClass);
                } else {
                    // ok, all test classes were found
                    break;
                }
            }

            i++;
        }

        return testClasses;
    }
}
