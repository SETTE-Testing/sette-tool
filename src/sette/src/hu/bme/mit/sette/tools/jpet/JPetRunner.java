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
package hu.bme.mit.sette.tools.jpet;

import hu.bme.mit.sette.common.exceptions.ConfigurationException;
import hu.bme.mit.sette.common.model.snippet.Snippet;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;
import hu.bme.mit.sette.common.tasks.RunnerProjectRunner;
import hu.bme.mit.sette.common.util.JavaFileUtils;
import hu.bme.mit.sette.common.util.process.ProcessRunner;
import hu.bme.mit.sette.common.util.process.ProcessRunnerListener;
import hu.bme.mit.sette.common.util.process.ProcessUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public final class JPetRunner extends RunnerProjectRunner<JPetTool> {

    public JPetRunner(SnippetProject snippetProject, File outputDirectory, JPetTool tool,
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
            // throw new SetteGeneralException("jPET ant build has failed");
            throw new RuntimeException("jPET ant build has failed");
        }

        getTool();
        // delete test cases directory
        File testCasesDirectory = JPetTool.getTestCasesDirectory(getRunnerProjectSettings());
        if (testCasesDirectory.exists()) {
            FileUtils.forceDelete(new File(getRunnerProjectSettings().getBaseDirectory(),
                    JPetTool.TESTCASES_DIRNAME));
        }
    }

    @Override
    protected void runOne(Snippet snippet, File infoFile, File outputFile, File errorFile)
            throws IOException, ConfigurationException {
        // TODO extract, make more clear
        File pet = getTool().getPetExecutable();

        getTool();
        File testCaseXml = JPetTool.getTestCaseXmlFile(getRunnerProjectSettings(), snippet);

        FileUtils.forceMkdir(testCaseXml.getParentFile());

        StringBuilder jPetName = new StringBuilder();

        jPetName.append(JavaFileUtils
                .packageNameToFilename(snippet.getContainer().getJavaClass().getName()));
        jPetName.append('.');
        jPetName.append(snippet.getMethod().getName());
        // params
        jPetName.append('(');

        for (Class<?> param : snippet.getMethod().getParameterTypes()) {
            System.err.println(param.getName() + "-" + param.getCanonicalName());
            jPetName.append(JPetTypeConverter.fromJava(param));
        }

        jPetName.append(')');

        // return type
        System.err.println(snippet.getMethod().getReturnType().getName() + "-"
                + snippet.getMethod().getReturnType().getCanonicalName());
        jPetName.append(JPetTypeConverter.fromJava(snippet.getMethod().getReturnType()));

        // TODO better way to create command

        // create command
        List<String> cmd = new ArrayList<>();
        cmd.add(pet.getAbsolutePath());

        cmd.add(jPetName.toString());

        cmd.add("-cp");
        cmd.add("build");

        cmd.add("-c");
        cmd.add("bck");
        cmd.add("10");

        cmd.add("-td");
        cmd.add("num");

        cmd.add("-d");
        cmd.add("-100000");
        cmd.add("100000");

        cmd.add("-l");
        cmd.add("ff");

        cmd.add("-v");
        cmd.add("2");
        cmd.add("-w");

        cmd.add("-tr");
        cmd.add("statements");

        cmd.add("-cc");
        cmd.add("yes");

        cmd.add("-xml");
        cmd.add(testCaseXml.getCanonicalPath());

        System.out.println("  command: " + StringUtils.join(cmd, ' '));

        // run process
        ProcessRunner pr = new ProcessRunner();
        pr.setCommand(cmd);

        pr.setWorkingDirectory(getRunnerProjectSettings().getBaseDirectory());
        pr.setTimeoutInMs(getTimeoutInMs());
        pr.setPollIntervalInMs(RunnerProjectRunner.POLL_INTERVAL);

        OutputWriter l = new OutputWriter(cmd.toString(), infoFile, outputFile, errorFile);
        pr.addListener(l);
        pr.execute();
    }

    @Override
    public void cleanUp() throws IOException {
        // TODO better search
        for (Integer pid : ProcessUtils.searchProcess("jpet/pet")) {
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
}
