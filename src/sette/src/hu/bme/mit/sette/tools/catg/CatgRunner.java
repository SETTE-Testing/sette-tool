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
package hu.bme.mit.sette.tools.catg;

import static hu.bme.mit.sette.core.util.io.PathUtils.exists;

import java.nio.file.Path;
import java.util.Arrays;

import hu.bme.mit.sette.core.SetteException;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tasks.AntExecutor;
import hu.bme.mit.sette.core.tasks.RunnerProjectRunnerBase;
import hu.bme.mit.sette.core.util.process.ProcessUtils;
import hu.bme.mit.sette.core.validator.PathValidator;
import hu.bme.mit.sette.core.validator.ValidationException;

public final class CatgRunner extends RunnerProjectRunnerBase<CatgTool> {
    private static final int TRIAL_COUNT = 100;

    public CatgRunner(SnippetProject snippetProject, Path outputDir, CatgTool tool,
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
    }

    @Override
    protected void runOne(Snippet snippet, Path infoFile, Path outputFile, Path errorFile)
            throws ValidationException {
        // TODO make better
        Path concolic = getRunnerProjectSettings().getBaseDir().resolve("concolic");
        concolic.toFile().setExecutable(true);

        PathValidator.forRegularFile(concolic, true, null, true, null).validate();

        String methodName = snippet.getContainer().getJavaClass().getName() + "_"
                + snippet.getMethod().getName();

        String filename = methodName.replace('.', '/') + ".java";

        Path file = getRunnerProjectSettings().getGeneratedDir().resolve(filename);

        if (!exists(file)) {
            System.err.println("Not found: " + file);
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

        cmd.append("./concolic ").append(CatgRunner.TRIAL_COUNT).append(" ").append(methodName);

        System.out.println("  command: " + cmd.toString());

        // run process
        executeToolProcess(Arrays.asList(cmd.toString().split("\\s+")), infoFile, outputFile,
                errorFile);
    }

    @Override
    public void cleanUp() throws SetteException {
        // TODO better search expression!
        ProcessUtils.searchAndTerminateProcesses("Djanala.conf");
        System.gc();
    }
}
