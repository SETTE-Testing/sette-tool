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
// TODO z revise this file
// TODO z revise this file
// TODO z revise this file
package hu.bme.mit.sette;

import hu.bme.mit.sette.common.Tool;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;
import hu.bme.mit.sette.common.tasks.RunnerProjectGenerator;
import hu.bme.mit.sette.run.Run;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;

public final class GeneratorUI implements BaseUI {
    private final RunnerProjectGenerator<?> generator;

    public GeneratorUI(SnippetProject snippetProject, Tool tool) {
        Validate.notNull(snippetProject, "Snippet project settings must not be null");
        Validate.notNull(tool, "The tool must not be null");
        generator = tool.createRunnerProjectGenerator(snippetProject, Run.OUTPUT_DIR);
    }

    @Override
    public void run(BufferedReader in, PrintStream out) throws Exception {
        // directories
        File snippetProjectDir = generator.getSnippetProjectSettings().getBaseDirectory();
        File runnerProjectDir = generator.getRunnerProjectSettings().getBaseDirectory();

        out.println("Snippet project: " + snippetProjectDir);
        out.println("Runner project: " + runnerProjectDir);

        // backup output directory if it exists
        if (runnerProjectDir.exists()) {
            out.print(
                    "The output directory exists. It will be deleted before generation. Would you like to make a backup? [yes] ");

            String line = in.readLine();

            if (line == null) {
                out.println("EOF detected, exiting");
                return;
            }

            if (!line.trim().equalsIgnoreCase("no")) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH;mm;ss");

                File backup = new File(runnerProjectDir.getParentFile(),
                        runnerProjectDir.getName() + "-backup-" + dateFormat.format(new Date()))
                                .getCanonicalFile();

                if (runnerProjectDir.renameTo(backup)) {
                    out.println("Backup location: " + backup);
                } else {
                    out.println("Backup failed, exiting");
                    return;
                }
            }
        }

        try {
            // generate runner project
            out.println("Starting generation");
            FileUtils.deleteDirectory(runnerProjectDir);
            generator.generate();
            out.println("Generation successful");
        } catch (Exception e) {
            out.println("Generation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
