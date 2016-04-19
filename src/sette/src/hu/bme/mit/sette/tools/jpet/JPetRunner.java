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
package hu.bme.mit.sette.tools.jpet;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import hu.bme.mit.sette.core.configuration.SetteConfigurationException;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tasks.AntExecutor;
import hu.bme.mit.sette.core.tasks.RunnerProjectRunner;
import hu.bme.mit.sette.core.util.io.PathUtils;
import hu.bme.mit.sette.core.util.process.ProcessExecutionException;
import hu.bme.mit.sette.core.util.process.ProcessUtils;

public final class JPetRunner extends RunnerProjectRunner<JPetTool> {

    public JPetRunner(SnippetProject snippetProject, Path outputDir, JPetTool tool,
            String runnerProjectTag) {
        super(snippetProject, outputDir, tool, runnerProjectTag);
    }

    @Override
    public boolean shouldKillAfterTimeout() {
        return true;
    }

    @Override
    protected void afterPrepare() {
        // ant build
        AntExecutor.executeAnt(getRunnerProjectSettings().getBaseDir(), null);

        getTool();
        // delete test cases directory
        File testCasesDirectory = JPetTool.getTestCasesDirectory(getRunnerProjectSettings());
        if (testCasesDirectory.exists()) {
            Path dir = new File(getRunnerProjectSettings().getBaseDir(),
                    JPetTool.TESTCASES_DIRNAME).toPath();
            PathUtils.delete(dir);
        }
    }

    @Override
    protected void runOne(Snippet snippet, File infoFile, File outputFile, File errorFile)
            throws SetteConfigurationException {
        // TODO extract, make more clear
        File pet = getTool().getPetExecutable().toFile();

        getTool();
        File testCaseXml = JPetTool.getTestCaseXmlFile(getRunnerProjectSettings(), snippet);

        PathUtils.createDir(testCaseXml.getParentFile().toPath());

        StringBuilder jPetName = new StringBuilder();

        jPetName.append(snippet.getContainer().getJavaClass().getName().replace('.', '/'));
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
        cmd.add(testCaseXml.getAbsolutePath());

        System.out.println("  command: " + StringUtils.join(cmd, ' '));

        // run process
        executeToolProcess(cmd, infoFile, outputFile, errorFile);
    }

    @Override
    public void cleanUp() throws ProcessExecutionException {
        // TODO better search
        ProcessUtils.searchAndTerminateProcesses("jpet/pet");
        System.gc();
    }
}
