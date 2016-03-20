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
package hu.bme.mit.sette.core.model.runner;

import static com.google.common.base.Preconditions.checkArgument;

import java.nio.file.Path;
import java.text.Collator;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tool.Tool;
import lombok.Getter;
import lombok.NonNull;

// FIXME future replacement for RunnerProjectSettings abnd RunnerProjectUtils
/*
 * better api for runner projects
 * 
 * features
 *  - get dirs
 *  - get err/out/info files for a snippet
 *  - get  
 *  - can be created using objects AND also can be parsed from an existing directory
 *  - 
 */
public final class RunnerProject<T extends Tool> implements Comparable<RunnerProject<?>> {
    @Getter
    private final SnippetProject snippetProject;
    @Getter
    private final T tool;
    @Getter
    private final Path baseDir;
    @Getter
    private final String tag;

    public RunnerProject(@NonNull SnippetProject snippetProject,
            @NonNull Path outputDir, @NonNull T tool, @NonNull String tag) {
        Preconditions.checkArgument(!tag.trim().isEmpty(), "The tag must not be blank");
        Preconditions.checkArgument(!tag.contains("___"),
                "The tag must not contain the '___' substring");

        this.snippetProject = snippetProject;
        this.tool = tool;
        this.tag = tag;

        String projectName = String.format("%s___%s___%s", snippetProject.getName(),
                tool.getName(), tag).toLowerCase();
        this.baseDir = outputDir.resolve(projectName);
    }

    public String getProjectName() {
        return baseDir.getFileName().toString();
    }

    public Path getSnippetSourceDirectory() {
        return baseDir.resolve("snippet-src");
    }

    public Path getSnippetLibraryDirectory() {
        return baseDir.resolve("snippet-lib");
    }

    public Path getBinaryDirectory() {
        return baseDir.resolve("bin");
    }

    public Path getGeneratedDirectory() {
        return baseDir.resolve("gen");
    }

    public Path getRunnerOutputDirectory() {
        return baseDir.resolve("runner-out");
    }

    public Path getTestDirectory() {
        return baseDir.resolve("test");
    }

    public Path getInfoFile(@NonNull Snippet snippet) {
        return getSnippetFile(snippet, "info");
    }

    public Path getOutputFile(Snippet snippet) {
        return getSnippetFile(snippet, "out");
    }

    public Path getErrorOutputFile(Snippet snippet) {
        return getSnippetFile(snippet, "err");
    }

    public Path getInputsXmlFile(Snippet snippet) {
        return getSnippetFile(snippet, "inputs.xml");
    }

    public Path getResultXmlFile(Snippet snippet) {
        return getSnippetFile(snippet, "result.xml");
    }

    public Path getCoverageXmlFile(Snippet snippet) {
        return getSnippetFile(snippet, "coverage.xml");
    }

    public Path getCoverageHtmlFile(Snippet snippet) {
        return getSnippetFile(snippet, "html");
    }

    private Path getSnippetFile(@NonNull Snippet snippet, @NonNull String extension) {
        checkArgument(snippet.getContainer().getSnippetProject() == snippetProject);
        checkArgument(!extension.isEmpty());

        String baseName = snippet.getContainer().getJavaClass().getName().replace('.', '/') + "_"
                + snippet.getMethod().getName();
        String relativePath = baseName + '.' + extension;

        return getRunnerOutputDirectory().resolve(relativePath);
    }

    @Override
    public int compareTo(@NonNull RunnerProject<?> o) {
        // Total Commander-like sorting
        // ASCII '-' < '_', but proj___tool___tag should be before proj-extra___tool___tag
        return Collator.getInstance().compare(getProjectName(), o.getProjectName());
    }

    public static RunnerProject<Tool> parse(@NonNull Collection<SnippetProject> snippetProjects,
            @NonNull Collection<Tool> tools, @NonNull Path baseDir) {
        String dirName = baseDir.getFileName().toString();
        List<String> parts = Splitter.on("___").limit(3).splitToList(dirName);
        if (parts.size() < 3) {
            throw new RuntimeException("Not a valid runner project directory: " + baseDir);
        }

        String snippetProjectName = parts.get(0);
        String toolName = parts.get(1);
        String tag = parts.get(2);

        Optional<SnippetProject> snippetProject = snippetProjects.stream()
                .filter(sp -> sp.getName().equalsIgnoreCase(snippetProjectName)).findAny();

        Optional<Tool> tool = tools.stream()
                .filter(t -> t.getName().equalsIgnoreCase(toolName)).findAny();

        if (!snippetProject.isPresent()) {
            throw new RuntimeException("Unknown snippet project for " + baseDir);
        } else if (!tool.isPresent()) {
            throw new RuntimeException("Unknown tool for " + baseDir);
        } else if (tag.trim().isEmpty()) {
            throw new RuntimeException("Blank tag for: " + baseDir);
        }

        // baseDir.parent() == outputDir
        return new RunnerProject<Tool>(snippetProject.get(), baseDir.getParent(), tool.get(), tag);
    }
}
