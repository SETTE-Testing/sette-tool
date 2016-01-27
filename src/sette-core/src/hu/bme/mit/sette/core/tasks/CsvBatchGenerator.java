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

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tool.Tool;
import hu.bme.mit.sette.core.tool.ToolRegister;
import hu.bme.mit.sette.core.util.io.PathUtils;

public final class CsvBatchGenerator {
    private final SnippetProject snippetProject;
    private final File outputDir;
    private final Tool[] tools;
    private final String[] runnerProjectTags;

    public CsvBatchGenerator(SnippetProject snippetProject, Path outputDir, String tools,
            String runnerProjectTags) {
        this.snippetProject = snippetProject;
        this.outputDir = outputDir.toFile();

        this.tools = Stream.of(tools.split(",")).sorted()
                .map(toolName -> ToolRegister.get(toolName)).toArray(Tool[]::new);

        this.runnerProjectTags = Stream.of(runnerProjectTags.split(",")).sorted()
                .toArray(String[]::new);
    }

    public void generateAll() throws Exception {
        List<File> filesToMerge = new ArrayList<>();

        // generate for each
        for (Tool tool : tools) {
            for (String tag : runnerProjectTags) {
                CsvGenerator gen = new CsvGenerator(snippetProject, outputDir.toPath(), tool, tag);
                System.err.println("CsvBatchGenerator.generate for: "
                        + gen.getRunnerProjectSettings().getProjectName());
                gen.generate();
                filesToMerge.add(gen.getCsvFile());
            }
        }

        // merge
        List<String> mergedLines = new ArrayList<>();
        for (File csvFile : filesToMerge) {
            System.err.println("Merging: " + csvFile);
            List<String> csvLines = PathUtils.readAllLines(csvFile.toPath());
            if (!mergedLines.isEmpty()) {
                csvLines.remove(0); // remove header if not first file
            }
            mergedLines.addAll(csvLines);
        }

        // write
        String mergedFilename = snippetProject.getName();
        mergedFilename += "___";
        mergedFilename += Stream.of(tools).map(tool -> tool.getName())
                .collect(Collectors.joining(","));
        mergedFilename += "___";
        mergedFilename += String.join(",", runnerProjectTags);
        mergedFilename += ".csv";

        File mergedFile = new File(outputDir, mergedFilename);

        System.err.println("Writing into: " + mergedFile);
        PathUtils.write(mergedFile.toPath(), mergedLines);
    }
}
