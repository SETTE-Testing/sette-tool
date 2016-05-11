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
package hu.bme.mit.sette.core.tasks.testsuiterunner;

import static hu.bme.mit.sette.core.tasks.testsuiterunner.TestSuiteRunnerHelper.decideResultType;
import static hu.bme.mit.sette.core.tasks.testsuiterunner.TestSuiteRunnerHelper.invokeMethod;
import static hu.bme.mit.sette.core.tasks.testsuiterunner.TestSuiteRunnerHelper.loadTestClasses;
import static hu.bme.mit.sette.core.util.io.PathUtils.exists;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.IRuntime;
import org.jacoco.core.runtime.LoggerRuntime;
import org.jacoco.core.runtime.RuntimeData;
import org.junit.Before;

import com.google.common.collect.Lists;

import hu.bme.mit.sette.core.exceptions.TestSuiteRunnerException;
import hu.bme.mit.sette.core.model.runner.ResultType;
import hu.bme.mit.sette.core.model.runner.RunnerProject;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetContainer;
import hu.bme.mit.sette.core.model.xml.FileCoverageElement;
import hu.bme.mit.sette.core.model.xml.SnippetCoverageXml;
import hu.bme.mit.sette.core.model.xml.SnippetElement;
import hu.bme.mit.sette.core.model.xml.SnippetInputsXml;
import hu.bme.mit.sette.core.model.xml.SnippetProjectElement;
import hu.bme.mit.sette.core.model.xml.SnippetResultXml;
import hu.bme.mit.sette.core.tasks.AntExecutor;
import hu.bme.mit.sette.core.tasks.EvaluationTaskBase;
import hu.bme.mit.sette.core.tasks.TestSuiteGenerator;
import hu.bme.mit.sette.core.tool.Tool;
import hu.bme.mit.sette.core.util.io.PathUtils;
import hu.bme.mit.sette.core.util.process.ProcessExecutionResult;
import hu.bme.mit.sette.core.util.process.ProcessExecutor;
import hu.bme.mit.sette.core.util.process.SimpleProcessExecutorListener;
import hu.bme.mit.sette.core.util.xml.XmlUtils;
import hu.bme.mit.sette.core.validator.PathType;
import hu.bme.mit.sette.core.validator.PathValidator;
import hu.bme.mit.sette.core.validator.ValidationException;
import junit.framework.AssertionFailedError;
import lombok.Getter;
import lombok.Setter;

public final class TestSuiteRunner extends EvaluationTaskBase<Tool> {
    public static final int TEST_CASE_TIMEOUT_IN_MS = 30000;

    @Getter
    @Setter
    private Pattern snippetSelector = null;

    public TestSuiteRunner(RunnerProject runnerProject, Tool tool) {
        super(runnerProject, tool);
    }

    public final void analyze() throws Exception {
        if (!exists(runnerProject.getRunnerLogFile())) {
            throw new TestSuiteRunnerException(
                    "Run the tool on the runner project first (and then parse and generate tests)");
        }

        // ant build
        AntExecutor.executeAnt(runnerProject.getBaseDir(),
                TestSuiteGenerator.ANT_BUILD_TEST_FILENAME);

        // binary directories for the JaCoCoClassLoader
        Path[] binaryDirectories = {
                getSnippetProject().getBuildDir(),
                runnerProject.getBinaryDir()
        };
        log.debug("Binary directories: {}", (Object) binaryDirectories);

        // foreach containers
        for (SnippetContainer container : getSnippetProject().getSnippetContainers()) {
            // foreach snippets
            for (Snippet snippet : container.getSnippets().values()) {
                // FIXME duplicated in RunnerProjectRunner -> replace loop with proper iterator
                if (snippetSelector != null
                        && !snippetSelector.matcher(snippet.getId()).matches()) {
                    String msg = String.format("Skipping %s (--snippet-selector)", snippet.getId());
                    log.info(msg);
                    continue;
                }

                handleSnippet(snippet, binaryDirectories);
            }
        }

        // NOTE check whether all inputs and info files are created
        // foreach containers
        for (SnippetContainer container : getSnippetProject().getSnippetContainers()) {
            // foreach snippets
            for (Snippet snippet : container.getSnippets().values()) {
                // FIXME duplicated in RunnerProjectRunner (and above too) -> replace loop with
                // proper iterator
                if (snippetSelector != null
                        && !snippetSelector.matcher(snippet.getId()).matches()) {
                    String msg = String.format("Skipping %s (--snippet-selector)", snippet.getId());
                    log.info(msg);
                    continue;
                }

                Path resultXmlFile = runnerProject.snippet(snippet).getResultXmlFile();

                new PathValidator(resultXmlFile).type(PathType.REGULAR_FILE).validate();
            }
        }

        // TODO remove debug
        System.err.println("=> ANALYZE ENDED");
    }

