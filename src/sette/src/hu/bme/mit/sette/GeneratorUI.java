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
package hu.bme.mit.sette;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.Validate;

import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tasks.RunnerProjectGenerator;
import hu.bme.mit.sette.core.tool.Tool;
import hu.bme.mit.sette.run.Run;

public final class GeneratorUI implements BaseUI {
    private final RunnerProjectGenerator<?> generator;

    public GeneratorUI(SnippetProject snippetProject, Tool tool, String runnerProjectTag) {
        Validate.notNull(snippetProject, "Snippet project settings must not be null");
        Validate.notNull(tool, "The tool must not be null");
        generator = tool.createRunnerProjectGenerator(snippetProject, Run.OUTPUT_DIR,
                runnerProjectTag);
    }

    @Override
    public void run(BufferedReader in, PrintStream out) throws Exception {
        // directories
        File snippetProjectDir = generator.getSnippetProject().getBaseDir().toFile();
        File runnerProjectDir = generator.getSnippetProject().getBaseDir().toFile();

        out.println("Snippet project: " + snippetProjectDir);
        out.println("Runner project: " + runnerProjectDir);

        // backup output directory if it exists
        if (runnerProjectDir.exists()) {
            if (Run.CREATE_BACKUP) {
                // create
                doBackup(runnerProjectDir, out);
            } else if (Run.SKIP_BACKUP) {
                // skip
            } else {
                // ask
                out.print("The output directory exists. It will be deleted before generation. "
                        + "Would you like to make a backup? [yes] ");

                String line = in.readLine();

                if (line == null) {
                    out.println("EOF detected, exiting");
                    return;
                }

                if (!line.trim().equalsIgnoreCase("no")) {
                    doBackup(runnerProjectDir, out);
                }
            }
        }

        try {
            // generate runner project
            out.println("Starting generation");
            Files.deleteIfExists(runnerProjectDir.toPath());
            generator.generate();
            out.println("Generation successful");
        } catch (Exception ex) {
            out.println("Generation failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static void doBackup(File runnerProjectDir, PrintStream out) throws IOException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH;mm;ss");

        File backup = new File(runnerProjectDir.getParentFile(),
                runnerProjectDir.getName() + "___backup-" + dateFormat.format(new Date()))
                        .getCanonicalFile();

        if (runnerProjectDir.renameTo(backup)) {
            out.println("Backup location: " + backup);
        } else {
            throw new RuntimeException("Backup failed, exiting");
        }
    }
}
