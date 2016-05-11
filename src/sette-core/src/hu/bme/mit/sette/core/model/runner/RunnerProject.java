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

import java.nio.file.Path;
import java.text.Collator;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

import hu.bme.mit.sette.core.configuration.SetteConfigurationException;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.util.io.PathUtils;
import hu.bme.mit.sette.core.validator.PathValidator;
import hu.bme.mit.sette.core.validator.ValidationException;
import hu.bme.mit.sette.core.validator.Validator;
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
public final class RunnerProject implements Comparable<RunnerProject> {
    @Getter
    private final SnippetProject snippetProject;

    @Getter
    private final String toolName;

    @Getter
    private final Path baseDir;

    @Getter
    private final String tag;

    private final Map<Snippet, RunnerProjectSnippet> runnerProjectSnippetCache = new HashMap<>();

    public RunnerProject(@NonNull SnippetProject snippetProject,
            @NonNull Path outputDir, @NonNull String toolName, @NonNull String tag) {
        Preconditions.checkArgument(!tag.trim().isEmpty(), "The tag must not be blank");
        Preconditions.checkArgument(!tag.contains("___"),
                "The tag must not contain the '___' substring");

        this.snippetProject = snippetProject;
        this.toolName = toolName;
        this.tag = tag;

        String projectName = String.format("%s___%s___%s", snippetProject.getName(), toolName, tag)
                .toLowerCase();
        this.baseDir = outputDir.resolve(projectName);
    }

    public String getProjectName() {
        return baseDir.getFileName().toString();
    }

    public Path getSnippetSourceDir() {
        return baseDir.resolve("snippet-src");
    }

    public Path getSnippetLibraryDir() {
        return baseDir.resolve("snippet-lib");
    }

    public Path getBinaryDir() {
        return baseDir.resolve("build");
    }

    public Path getGeneratedDir() {
        return baseDir.resolve("gen");
    }

    public Path getRunnerOutputDir() {
        return baseDir.resolve("runner-out");
    }

    public Path getTestDir() {
        return baseDir.resolve("test");
    }

    public Path getRunnerLogFile() {
        return getRunnerOutputDir().resolve("runner.log");
    }

    public RunnerProjectSnippet snippet(@NonNull Snippet snippet) {
        if (runnerProjectSnippetCache.containsKey(snippet)) {
            return runnerProjectSnippetCache.get(snippet);
        } else {
            RunnerProjectSnippet rps = new RunnerProjectSnippet(this, snippet);
            runnerProjectSnippetCache.put(snippet, rps);
            return rps;
        }
    }

    // FIXME delete after properly moved to RunnerProjectSnippet
    // public Path getInfoFile(@NonNull Snippet snippet) {
    // return getSnippetFile(snippet, "info");
    // }
    //
    // public Path getOutputFile(Snippet snippet) {
    // return getSnippetFile(snippet, "out");
    // }
    //
    // public Path getErrorOutputFile(Snippet snippet) {
    // return getSnippetFile(snippet, "err");
    // }
    //
    // public Path getInputsXmlFile(Snippet snippet) {
    // return getSnippetFile(snippet, "inputs.xml");
    // }
    //
    // public Path getResultXmlFile(Snippet snippet) {
    // return getSnippetFile(snippet, "result.xml");
    // }
    //
    // public Path getCoverageXmlFile(Snippet snippet) {
    // return getSnippetFile(snippet, "coverage.xml");
    // }
    //
    // public Path getCoverageHtmlFile(Snippet snippet) {
    // return getSnippetFile(snippet, "html");
    // }
    //
    // private Path getSnippetFile(@NonNull Snippet snippet, @NonNull String extension) {
    // checkArgument(snippet.getContainer().getSnippetProject() == snippetProject);
    // checkArgument(!extension.isEmpty());
    //
    // String baseName = snippet.getContainer().getJavaClass().getName().replace('.', '/') + "_"
    // + snippet.getMethod().getName();
    // String relativePath = baseName + '.' + extension;
    //
    // return getRunnerOutputDir().resolve(relativePath);
    // }

    /**
     * Validates whether the runner project exists. This method does not check whether the
     * underlying snippet project exists.
     *
     * @throws SetteConfigurationException
     *             If the runner project does not exist or it has other file problems.
     */
    public void validateExists() throws SetteConfigurationException {
        try {
            Validator<?> v = Validator.of(this);

            PathValidator.forDirectory(baseDir, true, null, true).addTo(v);
            PathValidator.forDirectory(getSnippetSourceDir(), true, null, true).addTo(v);

            Path libraryDir = getSnippetLibraryDir();
            if (PathUtils.exists(libraryDir)) {
                PathValidator.forDirectory(libraryDir, true, null, true).addTo(v);
            }

            Path generatedDir = getGeneratedDir();
            if (PathUtils.exists(generatedDir)) {
                PathValidator.forDirectory(generatedDir, true, null, true).addTo(v);
            }

            Path runnerOutputDir = getRunnerOutputDir();
            if (PathUtils.exists(runnerOutputDir)) {
                PathValidator.forDirectory(runnerOutputDir, true, null, true).addTo(v);
            }

            Path testDir = getTestDir();
            if (PathUtils.exists(testDir)) {
                PathValidator.forDirectory(testDir, true, null, true).addTo(v);
            }

            v.validate();
        } catch (ValidationException ex) {
            throw new SetteConfigurationException(
                    "The runner project or a part of it does not exists or is not readable", ex);
        }
    }

    /**
     * Validates whether the runner project does not exist.
     *
     * @throws SetteConfigurationException
     *             If the runner project exists.
     */
    public void validateNotExists() throws SetteConfigurationException {
        try {
            PathValidator.forNonexistent(baseDir).validate();
        } catch (ValidationException ex) {
            throw new SetteConfigurationException("The runner project already exists", ex);
        }
    }

    @Override
    public int compareTo(@NonNull RunnerProject o) {
        // Total Commander-like sorting
        // ASCII '-' < '_', but proj___tool___tag should be before proj-extra___tool___tag
        return Collator.getInstance().compare(getProjectName(), o.getProjectName());
    }

    public static RunnerProject parse(@NonNull Collection<SnippetProject> snippetProjects,
            @NonNull Path baseDir) throws ValidationException {
        PathValidator.forDirectory(baseDir, true, true, true).validate();

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

        if (!snippetProject.isPresent()) {
            throw new RuntimeException("Unknown snippet project for " + baseDir);
        } else if (tag.trim().isEmpty()) {
            throw new RuntimeException("Blank tag for: " + baseDir);
        }

        // baseDir.parent() == outputDir
        return new RunnerProject(snippetProject.get(), baseDir.getParent(), toolName, tag);
    }

    @Override
    public String toString() {
        return String.format("RunnerProject [baseDir=%s]", baseDir);
    }
}