    private void handleSnippet(Snippet snippet, Path[] binaryDirectories)
            throws Exception {
        Path inputsXmlFile = runnerProject.snippet(snippet).getInputsXmlFile();

        if (!exists(inputsXmlFile)) {
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
            inputsXml = XmlUtils.deserializeFromXml(SnippetInputsXml.class, inputsXmlFile);

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

            // save results xml
            SnippetResultXml resultXml = SnippetResultXml.createForWithResult(inputsXml,
                    inputsXml.getResultType(), reqCov);
            runnerProject.snippet(snippet).writeResultXml(resultXml);
            return;
        }

        if (inputsXml.getGeneratedInputCount() == 0) {
            // throw new RuntimeException("No inputs: " + inputsXmlFile);
        }

        // NOTE remove try-catch
        try {
            // analyze
            SnippetCoverageXml coverageXml = analyzeOne(snippet, inputsXml, binaryDirectories);

            // save results xml
            SnippetResultXml resultXml = SnippetResultXml.createForWithResult(inputsXml,
                    coverageXml.getResultType(), coverageXml.getAchievedCoverage());
            runnerProject.snippet(snippet).writeResultXml(resultXml);
        } catch (ValidationException ex) {
            System.err.println(ex.getMessage());
            throw new RuntimeException("Validation failed");
        } catch (Throwable ex) {
            // now dump and go on
            System.err.println("========================================================");
            ex.printStackTrace();
            System.err.println("========================================================");
            System.err.println("========================================================");
            if (ex instanceof ThreadDeath) {
                // ignore
            } else {
                throw new RuntimeException(ex);
            }
        }
    }

    private SnippetCoverageXml analyzeOne(Snippet snippet, SnippetInputsXml inputsXml,
            Path[] binaryDirectories)
                    throws Throwable {
        // FIXME allow fork defined on snippwet level (Env3_deadlock)
        // problem: this snippet goes into a native method and block-waiting, and cannot be
        // interrupted => run using agent
        if ((snippet.getContainer().isForkDuringEvaluation()
                || snippet.getId().equals("Env3_deadlock"))
                && inputsXml.getGeneratedInputCount() > 0) {
            // FIXME latter part is needed to use reflection-mode when there are no test cases
            // (to generate the html properly and do not produce NaN coverage)
            return analyzeOneWithAgent(snippet, binaryDirectories);
        } else {
            return analyzeOneWithReflection(snippet, binaryDirectories);
        }
    }

