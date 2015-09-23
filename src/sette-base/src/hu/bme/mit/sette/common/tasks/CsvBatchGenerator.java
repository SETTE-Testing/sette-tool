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
package hu.bme.mit.sette.common.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import hu.bme.mit.sette.common.Tool;
import hu.bme.mit.sette.common.ToolRegister;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;

public final class CsvBatchGenerator {
    private final SnippetProject snippetProject;
    private final File outputDirectory;
    private final Tool[] tools;
    private final String[] runnerProjectTags;

    public CsvBatchGenerator(SnippetProject snippetProject, File outputDirectory, String tools,
            String runnerProjectTags) {
        this.snippetProject = snippetProject;
        this.outputDirectory = outputDirectory;

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
                CsvGenerator gen = new CsvGenerator(snippetProject, outputDirectory, tool, tag);
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
            List<String> csvLines = FileUtils.readLines(csvFile);
            if (!mergedLines.isEmpty()) {
                csvLines.remove(0); // remove header if not first file
            }
            mergedLines.addAll(csvLines);
        }

        // write
        String mergedFilename = snippetProject.getSettings().getProjectName();
        mergedFilename += "___";
        mergedFilename += Stream.of(tools).map(tool -> tool.getName())
                .collect(Collectors.joining(","));
        mergedFilename += "___";
        mergedFilename += String.join(",", runnerProjectTags);
        mergedFilename += ".csv";

        File mergedFile = new File(outputDirectory, mergedFilename);

        System.err.println("Writing into: " + mergedFile);
        FileUtils.writeLines(mergedFile, mergedLines);
    }
}
