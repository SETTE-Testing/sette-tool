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
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.IRuntime;
import org.jacoco.core.runtime.LoggerRuntime;
import org.jacoco.core.runtime.RuntimeData;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import hu.bme.mit.sette.common.Tool;
import hu.bme.mit.sette.common.exceptions.TestSuiteRunnerException;
import hu.bme.mit.sette.common.model.parserxml.FileCoverageElement;
import hu.bme.mit.sette.common.model.parserxml.SnippetCoverageXml;
import hu.bme.mit.sette.common.model.parserxml.SnippetElement;
import hu.bme.mit.sette.common.model.parserxml.SnippetInputsXml;
import hu.bme.mit.sette.common.model.parserxml.SnippetProjectElement;
import hu.bme.mit.sette.common.model.parserxml.SnippetResultXml;
import hu.bme.mit.sette.common.model.runner.ResultType;
import hu.bme.mit.sette.common.model.runner.RunnerProjectUtils;
import hu.bme.mit.sette.common.model.snippet.Snippet;
import hu.bme.mit.sette.common.model.snippet.SnippetContainer;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;
import hu.bme.mit.sette.common.util.process.ProcessRunner;
import hu.bme.mit.sette.common.util.process.ProcessRunnerListener;
import hu.bme.mit.sette.common.validator.FileType;
import hu.bme.mit.sette.common.validator.FileValidator;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public final class TestSuiteRunner extends SetteTask<Tool> {
    private static final Logger logger = LoggerFactory.getLogger(TestSuiteRunner.class);

    public TestSuiteRunner(SnippetProject snippetProject, File outputDirectory, Tool tool,
            String runnerProjectTag) {
        super(snippetProject, outputDirectory, tool, runnerProjectTag);
    }

    public void analyze() throws Exception {
        if (!RunnerProjectUtils.getRunnerLogFile(getRunnerProjectSettings()).exists()) {
            throw new TestSuiteRunnerException(
                    "Run the tool on the runner project first (and then parse and generate tests)",
                    this);
        }

        File testDir = getRunnerProjectSettings().getTestDirectory();
        if (!testDir.exists()) {
            throw new TestSuiteRunnerException("test dir does not exist", this);
        }

        // call ant build
        // ant build
        ProcessRunner pr = new ProcessRunner();
        pr.setPollIntervalInMs(1000);
        if (SystemUtils.IS_OS_WINDOWS) {
            pr.setCommand(new String[] { "cmd.exe", "/c",
                    "ant -buildfile " + TestSuiteGenerator.ANT_BUILD_TEST_FILENAME });
        } else {
            pr.setCommand(new String[] { "/bin/bash", "-c",
                    "ant -buildfile " + TestSuiteGenerator.ANT_BUILD_TEST_FILENAME });
        }
        pr.setWorkingDirectory(getRunnerProjectSettings().getBaseDirectory());
        pr.addListener(new ProcessRunnerListener() {
            @Override
            public void onTick(ProcessRunner processRunner, long elapsedTimeInMs) {
                System.out.println("ant build tick: " + elapsedTimeInMs);
            }

            @Override
            public void onIOException(ProcessRunner processRunner, IOException ex) {
                // TODO handle error
                ex.printStackTrace();
            }

            @Override
            public void onComplete(ProcessRunner processRunner) {
                if (processRunner.getStdout().length() > 0) {
                    System.out.println("Ant build output:");
                    System.out.println("========================================");
                    System.out.println(processRunner.getStdout().toString());
                    System.out.println("========================================");
                }

                if (processRunner.getStderr().length() > 0) {
                    System.out.println("Ant build error output:");
                    System.out.println("========================================");
                    System.out.println(processRunner.getStderr().toString());
                    System.out.println("========================================");
                    System.out.println("Terminating");
                }
            }

            @Override
            public void onStdoutRead(ProcessRunner processRunner, int charactersRead) {
                // not needed
            }

            @Override
            public void onStderrRead(ProcessRunner processRunner, int charactersRead) {
                // not needed
            }
        });

        pr.execute();

        if (pr.getStderr().length() > 0) {
            // TODO enchance error handling
            throw new RuntimeException("ant build has failed");
        }

        //
        Serializer serializer = new Persister(new AnnotationStrategy());

        // binary directories for the JaCoCoClassLoader
        File[] binaryDirectories = new File[2];
        binaryDirectories[0] = getSnippetProject().getSettings().getSnippetBinaryDirectory();
        binaryDirectories[1] = getRunnerProjectSettings().getBinaryDirectory();
        logger.debug("Binary directories: {}", (Object) binaryDirectories);

        // foreach containers
        for (SnippetContainer container : getSnippetProject().getModel().getContainers()) {
            // foreach snippets
            for (Snippet snippet : container.getSnippets().values()) {
                File inputsXmlFile = RunnerProjectUtils
                        .getSnippetInputsFile(getRunnerProjectSettings(), snippet);

                if (!inputsXmlFile.exists()) {
                    throw new RuntimeException("Missing inputsXML: " + inputsXmlFile);
                }

                // it is now tricky, classloader hell
                SnippetInputsXml inputsXml;
                {
                    // save current class loader
                    ClassLoader originalClassLoader = Thread.currentThread()
                            .getContextClassLoader();

                    // set snippet project class loader
                    Thread.currentThread()
                            .setContextClassLoader(getSnippetProject().getClassLoader());

                    // read data
                    inputsXml = serializer.read(SnippetInputsXml.class, inputsXmlFile);

                    // set back the original class loader
                    Thread.currentThread().setContextClassLoader(originalClassLoader);
                }

                // skip N/A, EX, T/M and already done
                if (inputsXml.getResultType() != ResultType.S) {
                    Double reqCov;
                    if (inputsXml.getResultType() == ResultType.C) {
                        reqCov = snippet.getRequiredStatementCoverage();
                    } else if (inputsXml.getResultType() == ResultType.NC) {
                        throw new RuntimeException("SETTE error: result is NC before test-runner"
                                + snippet.getContainer().getJavaClass().getSimpleName() + "_"
                                + snippet.getMethod().getName());
                    } else {
                        reqCov = null;
                    }

                    // create results xml
                    SnippetResultXml resultXml = SnippetResultXml.createForWithResult(inputsXml,
                            inputsXml.getResultType(), reqCov);
                    resultXml.validate();

                    // TODO needs more documentation
                    File resultFile = RunnerProjectUtils
                            .getSnippetResultFile(getRunnerProjectSettings(), snippet);

                    Serializer serializerWrite = new Persister(new AnnotationStrategy(),
                            new Format("<?xml version=\"1.0\" encoding= \"UTF-8\" ?>"));

                    serializerWrite.write(resultXml, resultFile);

                    continue;
                }

                if (inputsXml.getGeneratedInputCount() == 0) {
                    // throw new RuntimeException("No inputs: " + inputsXmlFile);
                }

                // NOTE remove try-catch
                try {
                    // analyze
                    SnippetCoverageXml coverageXml = analyzeOne(snippet, binaryDirectories);

                    // create results xml
                    SnippetResultXml resultXml = SnippetResultXml.createForWithResult(inputsXml,
                            coverageXml.getResultType(), coverageXml.getAchievedCoverage());
                    resultXml.validate();

                    // TODO needs more documentation
                    File resultFile = RunnerProjectUtils
                            .getSnippetResultFile(getRunnerProjectSettings(), snippet);

                    Serializer serializerWrite = new Persister(new AnnotationStrategy(),
                            new Format("<?xml version=\"1.0\" encoding= \"UTF-8\" ?>"));

                    serializerWrite.write(resultXml, resultFile);
                } catch (ValidatorException ex) {
                    System.err.println(ex.getFullMessage());
                    throw new RuntimeException("Validation failed");
                } catch (Throwable ex) {
                    // now dump and go on
                    System.out.println("========================================================");
                    ex.printStackTrace();
                    System.out.println("========================================================");
                    System.out.println("========================================================");
                }
            }
        }

        // NOTE check whether all inputs and info files are created
        // foreach containers
        for (SnippetContainer container : getSnippetProject().getModel().getContainers()) {
            // foreach snippets
            for (Snippet snippet : container.getSnippets().values()) {
                File resultXmlFile = RunnerProjectUtils
                        .getSnippetResultFile(getRunnerProjectSettings(), snippet);

                new FileValidator(resultXmlFile).type(FileType.REGULAR_FILE).validate();
            }
        }

        // TODO remove debug
        System.err.println("=> ANALYZE ENDED");
    }

    private SnippetCoverageXml analyzeOne(Snippet snippet, File[] binaryDirectories)
            throws Throwable {
        //
        // Initialize
        //
        String snippetClassName = snippet.getContainer().getJavaClass().getName();
        String snippetMethodName = snippet.getMethod().getName();
        String testClassName = snippet.getContainer().getJavaClass().getName() + "_"
                + snippet.getMethod().getName() + "_Test";

        logger.debug("Snippet: {}#{}()", snippetClassName, snippetMethodName);
        logger.debug("Test: {}", testClassName);

        // create JaCoCo runtime and instrumenter
        IRuntime runtime = new LoggerRuntime();
        Instrumenter instrumenter = new Instrumenter(runtime);

        // start runtime
        RuntimeData data = new RuntimeData();
        runtime.startup(data);

        // create class loader
        JaCoCoClassLoader testClassLoader = new JaCoCoClassLoader(binaryDirectories, instrumenter,
                getSnippetProject().getClassLoader());

        // load test class
        // snippet class and other dependencies will be loaded and instrumented
        // on the fly
        List<Class<?>> testClasses = loadTestClasses(testClassLoader, testClassName);

        //
        // Invoke test methods in each test class
        //
        for (Class<?> testClass : testClasses) {
            System.err.println("Test runner: Test class: " + testClass.getName());

            TestCase testClassInstance = (TestCase) testClass.newInstance();
            // TODO separate collect and invoke
            for (Method m : testClass.getDeclaredMethods()) {
                if (m.isSynthetic()) {
                    // skip synthetic method
                    continue;
                }

                if (m.getName().startsWith("test")) {
                    try {
                        // NOTE skip infinite, consider using timeout
                        if (!snippet.getMethod().getName().contains("infinite")) {
                            logger.trace("Invoking: " + m.getName());
                            // NOTE maybe thread and kill it thread if takes too much time?
                            invokeMethod(testClassInstance, m);
                        } else {
                            System.err.println("Not Invoking: " + m.getName());
                            logger.trace("Not invoking: " + m.getName());
                        }
                    } catch (InvocationTargetException ex) {
                        Throwable cause = ex.getCause();

                        if (cause instanceof NullPointerException
                                || cause instanceof ArrayIndexOutOfBoundsException
                                || cause instanceof AssertionFailedError) {
                            logger.warn(cause.getClass().getName() + ": "
                                    + m.getDeclaringClass().getName() + "." + m.getName());
                        } else {
                            logger.error("Exception: " + m.getDeclaringClass().getName() + "."
                                    + m.getName());
                        }
                        logger.debug(ex.getMessage(), ex);
                    }
                } else {
                    logger.warn("Not test method: {}", m.getName());
                }
            }
        }

        //
        // Collect data
        //
        ExecutionDataStore executionData = new ExecutionDataStore();
        SessionInfoStore sessionInfos = new SessionInfoStore();
        data.collect(executionData, sessionInfos, false);
        runtime.shutdown();

        // get classes to analyse
        // store string to avoid the mess up between the different class loaders
        Set<String> javaClasses = new HashSet<>();
        javaClasses.add(snippetClassName);

        for (Constructor<?> inclConstructor : snippet.getIncludedConstructors()) {
            javaClasses.add(inclConstructor.getDeclaringClass().getName());
        }

        for (Method inclMethod : snippet.getIncludedMethods()) {
            javaClasses.add(inclMethod.getDeclaringClass().getName());
        }

        // TODO inner classes are not handled well enough

        // TODO anonymous classes can also have anonymous classes -> recursion

        Set<String> toAdd = new HashSet<>();
        for (String javaClass : javaClasses) {
            int i = 1;
            while (true) {
                // guess anonymous classes, like ClassName$1, ClassName$2 etc.
                try {
                    testClassLoader.loadClass(javaClass + "$" + i);
                    toAdd.add(javaClass + "$" + i);
                    i++;
                } catch (ClassNotFoundException ex) {
                    // bad guess, no more anonymous classes on this level
                    break;
                }
            }
        }
        javaClasses.addAll(toAdd);

        // analyse classes
        CoverageBuilder coverageBuilder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(executionData, coverageBuilder);

        for (String javaClassName : javaClasses) {
            logger.trace("Analysing: {}", javaClassName);
            analyzer.analyzeClass(testClassLoader.readBytes(javaClassName), javaClassName);
        }

        // TODO remove debug
        // new File("D:/SETTE/!DUMP/" + getTool().getName()).mkdirs();
        // PrintStream out = new PrintStream("D:/SETTE/!DUMP/"
        // + getTool().getName() + "/" + testClassName + ".out");

        Map<String, Triple<TreeSet<Integer>, TreeSet<Integer>, TreeSet<Integer>>> coverageInfo = new HashMap<>();

        for (IClassCoverage cc : coverageBuilder.getClasses()) {
            String file = cc.getPackageName() + '/' + cc.getSourceFileName();
            file = file.replace('\\', '/');

            if (!coverageInfo.containsKey(file)) {
                coverageInfo.put(file, Triple.of(new TreeSet<Integer>(), new TreeSet<Integer>(),
                        new TreeSet<Integer>()));
            }

            // out.printf("Coverage of class %s%n", cc.getName());
            //
            // printCounter(out, "instructions",
            // cc.getInstructionCounter());
            // printCounter(out, "branches", cc.getBranchCounter());
            // printCounter(out, "lines", cc.getLineCounter());
            // printCounter(out, "methods", cc.getMethodCounter());
            // printCounter(out, "complexity", cc.getComplexityCounter());

            for (int l = cc.getFirstLine(); l <= cc.getLastLine(); l++) {
                switch (cc.getLine(l).getStatus()) {
                    case ICounter.FULLY_COVERED:
                        coverageInfo.get(file).getLeft().add(l);
                        break;

                    case ICounter.PARTLY_COVERED:
                        coverageInfo.get(file).getMiddle().add(l);
                        break;

                    case ICounter.NOT_COVERED:
                        coverageInfo.get(file).getRight().add(l);
                        break;

                    default:
                        // empty
                        break;
                }
            }
        }

        // decide result type
        Pair<ResultType, Double> resultTypeAndCoverage = decideResultType(snippet, coverageInfo);
        ResultType resultType = resultTypeAndCoverage.getLeft();
        double coverage = resultTypeAndCoverage.getRight();

        // create coverage XML
        SnippetCoverageXml coverageXml = new SnippetCoverageXml();
        coverageXml.setToolName(getTool().getName());
        coverageXml.setSnippetProjectElement(new SnippetProjectElement(
                getSnippetProjectSettings().getBaseDirectory().getCanonicalPath()));

        coverageXml.setSnippetElement(new SnippetElement(
                snippet.getContainer().getJavaClass().getName(), snippet.getMethod().getName()));

        coverageXml.setResultType(resultType);
        coverageXml.setAchievedCoverage(coverage);

        for (Entry<String, Triple<TreeSet<Integer>, TreeSet<Integer>, TreeSet<Integer>>> entry : coverageInfo
                .entrySet()) {
            TreeSet<Integer> full = entry.getValue().getLeft();
            TreeSet<Integer> partial = entry.getValue().getMiddle();
            TreeSet<Integer> not = entry.getValue().getRight();

            FileCoverageElement fce = new FileCoverageElement();
            fce.setName(entry.getKey());
            fce.setFullyCoveredLines(StringUtils.join(full, ' '));
            fce.setPartiallyCoveredLines(StringUtils.join(partial, ' '));
            fce.setNotCoveredLines(StringUtils.join(not, ' '));

            coverageXml.getCoverage().add(fce);
        }

        coverageXml.validate();

        // TODO needs more documentation
        File coverageFile = RunnerProjectUtils.getSnippetCoverageFile(getRunnerProjectSettings(),
                snippet);

        Serializer serializer = new Persister(new AnnotationStrategy(),
                new Format("<?xml version=\"1.0\" encoding= \"UTF-8\" ?>"));

        serializer.write(coverageXml, coverageFile);

        // generate html
        new HtmlGenerator().generate(snippet, coverageXml);

        return coverageXml;
    }

    private volatile Throwable invokeException;

    @SuppressWarnings("deprecation")
    private void invokeMethod(TestCase testClassInstance, Method m) throws Throwable {
        invokeException = null;

        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    m.invoke(testClassInstance);
                } catch (Throwable ex) {
                    invokeException = ex;
                }
            }
        };
        t.setName(testClassInstance.getClass().getSimpleName() + "_" + m.getName());

        t.start();
        // FIXME no more than 30 sec per test case
        t.join(30 * 1000);

        if (t.isAlive()) {
            // FIXME find a better way if possible (e.g. daemon thread, separate jvm, etc.)
            System.err.println("Stopped test: " + m.getName());
            try {
                t.stop();
            } catch (Throwable ex) {
                System.err.println("Thread Stop...");
                ex.printStackTrace();
            }
        }

        if (invokeException != null) {
            Throwable ex = invokeException;
            invokeException = null;
            throw ex;
        }
    }

    private Pair<ResultType, Double> decideResultType(Snippet snippet,
            Map<String, Triple<TreeSet<Integer>, TreeSet<Integer>, TreeSet<Integer>>> coverageInfo)
                    throws Exception {
        int linesToCover = 0;
        int linesCovered = 0;

        // iterate through files
        for (String relJavaFile : coverageInfo.keySet()) {
            // relJavaFile: hu/bme/mit/sette/snippets/_1_basic/B3_loops/B3c_DoWhile.java
            File javaFile = new File(getSnippetProjectSettings().getSnippetSourceDirectory(),
                    relJavaFile);

            // parse file
            CompilationUnit compilationUnit = JavaParser.parse(javaFile);
            int beginLine = compilationUnit.getBeginLine();
            int endLine = compilationUnit.getEndLine();

            // get "line colours" to variables
            int[] full = coverageInfo.get(relJavaFile).getLeft().stream().sorted()
                    .mapToInt(i -> (int) i).sorted().toArray();
            int[] partial = coverageInfo.get(relJavaFile).getMiddle().stream().sorted()
                    .mapToInt(i -> (int) i).sorted().toArray();
            int[] not = coverageInfo.get(relJavaFile).getRight().stream().sorted()
                    .mapToInt(i -> (int) i).sorted().toArray();

            // validations
            Validate.isTrue(beginLine >= 1, relJavaFile + " begin line: " + beginLine);
            Validate.isTrue(endLine > beginLine, relJavaFile + " end line: " + endLine);

            // lines store, set order is very important!
            LineStatuses lines = new LineStatuses(beginLine, endLine);
            lines.setStatus(not, ICounter.NOT_COVERED);
            lines.setStatus(partial, ICounter.PARTLY_COVERED);
            lines.setStatus(full, ICounter.FULLY_COVERED);

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
                    int s = lines.getStatus(lineNumber);

                    if (s != ICounter.EMPTY) {
                        linesToCover++;
                        if (s != ICounter.NOT_COVERED) {
                            linesCovered++;
                        }
                    }
                }
            } catch (NoSuchElementException ex) {
                // dependency file
                if (snippet.getIncludedConstructors().isEmpty()
                        && snippet.getIncludedMethods().isEmpty()) {
                    // nothing to do
                } else {
                    // handle included coverage
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
                            int s = lines.getStatus(lineNumber);

                            if (s != ICounter.EMPTY) {
                                linesToCover++;
                                if (s == ICounter.FULLY_COVERED || s == ICounter.PARTLY_COVERED) {
                                    linesCovered++;
                                }
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
            // File htmlFile = RunnerProjectUtils.getSnippetHtmlFile(getRunnerProjectSettings(),
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

    private static List<Class<?>> loadTestClasses(JaCoCoClassLoader classLoader,
            String testClassName) {
        List<Class<?>> testClasses = new ArrayList<>();

        Class<?> testClass = classLoader.tryLoadClass(testClassName);

        if (testClass != null) {
            // one class containing the test cases
            testClasses.add(testClass);
        } else {
            // one package containing the test cases
            String testPackageName = testClassName;
            // try to load the class for the test suite (it is not used by
            // SETTE)
            testClassName = testPackageName + ".Test";

            if (classLoader.tryLoadClass(testClassName) == null) {
                // no test suite class
                // TODO
                System.err.println("No test suite class for " + testClassName);
            }

            int i = 0;
            while (true) {
                // load the i-th test class (i starts at zero)
                testClass = classLoader.tryLoadClass(testClassName + i);

                if (testClass != null) {
                    testClasses.add(testClass);
                } else {
                    // the i-th test class does not exists
                    if (classLoader.tryLoadClass(testClassName + (i + 1)) != null) {
                        // but the (i+1)-th test class exists -> problem
                        throw new RuntimeException("i-th does not, but (i+1)-th exists! i=" + i);
                    } else {
                        // ok, all test classes were found
                        break;
                    }
                }

                i++;
            }
        }
        return testClasses;
    }

    private static int[] linesToArray(String lines) {
        return Stream.of(lines.split("\\s+")).filter(line -> !StringUtils.isBlank(line))
                .mapToInt(line -> Integer.parseInt(line)).sorted().toArray();
    }

    private static final class LineStatuses {
        private final int beginLine;
        private final int endLine;
        private final int[] lineStatuses;

        public LineStatuses(int beginLine, int endLine) {
            this.beginLine = beginLine;
            this.endLine = endLine;

            lineStatuses = new int[endLine - beginLine + 1];
            for (int i = 0; i < lineStatuses.length; i++) {
                lineStatuses[i] = ICounter.EMPTY;
            }
        }

        public int getStatus(int lineNumber) {
            Validate.isTrue(lineNumber >= beginLine);
            Validate.isTrue(lineNumber <= endLine);

            return lineStatuses[lineNumber - beginLine];
        }

        public void setStatus(int lineNumber, int status) {
            Validate.isTrue(lineNumber >= beginLine);
            Validate.isTrue(lineNumber <= endLine);

            lineStatuses[lineNumber - beginLine] = status;
        }

        public void setStatus(int[] lineNumbers, int status) {
            for (int lineNumber : lineNumbers) {
                setStatus(lineNumber, status);
            }
        }
    }

    private final class HtmlGenerator {
        public void generate(Snippet snippet, SnippetCoverageXml coverageXml) throws IOException {
            File htmlFile = RunnerProjectUtils.getSnippetHtmlFile(getRunnerProjectSettings(),
                    snippet);

            String htmlTitle = getTool().getName() + " - "
                    + snippet.getContainer().getJavaClass().getName() + '.'
                    + snippet.getMethod().getName() + "()";
            StringBuilder htmlData = new StringBuilder();
            htmlData.append("<!DOCTYPE html>\n");
            htmlData.append("<html lang=\"hu\">\n");
            htmlData.append("<head>\n");
            htmlData.append("       <meta charset=\"utf-8\" />\n");
            htmlData.append("       <title>" + htmlTitle + "</title>\n");
            htmlData.append("       <style type=\"text/css\">\n");
            htmlData.append("               .code { font-family: 'Consolas', monospace; }\n");
            htmlData.append(
                    "               .code .line { border-bottom: 1px dotted #aaa; white-space: pre; }\n");
            htmlData.append("               .code .green { background-color: #CCFFCC; }\n");
            htmlData.append("               .code .yellow { background-color: #FFFF99; }\n");
            htmlData.append("               .code .red { background-color: #FFCCCC; }\n");
            htmlData.append("               .code .line .number {\n");
            htmlData.append("                       display: inline-block;\n");
            htmlData.append("                       width:50px;\n");
            htmlData.append("                       text-align:right;\n");
            htmlData.append("                       margin-right:5px;\n");
            htmlData.append("               }\n");
            htmlData.append("       </style>\n");
            htmlData.append("</head>\n");
            htmlData.append("\n");
            htmlData.append("<body>\n");
            htmlData.append("       <h1>" + htmlTitle + "</h1>\n");

            for (FileCoverageElement fce : coverageXml.getCoverage()) {
                htmlData.append("       <h2>" + fce.getName() + "</h2>\n");
                htmlData.append("       \n");

                File src = new File(getSnippetProject().getSettings().getSnippetSourceDirectory(),
                        fce.getName());
                List<String> srcLines = FileUtils.readLines(src);

                int[] full = linesToArray(fce.getFullyCoveredLines());
                int[] partial = linesToArray(fce.getPartiallyCoveredLines());
                int[] not = linesToArray(fce.getNotCoveredLines());

                htmlData.append("       <div class=\"code\">\n");
                int i = 1;
                for (String srcLine : srcLines) {
                    String divClass = getLineDivClass(i, full, partial, not);
                    htmlData.append("               <div class=\"" + divClass
                            + "\"><div class=\"number\">" + i + "</div> " + srcLine + "</div>\n");
                    i++;
                }
                htmlData.append("       </div>\n\n");
            }

            htmlData.append("</body>\n");
            htmlData.append("</html>\n");

            FileUtils.write(htmlFile, htmlData);
        }

        private String getLineDivClass(int lineNumber, int[] full, int[] partial, int[] not) {
            if (Arrays.binarySearch(full, lineNumber) >= 0) {
                return "line green";
            } else if (Arrays.binarySearch(partial, lineNumber) >= 0) {
                return "line yellow";
            } else if (Arrays.binarySearch(not, lineNumber) >= 0) {
                return "line red";
            } else {
                return "line";
            }
        }
    }
}