    private SnippetCoverageXml analyzeOneWithReflection(Snippet snippet, Path[] binaryDirectories)
            throws Throwable {
        //
        // Initialize
        //
        String snippetClassName = snippet.getContainer().getJavaClass().getName();
        String snippetMethodName = snippet.getMethod().getName();
        String testClassName = snippet.getContainer().getJavaClass().getName() + "_"
                + snippet.getMethod().getName() + "_Test";

        log.debug("Snippet: {}#{}()", snippetClassName, snippetMethodName);
        log.debug("Test: {}", testClassName);

        // create JaCoCo runtime and instrumenter
        IRuntime runtime = new LoggerRuntime();
        Instrumenter instrumenter = new Instrumenter(runtime);

        // start runtime
        RuntimeData data = new RuntimeData();
        runtime.startup(data);

        // create class loader
        JaCoCoClassLoader testClassLoader = new JaCoCoClassLoader(binaryDirectories,
                instrumenter, getSnippetProject().getClassLoader());
        // load test class
        // snippet class and other dependencies will be loaded and instrumented
        // on the fly
        List<Class<?>> testClasses = loadTestClasses(testClassLoader, testClassName);

        if (testClasses.isEmpty()) {
            log.error("No test class was found for: " + snippet.getId());
        }

        //
        // Invoke test methods in each test class
        //
        for (Class<?> testClass : testClasses) {
            System.err.println("Test runner: Test class: " + testClass.getName());

            Object testClassInstance = testClass.newInstance();

            // TODO separate collect and invoke
            for (Method m : testClass.getDeclaredMethods()) {
                if (m.isSynthetic()) {
                    // skip synthetic method
                    continue;
                }

                if (m.getName().startsWith("test")) {
                    // execute @Before
                    for (Method bm : testClass.getDeclaredMethods()) {
                        for (Annotation a : bm.getAnnotations()) {
                            if (a.annotationType().equals(Before.class)) {
                                bm.invoke(testClassInstance);
                                break;
                            }
                        }
                    }

                    try {
                        // NOTE skip infinite, consider using timeout
                        if (!snippet.getMethod().getName().contains("infinite")) {
                            log.trace("Invoking: " + m.getName());
                            // NOTE maybe thread and kill it thread if takes too much time?
                            invokeMethod(testClassInstance, m, true);
                            log.trace("Invoked: " + m.getName());
                        } else {
                            System.err.println("Not Invoking: " + m.getName());
                            log.trace("Not invoking: " + m.getName());
                        }
                    } catch (InvocationTargetException ex) {
                        log.debug("Exception during invoke: " + m.getName());
                        log.debug(ex.getMessage(), ex);

                        Throwable cause = ex.getCause();

                        if (cause instanceof NullPointerException
                                || cause instanceof ArrayIndexOutOfBoundsException
                                || cause instanceof AssertionFailedError
                                || cause instanceof AssertionError) {
                            log.warn(cause.getClass().getName() + ": "
                                    + m.getDeclaringClass().getName() + "." + m.getName());
                        } else {
                            log.error("Exception: " + m.getDeclaringClass().getName() + "."
                                    + m.getName());
                        }
                    }
                } else {
                    // NOTE LOG.warn("Not test method: {}", m.getName());
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
            log.trace("Analysing: {}", javaClassName);
            analyzer.analyzeClass(testClassLoader.readBytes(javaClassName), javaClassName);
        }

        // TODO remove debug
        // new File("D:/SETTE/!DUMP/" + tool.getName()).mkdirs();
        // PrintStream out = new PrintStream("D:/SETTE/!DUMP/"
        // + tool.getName() + "/" + testClassName + ".out");

        Map<String, Triple<SortedSet<Integer>, SortedSet<Integer>, SortedSet<Integer>>> coverageInfo = new HashMap<>();

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
                switch (LineStatus.fromJaCoCo(cc.getLine(l).getStatus())) {
                    case FULLY_COVERED:
                        coverageInfo.get(file).getLeft().add(l);
                        break;

                    case PARTLY_COVERED:
                        coverageInfo.get(file).getMiddle().add(l);
                        break;

                    case NOT_COVERED:
                        coverageInfo.get(file).getRight().add(l);
                        break;

                    default:
                        // empty
                        break;
                }
            }
        }

        SnippetCoverageXml coverageXml = createAndWriteCoverageXmlAndHtml(snippet,
                new CoverageInfo(coverageInfo));

        return coverageXml;
    }

