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
package hu.bme.mit.sette.tools.evosuite;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import hu.bme.mit.sette.common.exceptions.ConfigurationException;
import hu.bme.mit.sette.common.model.snippet.Snippet;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;
import hu.bme.mit.sette.common.tasks.RunnerProjectRunner;
import hu.bme.mit.sette.common.util.process.ProcessRunner;
import hu.bme.mit.sette.common.util.process.ProcessRunnerListener;
import hu.bme.mit.sette.common.util.process.ProcessUtils;

public final class EvoSuiteRunner extends RunnerProjectRunner<EvoSuiteTool> {
    public EvoSuiteRunner(SnippetProject snippetProject, File outputDirectory, EvoSuiteTool tool,
            String runnerProjectTag) {
        super(snippetProject, outputDirectory, tool, runnerProjectTag);
    }

    @Override
    protected void afterPrepare() throws IOException {
        // TODO make simpler and better

        // ant build
        ProcessRunner pr = new ProcessRunner();
        pr.setPollIntervalInMs(1000);
        pr.setCommand(new String[] { "/bin/bash", "-c", "ant" });
        pr.setWorkingDirectory(getRunnerProjectSettings().getBaseDirectory());

        pr.addListener(new ProcessRunnerListener() {
            @Override
            public void onTick(ProcessRunner processRunner, long elapsedTimeInMs) {
                System.out.println("ant build tick: " + elapsedTimeInMs);
            }

            @Override
            public void onIOException(ProcessRunner processRunner, IOException ex) {
                // TODO error handling
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
            // TODO error handling
            // throw new SetteGeneralException("EvoSuite ant build has failed");
            throw new RuntimeException("EvoSuite ant build has failed");
        }

        // FIXME
        // System.out.println("Ant build done, press enter to continue");
        // new BufferedReader(new InputStreamReader(System.in)).readLine();
    }

    @Override
    protected void runOne(Snippet snippet, File infoFile, File outputFile, File errorFile)
            throws IOException, ConfigurationException {
        // TODO make better
        // e.g.

        // java -jar evosuite-0.2.0.jar -projectCP bin -Dassertions=false -Dsearch_budget=60
        // -class=hu.bme.mit.sette.snippets._1_basic.B2_conditionals.B2a_IfElse_oneParamBoolean
        // -Dlog_goals=true

        // java -jar evosuite-0.2.0.jar -projectCP bin;snippet-libs/sette-snippets-external.jar
        // -prefix hu.bme.mit.sette.snippets._6_others -Dsearch_budget=30 -Dassertions=false

        // additional parameter: -Dtarget_method

        File evosuiteJar = getTool().getToolJAR();

        // create command
        String classpath = "build";
        for (File lib : getSnippetProject().getFiles().getLibraryFiles()) {
            classpath += ":" + lib.getAbsolutePath();
        }

        int timelimit = (getTimeoutInMs() + 500) / 1000; // ceil

        // create command
        List<String> cmd = new ArrayList<>();
        cmd.add("java");
        cmd.add("-jar");
        cmd.add(evosuiteJar.getAbsolutePath());
        cmd.add("-projectCP");
        cmd.add(classpath);
        // NOTE default: cmd.add("-generateSuite");
        cmd.add("-class=" + snippet.getContainer().getJavaClass().getName());
        // cmd.add("-Dtarget_method" + snippet.getMethod().getName()); // TODO it does not seem to
        // work in EvoSuite
        cmd.add("-Dsearch_budget=" + timelimit);
        cmd.add("-Dassertions=false");
        cmd.add("-Dlog_goals=true");
        // NOTE default: cmd.add("-Dtest_format=JUNIT4"); // JUnit 3 is not working
        cmd.add("-Djunit_suffix=_" + snippet.getMethod().getName() + "_Test");
        cmd.add("-Dtest_dir=test");

        System.out.println("  command: " + StringUtils.join(cmd, ' '));

        // run process
        ProcessRunner pr = new ProcessRunner();
        pr.setCommand(cmd);

        pr.setWorkingDirectory(getRunnerProjectSettings().getBaseDirectory());
        // Randoop will stop generation at the given time limit (however, it
        // needs extra time for dumping test cases)
        pr.setTimeoutInMs(0);
        pr.setPollIntervalInMs(RunnerProjectRunner.POLL_INTERVAL);

        OutputWriter l = new OutputWriter(cmd.toString(), infoFile, outputFile, errorFile);
        pr.addListener(l);
        pr.execute();
    }

    @Override
    public void cleanUp() throws IOException {
        // TODO better search
        for (Integer pid : ProcessUtils.searchProcess("randoop")) {
            System.err.println("  Terminating stuck process (PID: " + pid + ")");
            try {
                ProcessUtils.terminateProcess(pid);
            } catch (Exception ex) {
                System.err.println("  Exception");
                ex.printStackTrace();
            }
        }

        System.gc();
    }

    /**
     * Gets the method name and parameter types string.
     *
     * @param method
     *            the method
     * @return the method name and parameter types string, e.g.
     *         <code>pkg.Cls.m(int[],java.lang.String[])</code>
     */
    private static String getMethodNameAndParameterTypesString(Method method) {
        // collect and join parameter type names
        String paramsString = Stream.of(method.getParameterTypes()).map(p -> p.getTypeName())
                .collect(Collectors.joining(","));

        // create string
        return String.format("%s.%s(%s)", method.getDeclaringClass().getName(), method.getName(),
                paramsString);
    }
}
