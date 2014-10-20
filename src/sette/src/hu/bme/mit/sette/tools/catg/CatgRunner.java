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
package hu.bme.mit.sette.tools.catg;

import hu.bme.mit.sette.common.exceptions.SetteException;
import hu.bme.mit.sette.common.model.snippet.Snippet;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;
import hu.bme.mit.sette.common.tasks.RunnerProjectRunner;
import hu.bme.mit.sette.common.util.JavaFileUtils;
import hu.bme.mit.sette.common.util.process.ProcessRunner;
import hu.bme.mit.sette.common.util.process.ProcessRunnerListener;
import hu.bme.mit.sette.common.util.process.ProcessUtils;
import hu.bme.mit.sette.common.validator.FileType;
import hu.bme.mit.sette.common.validator.FileValidator;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;

import java.io.File;
import java.io.IOException;

public final class CatgRunner extends RunnerProjectRunner<CatgTool> {
    private static final int TRIAL_COUNT = 100;

    public CatgRunner(SnippetProject snippetProject,
            File outputDirectory, CatgTool tool) {
        super(snippetProject, outputDirectory, tool);
    }

    @Override
    protected void afterPrepare() {
        // TODO make simpler and better
        // TODO extract ant builder to sette-base as a class
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
                // TODO handle error
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
            // TODO enchance error handling
            // throw new SetteGeneralException("CATG ant build has failed");
            throw new RuntimeException("CATG ant build has failed");
        }
    }

    @Override
    protected void runOne(Snippet snippet, File infoFile,
            File outputFile, File errorFile) throws IOException,
            ValidatorException {
        // TODO make better
        File concolic = new File(getRunnerProjectSettings()
                .getBaseDirectory(), "concolic").getCanonicalFile();
        concolic.setExecutable(true);

        new FileValidator(concolic).type(FileType.REGULAR_FILE)
        .executable(true).validate();

        String methodName = snippet.getContainer().getJavaClass()
                .getName()
                + "_" + snippet.getMethod().getName();

        String filename = JavaFileUtils.packageNameToFilename(snippet
                .getContainer().getJavaClass().getName())
                + "_"
                + snippet.getMethod().getName()
                + JavaFileUtils.FILE_EXTENSION_SEPARATOR
                + JavaFileUtils.JAVA_SOURCE_EXTENSION;

        File file = new File(getRunnerProjectSettings()
                .getGeneratedDirectory(), filename).getCanonicalFile();

        if (!file.exists()) {
            System.err.println("Not found: " + file.getCanonicalPath());
            System.err.println("Skipping: " + methodName);
            return;
        }

        // create command

        /*
         * e.g.:
         *
         * concolic 100 generallibrary.subpkg.Testcase_main_func_cls
         */

        StringBuilder cmd = new StringBuilder();

        cmd.append("./concolic ").append(CatgRunner.TRIAL_COUNT)
        .append(" ").append(methodName);

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
    public void cleanUp() throws IOException, SetteException {
        // TODO better search expression!
        for (Integer pid : ProcessUtils.searchProcess("Djanala.conf")) {
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
