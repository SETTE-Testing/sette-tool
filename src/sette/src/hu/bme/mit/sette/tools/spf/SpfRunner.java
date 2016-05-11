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
package hu.bme.mit.sette.tools.spf;

import java.nio.file.Path;
import java.util.Arrays;

import hu.bme.mit.sette.core.configuration.SetteConfigurationException;
import hu.bme.mit.sette.core.model.runner.RunnerProject;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.tasks.AntExecutor;
import hu.bme.mit.sette.core.tasks.RunnerProjectRunnerBase;
import hu.bme.mit.sette.core.util.process.ProcessExecutionException;
import hu.bme.mit.sette.core.util.process.ProcessUtils;

public final class SpfRunner extends RunnerProjectRunnerBase<SpfTool> {
    public SpfRunner(RunnerProject runnerProject, SpfTool tool) {
        super(runnerProject, tool);
    }

    @Override
    public boolean shouldKillAfterTimeout() {
        return true;
    }

    @Override
    protected void afterPrepare() {
        // ant build
        AntExecutor.executeAnt(runnerProject.getBaseDir(), null);
    }

    @Override
    protected void runOne(Snippet snippet) throws SetteConfigurationException {
        // TODO make better
        /*
         * e.g.:
         * 
         * java -jar /data/workspaces/spf/jpf-core/build/RunJPF.jar +shell.port=4242
         * /data/workspaces/spf/GeneralLibrary-SPF/src/generallibrary
         * /spf/_1_base/BasePrimitiveTypes_twoParamBoolean.jpf
         */

        Path runJPFJar = tool.getToolJar();

        String filenameBase = snippet.getContainer().getJavaClass().getName().replace('.', '/')
                + "_" + snippet.getMethod().getName();
        Path configFile = runnerProject.getGeneratedDir()
                .resolve(filenameBase + ".jpf");

        // create command
        StringBuilder cmd = new StringBuilder();

        cmd.append("java -jar").append(' ');
        // cmd.append('"').append(runJPFJar.getCanonicalPath()).append('"').append(' ');
        // TODO if whitespace in jpf path?
        cmd.append(runJPFJar.toString()).append(' ');

        cmd.append("+shell.port=4242 ");
        cmd.append(configFile.toString());

        System.out.println("  command: " + cmd.toString());

        // run process
        executeToolProcess(snippet, Arrays.asList(cmd.toString().split("\\s+")));
    }

    @Override
    public void cleanUp() throws ProcessExecutionException {
        // TODO better search expression!
        ProcessUtils.searchAndTerminateProcesses("RunJPF.jar");
        System.gc();
    }
}
