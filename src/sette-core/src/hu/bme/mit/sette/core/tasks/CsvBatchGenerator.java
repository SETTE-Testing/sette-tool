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
package hu.bme.mit.sette.core.tasks;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import hu.bme.mit.sette.core.model.runner.RunnerProject;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tool.Tool;
import hu.bme.mit.sette.core.util.io.PathUtils;

public final class CsvBatchGenerator {
    private final SnippetProject snippetProject;
    private final Path outputDir;
    private final List<Tool> tools;
    private final String[] runnerProjectTags;

    public CsvBatchGenerator(SnippetProject snippetProject, Path outputDir, List<Tool> tools,
            String runnerProjectTags) {
        this.snippetProject = snippetProject;
        this.outputDir = outputDir;

        this.tools = tools;

        this.runnerProjectTags = Stream.of(runnerProjectTags.split(",")).sorted()
                .toArray(String[]::new);
    }

    public void generateAll() throws Exception {
        List<Path> filesToMerge = new ArrayList<>();

        // generate for each
        for (Tool tool : tools) {
            for (String tag : runnerProjectTags) {
                RunnerProject runnerProject = new RunnerProject(snippetProject, outputDir,
                        tool.getName(), tag);
                CsvGenerator gen = new CsvGenerator(runnerProject, tool);
                System.err.println("CsvBatchGenerator.generate for: "
                        + runnerProject.getProjectName());
                try {
                    gen.generate();
                    filesToMerge.add(gen.getCsvFile());
                } catch (FileNotFoundException ex) {
                    // skip
                    System.err.println("Not found: " + ex.getMessage());
                }
            }
        }

        // merge
        List<String> mergedLines = new ArrayList<>();
        for (Path csvFile : filesToMerge) {
            System.err.println("Merging: " + csvFile);
            List<String> csvLines = PathUtils.readAllLines(csvFile);
            if (!mergedLines.isEmpty()) {
                csvLines.remove(0); // remove header if not first file
            }
            mergedLines.addAll(csvLines);
        }

        // write
        String mergedFilename = snippetProject.getName();
        mergedFilename += "___";
        mergedFilename += String.join(",", runnerProjectTags);
        mergedFilename += ".csv";

        Path mergedFile = outputDir.resolve(mergedFilename);

        System.err.println("Writing into: " + mergedFile);
        PathUtils.write(mergedFile, mergedLines);
    }
}