    private SnippetCoverageXml analyzeOneWithAgent(Snippet snippet, Path[] binaryDirectories)
            throws Throwable {
        //
        // Initialize
        //
        String snippetClassName = snippet.getContainer().getJavaClass().getName();
        String snippetMethodName = snippet.getMethod().getName();
        String testClassName = snippet.getContainer().getJavaClass().getName() + "_"
                + snippet.getMethod().getName() + "_Test";

        log.debug("Snippet: {}#{}()", snippetClassName, snippetMethodName);
        log.debug("Test: {}", testClassName);

        // create JaCoCo runtime and instrumenter
        IRuntime runtime = new LoggerRuntime();
        Instrumenter instrumenter = new Instrumenter(runtime);

        // start runtime
        RuntimeData data = new RuntimeData();
        runtime.startup(data);

        // create class loader
        JaCoCoClassLoader testClassLoader = new JaCoCoClassLoader(binaryDirectories,
                instrumenter, getSnippetProject().getClassLoader());
        // load test class
        // snippet class and other dependencies will be loaded and instrumented
        // on the fly
        List<Class<?>> testClasses = loadTestClasses(testClassLoader, testClassName);

        if (testClasses.isEmpty()) {
            log.error("No test class was found for: " + snippet.getId());
        }

        // Collect test methods
        List<Method> testMethods = new ArrayList<>();

        for (Class<?> testClass : testClasses) {
            System.err.println("Test runner: Test class: " + testClass.getName());

            // TODO separate collect and invoke
            for (Method m : testClass.getDeclaredMethods()) {
                if (m.isSynthetic()) {
                    // skip synthetic method
                    continue;
                }

                if (m.getName().startsWith("test")) {
                    testMethods.add(m);
                } else {
                    // NOTE LOG.warn("Not test method: {}", m.getName());
                }
            }
        }

        Collections.sort(testMethods, new Comparator<Method>() {
            @Override
            public int compare(Method o1, Method o2) {
                int cmp = o1.getDeclaringClass().getName()
                        .compareTo(o2.getDeclaringClass().getName());
                if (cmp == 0) {
                    return o1.getName().compareTo(o2.getName());
                } else {
                    return cmp;
                }
            }
        });

        // Invoke test methods in each test class
        CoverageInfo mergedCoverageInfo = new CoverageInfo();
        for (Method method : testMethods) {
            CoverageInfo coverageInfo = executeOneTestCaseWithAgent(snippet, method);
            coverageInfo.data.forEach((filename, tuple) -> {
                if (!mergedCoverageInfo.data.containsKey(filename)) {
                    mergedCoverageInfo.data.put(filename, tuple);
                } else {
                    Triple<SortedSet<Integer>, SortedSet<Integer>, SortedSet<Integer>> mergedTuple;
                    mergedTuple = mergedCoverageInfo.data.get(filename);

                    mergedTuple.getLeft().addAll(tuple.getLeft());
                    mergedTuple.getMiddle().addAll(tuple.getMiddle());
                    mergedTuple.getRight().addAll(tuple.getRight());
                }
            });

        }

        // clean merged coverage info
        mergedCoverageInfo.data.forEach((filename, tuple) -> {
            // remove green and yellow from red
            tuple.getRight().removeAll(tuple.getLeft());
            tuple.getRight().removeAll(tuple.getMiddle());

            // remove green from yellow
            tuple.getMiddle().removeAll(tuple.getLeft());

            // condition "no intersections" will be checked during xml creation
        });

        return createAndWriteCoverageXmlAndHtml(snippet, mergedCoverageInfo);
    }

