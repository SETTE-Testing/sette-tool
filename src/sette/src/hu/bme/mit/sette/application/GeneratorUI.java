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
// NOTE revise this file
// NOTE revise this file
package hu.bme.mit.sette.application;

import java.io.File;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import hu.bme.mit.sette.core.tasks.RunnerProjectGenerator;
import hu.bme.mit.sette.core.util.io.PathUtils;

public final class GeneratorUI implements BaseUI {
    @Override
    public void execute(ExecutionContext context) throws Exception {
        RunnerProjectGenerator<?> generator = context.getTool().createRunnerProjectGenerator(
                context.getSnippetProject(), context.getOutputDir(),
                context.getRunnerProjectTag());

        // directories
        File snippetProjectDir = generator.getSnippetProject().getBaseDir().toFile();
        File runnerProjectDir = generator.getRunnerProjectSettings().getBaseDir();

        context.getOutput().println("Snippet project: " + snippetProjectDir);

        // backup output directory if it exists
        if (runnerProjectDir.exists()) {
            switch (context.getBackupPolicy()) {
                case ASK:
                    context.getOutput().print(
                            "The output directory exists. It will be deleted before generation. "
                                    + "Would you like to make a backup? [yes] ");

                    String line = context.getInput().readLine();

                    if (line == null) {
                        context.getOutput().println("EOF detected, exiting");
                        return;
                    }

                    if (!line.trim().equalsIgnoreCase("no")) {
                        doBackup(runnerProjectDir, context.getOutput());
                    }
                    break;

                case CREATE:
                    doBackup(runnerProjectDir, context.getOutput());
                    break;

                case SKIP:
                    // skip
                    break;

                default:
                    throw new UnsupportedOperationException(
                            "Unknown backup policy: " + context.getBackupPolicy());

            }
        }

        PathUtils.deleteIfExists(runnerProjectDir.toPath());

        try {
            // generate runner project
            context.getOutput().println("Starting generation");
            generator.generate();
            context.getOutput().println("Generation successful");
        } catch (Exception ex) {
            context.getOutput().println("Generation failed: " + ex.getMessage());
            throw ex;
        }
    }

    private static void doBackup(File runnerProjectDir, PrintStream out) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

        File backup = new File(runnerProjectDir.getParentFile(),
                runnerProjectDir.getName() + "___backup_" + dateFormat.format(new Date()))
                        .getAbsoluteFile();

        if (runnerProjectDir.renameTo(backup)) {
            out.println("Backup location: " + backup);
        } else {
            throw new RuntimeException("Backup failed, exiting");
        }
    }
}
