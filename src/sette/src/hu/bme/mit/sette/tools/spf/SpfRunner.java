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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import hu.bme.mit.sette.core.configuration.SetteConfigurationException;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tasks.AntExecutor;
import hu.bme.mit.sette.core.tasks.RunnerProjectRunner;
import hu.bme.mit.sette.core.util.process.ProcessUtils;

public final class SpfRunner extends RunnerProjectRunner<SpfTool> {
    public SpfRunner(SnippetProject snippetProject, Path outputDir, SpfTool tool,
            String runnerProjectTag) {
        super(snippetProject, outputDir, tool, runnerProjectTag);
    }

    @Override
    protected void afterPrepare() throws IOException {
        // ant build
        AntExecutor.executeAnt(getRunnerProjectSettings().getBaseDir(), null);
    }

    @Override
    protected void runOne(Snippet snippet, File infoFile, File outputFile, File errorFile)
            throws IOException, SetteConfigurationException {
        // TODO make better
        /*
         * e.g.:
         * 
         * java -jar /data/workspaces/spf/jpf-core/build/RunJPF.jar +shell.port=4242
         * /data/workspaces/spf/GeneralLibrary-SPF/src/generallibrary
         * /spf/_1_base/BasePrimitiveTypes_twoParamBoolean.jpf
         */

        File runJPFJar = getTool().getToolJar().toFile();

        String filenameBase = snippet.getContainer().getJavaClass().getName().replace('.', '/')
                + "_" + snippet.getMethod().getName();
        File configFile = new File(getRunnerProjectSettings().getGeneratedDirectory(),
                filenameBase + ".jpf").getCanonicalFile();

        // create command
        StringBuilder cmd = new StringBuilder();

        cmd.append("java -jar").append(' ');
        // cmd.append('"').append(runJPFJar.getCanonicalPath()).append('"').append(' ');
        // TODO if whitespace in jpf path?
        cmd.append(runJPFJar.getCanonicalPath()).append(' ');

        cmd.append("+shell.port=4242 ");
        cmd.append(configFile.getCanonicalPath());

        System.out.println("  command: " + cmd.toString());

        // run process
        executeToolProcess(Arrays.asList(cmd.toString().split("\\s+")), infoFile, outputFile,
                errorFile);
    }

    @Override
    public void cleanUp() throws IOException {
        // TODO better search expression!
        ProcessUtils.searchAndTerminateProcesses("RunJPF.jar");
        System.gc();
    }
}
