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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import hu.bme.mit.sette.core.configuration.SetteConfigurationException;
import hu.bme.mit.sette.core.model.runner.RunnerProject;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.tasks.AntExecutor;
import hu.bme.mit.sette.core.tasks.RunnerProjectRunnerBase;

public final class EvoSuiteRunner extends RunnerProjectRunnerBase<EvoSuiteTool> {
    public EvoSuiteRunner(RunnerProject runnerProject, EvoSuiteTool tool) {
        super(runnerProject, tool);
    }

    @Override
    public boolean shouldKillAfterTimeout() {
        return false;
    }

    @Override
    protected void afterPrepare() {
        // ant build
        AntExecutor.executeAnt(runnerProject.getBaseDir(), null);
    }

    @Override
    protected void runOne(Snippet snippet) throws SetteConfigurationException {
        // TODO make better
        // e.g.

        // java -jar evosuite-0.2.0.jar -projectCP bin -Dassertions=false -Dsearch_budget=60
        // -class=hu.bme.mit.sette.snippets._1_basic.B2_conditionals.B2a_IfElse_oneParamBoolean
        // -Dlog_goals=true

        // java -jar evosuite-0.2.0.jar -projectCP bin;snippet-lib/sette-snippets-external.jar
        // -prefix hu.bme.mit.sette.snippets._6_others -Dsearch_budget=30 -Dassertions=false

        // additional parameter: -Dtarget_method

        Path evosuiteJar = tool.getToolJar();

        // create command
        String classpath = "build";
        for (Path lib : getSnippetProject().getJavaLibFiles()) {
            if (SystemUtils.IS_OS_WINDOWS) {
                classpath += ";" + lib.toString();
            } else {
                classpath += ":" + lib.toString();
            }
        }

        int timelimit = (getTimeoutInMs() + 500) / 1000; // ceil

        // create command
        List<String> cmd = new ArrayList<>();
        cmd.add("java");
        cmd.add("-jar");
        cmd.add(evosuiteJar.toString());
        cmd.add("-projectCP");
        cmd.add(classpath);
        // NOTE default: cmd.add("-generateSuite");
        // use the one-snippet-per-class version
        cmd.add("-class=" + snippet.getContainer().getJavaClass().getName() + "_"
                + snippet.getMethod().getName());
        // cmd.add("-Dtarget_method" + snippet.getMethod().getName()); // TODO it does not seem to
        // work in EvoSuite
        cmd.add("-Dsearch_budget=" + timelimit);
        // NOTE default: cmd.add("-Dassertions=true");
        // NOTE not working cmd.add("-Dlog_goals=true");
        // NOTE default: cmd.add("-Dtest_format=JUNIT4"); // JUnit 3 is not working
        cmd.add("-Djunit_suffix=_" + snippet.getMethod().getName() + "_Test");
        cmd.add("-Dtest_dir=test");
        cmd.add("-Dassertions=false");
        cmd.add("-Dminimization_timeout=10");
        cmd.add("-Djunit_check_timeout=10");
        cmd.add("-Dassertion_timeout=10");
        cmd.add("-Dshow_progress=false");

        System.out.println("  command: " + StringUtils.join(cmd, ' '));

        // run process
        executeToolProcess(snippet, cmd);
    }

    @Override
    public void cleanUp() {
        // NOTE not needed to handle processes, only try gc
        System.gc();
    }

    // NOTE was not used, revise and move/delete
    // /**
    // * Gets the method name and parameter types string.
    // *
    // * @param method
    // * the method
    // * @return the method name and parameter types string, e.g.
    // * <code>pkg.Cls.m(int[],java.lang.String[])</code>
    // */
    // private static String getMethodNameAndParameterTypesString(Method method) {
    // // collect and join parameter type names
    // String paramsString = Stream.of(method.getParameterTypes()).map(p -> p.getTypeName())
    // .collect(Collectors.joining(","));
    //
    // // create string
    // return String.format("%s.%s(%s)", method.getDeclaringClass().getName(), method.getName(),
    // paramsString);
    // }
}
