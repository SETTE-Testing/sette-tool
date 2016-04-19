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

import java.io.File;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

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

import com.google.common.base.Splitter;
import com.google.common.collect.Queues;

import hu.bme.mit.sette.core.configuration.SetteToolConfiguration;
import hu.bme.mit.sette.core.model.runner.RunnerProjectSettings;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tool.Tool;
import junit.framework.AssertionFailedError;

/**
 * Agent to run one test case (as a separate JVM process). Output:
 * <ul>
 * <li>log on syserr (and possible output on syserr and sysout from the test case
 * <li>{@link #AGENT_JSON_INDICATOR} on sysout
 * <li>coverage result as a JSON on sysout
 * </ul>
 */
public final class TestSuiteRunnerForkAgent {
    public static final String AGENT_JSON_INDICATOR = "== TEST RESULT JSON ==";
    private static final AgentLogger log = new AgentLogger(System.err);

    public static void main(String[] args) {
        try {
            Thread.currentThread().setName("AGENT-MAIN");

            List<String> argsList = Arrays.asList(args);

            log.info("Args: " + argsList);
            TestSuiteRunnerForkAgent agent = new TestSuiteRunnerForkAgent(
                    Queues.newArrayDeque(argsList));

            log.info(String.format("Agent created for %s: %s_%s (project: %s)",
                    agent.snippet.getId(), agent.testClassName, agent.testMethodName,
                    agent.runnerProjectSettings.getProjectName()));

            CoverageInfo result = agent.analyze();
            String json = result.toJsonString();

            System.out.println(AGENT_JSON_INDICATOR);
            System.out.println(json);

            // make sure that no thread will keep the JVM running
            System.exit(0);
        } catch (Throwable ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private final SnippetProject snippetProject;
    private final Tool tool;
    private final RunnerProjectSettings runnerProjectSettings;
    private final Snippet snippet;
    private final String testClassName;
    private final String testMethodName;

    public TestSuiteRunnerForkAgent(Queue<String> args) throws Exception {
        // parse args
        Path snippetProjectDir = Paths.get(args.remove());
        Path outputDir = Paths.get(args.remove());
        List<String> toolConfig = Splitter.on('|').limit(3).splitToList(args.remove());
        String runnerProjectTag = args.remove();
        String snippetId = args.remove();
        testClassName = args.remove();
        testMethodName = args.remove();

        if (!args.isEmpty()) {
            throw new RuntimeException("Too many arguments: " + args);
        }

        // create context
        snippetProject = SnippetProject.parse(snippetProjectDir);
        tool = Tool.create(new SetteToolConfiguration(toolConfig.get(0), toolConfig.get(1),
                Paths.get(toolConfig.get(2))));
        runnerProjectSettings = new RunnerProjectSettings(snippetProject, outputDir, tool,
                runnerProjectTag);

        snippet = snippetProject.getSnippetContainers().stream()
                .flatMap(sc -> sc.getSnippets().values().stream())
                .filter(s -> s.getId().equals(snippetId)).findAny().get();
    }

    private CoverageInfo analyze()
            throws Throwable {
        //
        // Initialize
        //
        String snippetClassName = snippet.getContainer().getJavaClass().getName();
        String snippetMethodName = snippet.getMethod().getName();

        log.info("Snippet: " + snippetClassName + "#" + snippetMethodName + "()");
        log.info("Test: " + testClassName);

        // create JaCoCo runtime and instrumenter
        IRuntime runtime = new LoggerRuntime();
        Instrumenter instrumenter = new Instrumenter(runtime);

        // start runtime
        RuntimeData data = new RuntimeData();
        runtime.startup(data);

        // binary directories for the JaCoCoClassLoader
        File[] binaryDirectories = new File[2];
        binaryDirectories[0] = snippetProject.getBuildDir().toFile();
        binaryDirectories[1] = runnerProjectSettings.getBinaryDirectory();
        log.debug("Binary directories: " + Arrays.asList(binaryDirectories));

        // create class loader
        JaCoCoClassLoader testClassLoader = new JaCoCoClassLoader(binaryDirectories,
                instrumenter, snippetProject.getClassLoader());

        // load test class
        // snippet class and other dependencies will be loaded and instrumented on the fly
        Class<?> testClass = testClassLoader.loadClass(testClassName);

        log.info("Test runner: Test class: " + testClass.getName());

        Object testClassInstance = testClass.newInstance();

        Method testMethod = Stream.of(testClass.getDeclaredMethods())
                .filter(m -> m.getName().equals(testMethodName)).findAny().get();

        // call @Before
        // TODO only calls the first @Before
        for (Method m : testClass.getDeclaredMethods()) {
            for (Annotation a : m.getAnnotations()) {
                if (a.annotationType().equals(Before.class)) {
                    m.invoke(testClassInstance);
                    break;
                }
            }
        }

        try {
            // NOTE skip infinite, consider using timeout
            if (!snippet.getMethod().getName().contains("infinite")) {
                log.trace("Invoking: " + testMethod.getName());
                // NOTE maybe thread and kill it thread if takes too much time?
                // NOTE the method may write even to stderr and stdout
                TestSuiteRunnerHelper.invokeMethod(testClassInstance, testMethod, false);
                log.trace("Invoked: " + testMethod.getName());
            } else {
                log.info("Not Invoking: " + testMethod.getName());
                log.trace("Not invoking: " + testMethod.getName());
            }
        } catch (InvocationTargetException ex) {
            log.debug("Exception during invoke: " + testMethod.getName());
            log.debug(ex.getMessage());
            ex.printStackTrace();

            Throwable cause = ex.getCause();

            if (cause instanceof NullPointerException
                    || cause instanceof ArrayIndexOutOfBoundsException
                    || cause instanceof AssertionFailedError
                    || cause instanceof AssertionError) {
                log.warn(cause.getClass().getName() + ": "
                        + testMethod.getDeclaringClass().getName() + "." + testMethod.getName());
            } else {
                log.error("Exception: " + testMethod.getDeclaringClass().getName() + "."
                        + testMethod.getName());
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
            log.trace("Analysing: " + javaClassName);
            analyzer.analyzeClass(testClassLoader.readBytes(javaClassName), javaClassName);
        }

        // FIXME insane data structure
        // key: source file
        // value: tuple of FULLY, PARTLY and NOT covered line numbers sets (green, yellow, red)
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

        return new CoverageInfo(coverageInfo);
    }

    // simple logger for the agent
    private final static class AgentLogger {
        private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss,SSS");
        private final PrintStream out;

        public AgentLogger(PrintStream out) {
            this.out = out;
        }

        public void error(String message) {
            log("ERROR", message);
        }

        public void warn(String message) {
            log("WARN", message);
        }

        public void info(String message) {
            log("INFO", message);
        }

        public void debug(String message) {
            log("DEBUG", message);
        }

        public void trace(String message) {
            log("TRACE", message);
        }

        private void log(String level, String message) {
            String timestampStr = DATE_FORMAT.format(new Date());
            String line = String.format("%s\t[%s]\t%s\t%s", timestampStr,
                    Thread.currentThread().getName(), level, message);

            out.println(line);
        }

    }
}
