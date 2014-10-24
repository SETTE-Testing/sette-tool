/*
 * SETTE - Symbolic Execution based Test Tool Evaluator
 *
 * SETTE is a tool to help the evaluation and comparison of symbolic execution
 * based test input generator tools.
 *
 * Budapest University of Technology and Economics (BME)
 *
 * Authors: Lajos Cseppentő <lajos.cseppento@inf.mit.bme.hu>, Zoltán Micskei
 * <micskeiz@mit.bme.hu>
 *
 * Copyright 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package hu.bme.mit.sette.tools.spf;

import hu.bme.mit.sette.common.exceptions.SetteConfigurationException;
import hu.bme.mit.sette.common.model.snippet.Snippet;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;
import hu.bme.mit.sette.common.tasks.RunnerProjectRunner;
import hu.bme.mit.sette.common.util.JavaFileUtils;
import hu.bme.mit.sette.common.util.process.ProcessRunner;
import hu.bme.mit.sette.common.util.process.ProcessRunnerListener;
import hu.bme.mit.sette.common.util.process.ProcessUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public final class SpfRunner extends RunnerProjectRunner<SpfTool> {
    public SpfRunner(SnippetProject snippetProject,
            File outputDirectory, SpfTool tool) {
        super(snippetProject, outputDirectory, tool);
    }

    @Override
    protected void afterPrepare() throws IOException {
        // TODO make simpler and better

        // ant build
        ProcessRunner pr = new ProcessRunner();
        pr.setPollIntervalInMs(1000);
        pr.setCommand(new String[] { "/bin/bash", "-c", "ant" });
        pr.setWorkingDirectory(getRunnerProjectSettings()
                .getBaseDirectory());

        pr.addListener(new ProcessRunnerListener() {
            @Override
            public void onTick(ProcessRunner processRunner,
                    long elapsedTimeInMs) {
                System.out
                        .println("ant build tick: " + elapsedTimeInMs);
            }

            @Override
            public void onIOException(ProcessRunner processRunner,
                    IOException e) {
                // TODO error handling
                e.printStackTrace();
            }

            @Override
            public void onComplete(ProcessRunner processRunner) {
                if (processRunner.getStdout().length() > 0) {
                    System.out.println("Ant build output:");
                    System.out
                            .println("========================================");
                    System.out.println(processRunner.getStdout()
                            .toString());
                    System.out
                            .println("========================================");
                }

                if (processRunner.getStderr().length() > 0) {
                    System.out.println("Ant build error output:");
                    System.out
                            .println("========================================");
                    System.out.println(processRunner.getStderr()
                            .toString());
                    System.out
                            .println("========================================");
                    System.out.println("Terminating");
                }
            }

            @Override
            public void onStdoutRead(ProcessRunner processRunner,
                    int charactersRead) {
                // not needed
            }

            @Override
            public void onStderrRead(ProcessRunner processRunner,
                    int charactersRead) {
                // not needed
            }
        });

        pr.execute();

        if (pr.getStderr().length() > 0) {
            // TODO error handling
            // throw new SetteGeneralException("SPF ant build has failed");
            throw new RuntimeException("SPF ant build has failed");
        }

        System.out.println("Ant build done, press enter to continue");
        new BufferedReader(new InputStreamReader(System.in)).readLine();
    }

    @Override
    protected void runOne(Snippet snippet, File infoFile,
            File outputFile, File errorFile) throws IOException,
            SetteConfigurationException {
        // TODO make better
        /*
         * e.g.:
         * 
         * java -jar /data/workspaces/spf/jpf-core/build/RunJPF.jar
         * +shell.port=4242
         * /data/workspaces/spf/GeneralLibrary-SPF/src/generallibrary
         * /spf/_1_base/BasePrimitiveTypes_twoParamBoolean.jpf
         */

        File runJPFJar = getTool().getToolJAR();

        String filenameBase = JavaFileUtils
                .packageNameToFilename(snippet.getContainer()
                        .getJavaClass().getName())
                + "_" + snippet.getMethod().getName();
        File configFile = new File(getRunnerProjectSettings()
                .getGeneratedDirectory(), filenameBase
                + JavaFileUtils.FILE_EXTENSION_SEPARATOR
                + JPFConfig.JPF_CONFIG_EXTENSION).getCanonicalFile();

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
        ProcessRunner pr = new ProcessRunner();
        pr.setCommand(cmd.toString().split("\\s+"));

        pr.setWorkingDirectory(getRunnerProjectSettings()
                .getBaseDirectory());
        pr.setTimeoutInMs(getTimeoutInMs());
        pr.setPollIntervalInMs(RunnerProjectRunner.POLL_INTERVAL);

        OutputWriter l = new OutputWriter(cmd.toString(), infoFile,
                outputFile, errorFile);
        pr.addListener(l);
        pr.execute();
    }

    @Override
    public void cleanUp() throws IOException {
        // TODO better search expression!
        for (Integer pid : ProcessUtils.searchProcess("RunJPF.jar")) {
            System.err.println("  Terminating stuck process (PID: "
                    + pid + ")");
            try {
                ProcessUtils.terminateProcess(pid);
            } catch (Exception e) {
                System.err.println("  Exception");
                e.printStackTrace();
            }
        }

        System.gc();
    }
}
