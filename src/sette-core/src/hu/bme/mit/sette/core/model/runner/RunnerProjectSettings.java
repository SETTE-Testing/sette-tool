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
package hu.bme.mit.sette.core.model.runner;

import java.nio.file.Path;

import com.google.common.base.Preconditions;

import hu.bme.mit.sette.core.configuration.SetteConfigurationException;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tool.Tool;
import hu.bme.mit.sette.core.util.io.PathUtils;
import hu.bme.mit.sette.core.validator.PathValidator;
import hu.bme.mit.sette.core.validator.ValidationException;
import hu.bme.mit.sette.core.validator.Validator;
import lombok.Getter;
import lombok.NonNull;

/**
 * Stores settings for a runner project.
 *
 * @param <T>
 *            The type of the tool.
 */
public final class RunnerProjectSettings {
    /** Name of the directory containing the compiled files of the runner project. */
    public static final String BINARY_DIRNAME = "build";

    /** Name of the directory containing the generated files by the runner. */
    public static final String GENERATED_DIRNAME = "gen";

    /** Name of the directory containing the runner's output. */
    public static final String RUNNER_OUTPUT_DIRNAME = "runner-out";

    /** Name of the directory containing the tests. */
    public static final String TEST_DIRNAME = "test";

    /** The snippet project. */
    @Getter
    private final SnippetProject snippetProject;

    /** The tool. */
    @Getter
    private final Tool tool;

    /** The base directory of the runner project. */
    @Getter
    private final Path baseDir;

    /** The tag for the runner project. */
    @Getter
    private final String tag;

    /**
     * Creates an instance of the object. The project will be located in the
     * <code>parentDirectory</code> in a subdirectory named as
     * <code>[snippet project name]___[tool name]___[tag]</code> (lowercase), e.g.:
     * 
     * <pre>
     * <code>
     * sette-snippets___random-tool___1st-run
     * sette-snippets___random-tool___2nd-run
     * sette-snippets___random-tool___3rd-run
     * sette-snippets___se-tool___1st-run
     * sette-snippets___se-tool___2nd-run
     * sette-snippets___se-tool___3rd-run
     * test-snippets___random-tool___1st-run
     * test-snippets___random-tool___2nd-run
     * test-snippets___se-tool___1st-run
     * </code>
     * </pre>
     *
     * @param snippetProject
     *            The snippet project.
     * @param outputDir
     *            the output directory
     * @param tool
     *            The tool.
     * @param tag
     */
    public RunnerProjectSettings(@NonNull SnippetProject snippetProject,
            @NonNull Path outputDir, @NonNull Tool tool, @NonNull String tag) {
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

    /**
     * Returns the name of the runner project.
     *
     * @return The name of the runner project.
     */
    public String getProjectName() {
        return baseDir.getFileName().toString();
    }

    /**
     * Returns the snippet source directory.
     *
     * @return The snippet source directory.
     */
    public Path getSnippetSourceDir() {
        return baseDir.resolve("snippet-src");
    }

    /**
     * Returns the snippet library directory.
     *
     * @return The snippet library directory.
     */
    public Path getSnippetLibraryDir() {
        return baseDir.resolve("snippet-lib");
    }

    /**
     * Returns the binary directory.
     *
     * @return The binary directory.
     */
    public Path getBinaryDir() {
        return baseDir.resolve(RunnerProjectSettings.BINARY_DIRNAME);
    }

    /**
     * Returns the generated directory.
     *
     * @return The generated directory.
     */
    public Path getGeneratedDir() {
        return baseDir.resolve(RunnerProjectSettings.GENERATED_DIRNAME);
    }

    /**
     * Returns the runner output directory.
     *
     * @return The runner directory.
     */
    public Path getRunnerOutputDir() {
        return baseDir.resolve(RunnerProjectSettings.RUNNER_OUTPUT_DIRNAME);
    }

    /**
     * Returns the directory containing the tests.
     *
     * @return The directory containing the tests.
     */
    public Path getTestDir() {
        return baseDir.resolve(RunnerProjectSettings.TEST_DIRNAME);
    }

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
}
