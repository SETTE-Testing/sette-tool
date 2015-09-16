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
// TODO z revise this file
package hu.bme.mit.sette.common.tasks;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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

import hu.bme.mit.sette.common.Tool;
import hu.bme.mit.sette.common.exceptions.TestSuiteRunnerException;
import hu.bme.mit.sette.common.model.parserxml.FileCoverageElement;
import hu.bme.mit.sette.common.model.parserxml.SnippetCoverageXml;
import hu.bme.mit.sette.common.model.parserxml.SnippetElement;
import hu.bme.mit.sette.common.model.parserxml.SnippetInputsXml;
import hu.bme.mit.sette.common.model.parserxml.SnippetProjectElement;
import hu.bme.mit.sette.common.model.runner.ResultType;
import hu.bme.mit.sette.common.model.runner.RunnerProjectUtils;
import hu.bme.mit.sette.common.model.snippet.Snippet;
import hu.bme.mit.sette.common.model.snippet.SnippetContainer;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public final class TestSuiteRunner extends SetteTask<Tool> {
    private static final Logger logger = LoggerFactory.getLogger(TestSuiteRunner.class);

    public TestSuiteRunner(SnippetProject snippetProject, File outputDirectory, Tool tool) {
        super(snippetProject, outputDirectory, tool);
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

        Serializer serializer = new Persister(new AnnotationStrategy());

        // binary directories for the JaCoCoClassLoader
        File[] binaryDirectories = new File[2];
        binaryDirectories[0] = getSnippetProject().getSettings().getSnippetBinaryDirectory();
        binaryDirectories[1] = getRunnerProjectSettings().getBinaryDirectory();
        logger.debug("Binary directories: {}", (Object) binaryDirectories);

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

                // toto remove try-catch
                try {
                    analyzeOne(snippet, binaryDirectories);
                } catch (Exception e) {
                    // now dump and go on
                    e.printStackTrace();
                }
            }
        }

        // TODO remove debug
        System.err.println("=> ANALYZE ENDED");
    }

    private void analyzeOne(Snippet snippet, File[] binaryDirectories) throws Exception {
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
        JaCoCoClassLoader classLoader = new JaCoCoClassLoader(binaryDirectories, instrumenter,
                getSnippetProject().getClassLoader());

        // load test class
        // snippet class and other dependencies will be loaded and instrumented
        // on the fly
        List<Class<?>> testClasses = loadTestClasses(classLoader, testClassName);

        // invoke test methods in each test class
        for (Class<?> testClass : testClasses) {
            TestCase testClassInstance = (TestCase) testClass.newInstance();
            // TODO separate collect and invoke
            for (Method m : testClass.getDeclaredMethods()) {
                if (m.isSynthetic()) {
                    // skip synthetic method
                    continue;
                }

                if (m.getName().startsWith("test")) {
                    logger.trace("Invoking: " + m.getName());
                    try {
                        m.invoke(testClassInstance);
                    } catch (InvocationTargetException e) {
                        Throwable cause = e.getCause();

                        if (cause instanceof NullPointerException
                                || cause instanceof ArrayIndexOutOfBoundsException
                                || cause instanceof AssertionFailedError) {
                            logger.warn(cause.getClass().getName() + ": "
                                    + m.getDeclaringClass().getName() + "." + m.getName());
                        } else {
                            logger.error("Exception: " + m.getDeclaringClass().getName() + "."
                                    + m.getName());
                        }
                        logger.debug(e.getMessage(), e);
                    }
                } else {
                    logger.warn("Not test method: {}", m.getName());
                }
            }
        }

        // collect data
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
                    classLoader.loadClass(javaClass + "$" + i);
                    toAdd.add(javaClass + "$" + i);
                    i++;
                } catch (ClassNotFoundException e) {
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
            analyzer.analyzeClass(classLoader.readBytes(javaClassName), javaClassName);
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

        // create coverage XML
        SnippetCoverageXml coverageXml = new SnippetCoverageXml();
        coverageXml.setToolName(getTool().getName());
        coverageXml.setSnippetProjectElement(new SnippetProjectElement(
                getSnippetProjectSettings().getBaseDirectory().getCanonicalPath()));

        coverageXml.setSnippetElement(new SnippetElement(
                snippet.getContainer().getJavaClass().getName(), snippet.getMethod().getName()));

        coverageXml.setResultType(ResultType.S);

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

        // TODO move HTML generation to another file/phase
        File htmlFile = RunnerProjectUtils.getSnippetHtmlFile(getRunnerProjectSettings(), snippet);

        String htmlTitle = getTool().getName() + " - " + snippetClassName + '.' + snippetMethodName
                + "()";
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

            SortedSet<Integer> full = linesToSortedSet(fce.getFullyCoveredLines());
            SortedSet<Integer> partial = linesToSortedSet(fce.getPartiallyCoveredLines());
            SortedSet<Integer> not = linesToSortedSet(fce.getNotCoveredLines());

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

        // htmlData.append(" <div class=\"line\"><div class=\"number\">1</div> package
        // samplesnippets;</div>\n");
        // htmlData.append(" <div class=\"line\"><div class=\"number\">2</div> </div>\n");
        // htmlData.append(" <div class=\"line\"><div class=\"number\">3</div> import
        // hu.bme.mit.sette.annotations.SetteIncludeCoverage;</div>\n");
        // htmlData.append(" <div class=\"line\"><div class=\"number\">4</div> import
        // hu.bme.mit.sette.annotations.SetteNotSnippet;</div>\n");
        // htmlData.append(" <div class=\"line\"><div class=\"number\">5</div> import
        // hu.bme.mit.sette.annotations.SetteRequiredStatementCoverage;</div>\n");
        // htmlData.append(" <div class=\"line\"><div class=\"number\">6</div> import
        // hu.bme.mit.sette.annotations.SetteSnippetContainer;</div>\n");
        // htmlData.append(" <div class=\"line\"><div class=\"number\">7</div> import
        // samplesnippets.inputs.SampleContainer_Inputs;</div>\n");
        // htmlData.append(" <div class=\"line\"><div class=\"number\">8</div> </div>\n");
        // htmlData.append(" <div class=\"line\"><div class=\"number\">9</div>
        // @SetteSnippetContainer(category = "X1",</div>\n");
        // htmlData.append(" <div class=\"line\"><div class=\"number\">10</div> goal = "Sample
        // snippet container",</div>\n");
        // htmlData.append(" <div class=\"line\"><div class=\"number\">11</div>
        // inputFactoryContainer = SampleContainer_Inputs.class)</div>\n");
        // htmlData.append(" <div class=\"line\"><div class=\"number\">12</div> public final class
        // SampleContainer {</div>\n");
        // htmlData.append(" <div class=\"line\"><div class=\"number\">13</div> private
        // SampleContainer() {</div>\n");
        // htmlData.append(" <div class=\"line red\"><div class=\"number\">14</div> throw new
        // UnsupportedOperationException("Static
        // class");</div>\n");
        // htmlData.append(" <div class=\"line\"><div class=\"number\">15</div> }</div>\n");
        // htmlData.append(" <div class=\"line\"><div class=\"number\">16</div> </div>\n");
        // htmlData.append(" <div class=\"line\"><div class=\"number\">17</div>
        // @SetteRequiredStatementCoverage(value = 100)</div>\n");
        // htmlData.append(" <div class=\"line green\"><div class=\"number\">18</div> public static
        // boolean snippet(int x) {</div>\n");
        // htmlData.append(" <div class=\"line yellow\"><div class=\"number\">19</div> if (20 * x +
        // 2 == 42) {</div>\n");
        // htmlData.append(" <div class=\"line green\"><div class=\"number\">20</div> return
        // true;</div>\n");
        // htmlData.append(" <div class=\"line green\"><div class=\"number\">21</div> } else
        // {</div>\n");
        // htmlData.append(" <div class=\"line green\"><div class=\"number\">22</div> return
        // false;</div>\n");
        // htmlData.append(" <div class=\"line green\"><div class=\"number\">23</div> }</div>\n");
        // htmlData.append(" <div class=\"line green\"><div class=\"number\">24</div> }</div>\n");
        // htmlData.append(" <div class=\"line\"><div class=\"number\">25</div> }</div>\n");
        // htmlData.append(" <div class=\"line\"><div class=\"number\">26</div> </div>\n");

        htmlData.append("</body>\n");
        htmlData.append("</html>\n");

        FileUtils.write(htmlFile, htmlData);
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

    // TODO needed anymore?
    // private void printCounter(PrintStream out, String unit,
    // ICounter counter) {
    // final Integer missed = Integer
    // .valueOf(counter.getMissedCount());
    // final Integer total = Integer.valueOf(counter.getTotalCount());
    // out.printf("%s of %s %s missed%n", missed, total, unit);
    // }

    private static SortedSet<Integer> linesToSortedSet(String lines) {
        SortedSet<Integer> sortedSet = new TreeSet<>();

        for (String line : lines.split("\\s+")) {
            if (StringUtils.isBlank(line)) {
                continue;
            }

            sortedSet.add(Integer.parseInt(line));
        }

        return sortedSet;
    }

    private static String getLineDivClass(int i, SortedSet<Integer> full,
            SortedSet<Integer> partial, SortedSet<Integer> not) {
        if (full.contains(i)) {
            return "line green";
        } else if (partial.contains(i)) {
            return "line yellow";
        } else if (not.contains(i)) {
            return "line red";
        } else {
            return "line";
        }
    }
}