    private CoverageInfo executeOneTestCaseWithAgent(Snippet snippet, Method testMethod)
            throws Exception {
        // NOTE absolute/real paths because workdir will be the runner project dir for the agent

        List<String> command = Lists.newArrayList("java", "-cp");

        String classpath;
        Path setteJar = Paths.get("sette-all.jar");
        if (PathUtils.exists(setteJar)) {
            // normal run (alljar)
            classpath = setteJar.toRealPath().toString();
        } else {
            // run with current classpath (maybe Eclipse)
            URL[] urls = ((URLClassLoader) ClassLoader.getSystemClassLoader()).getURLs();
            classpath = Stream.of(urls).map(url -> url.getFile())
                    .collect((Collectors.joining(File.pathSeparator)));
        }

        command.add(classpath);
        command.add(TestSuiteRunnerForkAgent.class.getName());

        // parameters for the agent
        command.add(getSnippetProject().getBaseDir().toString());
        // sette-results dir
        command.add(runnerProject.getBaseDir().getParent().toString());
        String toolArg = String.format("%s|%s|%s", tool.getClass().getName(),
                tool.getName(), tool.getToolDir());
        command.add(toolArg);
        command.add(runnerProject.getTag());
        command.add(snippet.getId());
        command.add(testMethod.getDeclaringClass().getName());
        command.add(testMethod.getName());

        // create process
        log.info("Agent for {} {} {}", snippet.getName(), testMethod.getDeclaringClass().getName(),
                testMethod.getName());
        log.trace("Agent command: " + command);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(runnerProject.getBaseDir().toFile());

        // test case timeout + (10% but at least 5 sec)
        int processTimeout = TEST_CASE_TIMEOUT_IN_MS
                + (int) Math.max(5000, TEST_CASE_TIMEOUT_IN_MS * 0.1);
        ProcessExecutor exec = new ProcessExecutor(pb, processTimeout);

        SimpleProcessExecutorListener listener = new SimpleProcessExecutorListener();
        ProcessExecutionResult execResult = exec.execute(listener);

        if (execResult.isDestroyed()) {
            System.err.println("TIMEOUT for agent: " + String.join(" ", command));
            System.err.println("Exit code: " + execResult);
            System.err.println("== STDOUT ==============================");
            System.err.println(listener.getStdoutData());

            System.err.println("== STDERR ==============================");
            System.err.println(listener.getStderrData());
            System.err.println("========================================");

            throw new RuntimeException("TIMEOUT for agent: " + String.join(" ", command));
        } else if (execResult.getExitValue() != 0) {
            System.err.println("FAILURE for agent: " + String.join(" ", command));
            System.err.println("Result: " + execResult);
            System.err.println("== STDOUT ==============================");
            System.err.println(listener.getStdoutData());

            System.err.println("== STDERR ==============================");
            System.err.println(listener.getStderrData());
            System.err.println("========================================");

            throw new RuntimeException("FAILURE for agent: " + String.join(" ", command));
        }

        String output = listener.getStdoutData().toString();
        int jsonIdx = output.lastIndexOf(TestSuiteRunnerForkAgent.AGENT_JSON_INDICATOR);
        String jsonString = output
                .substring(jsonIdx + TestSuiteRunnerForkAgent.AGENT_JSON_INDICATOR.length());
        return CoverageInfo.fromJsonString(jsonString);
    }

    private SnippetCoverageXml createAndWriteCoverageXmlAndHtml(Snippet snippet,
            CoverageInfo coverageInfo) throws Exception {
        // decide result type
        Pair<ResultType, Double> resultTypeAndCoverage = decideResultType(snippet, coverageInfo);
        ResultType resultType = resultTypeAndCoverage.getLeft();
        double coverage = resultTypeAndCoverage.getRight();

        // FIXME hook to check snippets, but should be elsewhere
        if (tool.getClass().getSimpleName().equals("SnippetInputCheckerTool")) {
            if (resultType != ResultType.C) {
                System.err.println("FAILURE for Checker, not C: " + snippet.getId());
                throw new RuntimeException();
            }
        }

        // create coverage XML
        SnippetCoverageXml coverageXml = new SnippetCoverageXml();
        coverageXml.setToolName(tool.getName());
        coverageXml.setSnippetProjectElement(new SnippetProjectElement(
                getSnippetProject().getBaseDir().toFile().getCanonicalPath()));

        coverageXml.setSnippetElement(new SnippetElement(
                snippet.getContainer().getJavaClass().getName(), snippet.getMethod().getName()));

        coverageXml.setResultType(resultType);
        coverageXml.setAchievedCoverage(coverage);

        for (Entry<String, Triple<SortedSet<Integer>, SortedSet<Integer>, SortedSet<Integer>>> entry : coverageInfo.data
                .entrySet()) {
            SortedSet<Integer> full = entry.getValue().getLeft();
            SortedSet<Integer> partial = entry.getValue().getMiddle();
            SortedSet<Integer> not = entry.getValue().getRight();

            FileCoverageElement fce = new FileCoverageElement();
            fce.setName(entry.getKey());
            fce.setFullyCoveredLines(StringUtils.join(full, ' '));
            fce.setPartiallyCoveredLines(StringUtils.join(partial, ' '));
            fce.setNotCoveredLines(StringUtils.join(not, ' '));

            coverageXml.getCoverage().add(fce);
        }

        coverageXml.validate();

        // save coverage xml
        runnerProject.snippet(snippet).writeCoverageXml(coverageXml);

        // generate html
        new HtmlGenerator(this).generate(snippet, coverageXml);

        return coverageXml;
    }
}
