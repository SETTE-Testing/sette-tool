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

import java.io.File;
import java.nio.file.Path;

import com.google.common.base.Preconditions;

import hu.bme.mit.sette.core.configuration.SetteConfigurationException;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tool.Tool;
import hu.bme.mit.sette.core.validator.PathType;
import hu.bme.mit.sette.core.validator.PathValidator;
import hu.bme.mit.sette.core.validator.ValidationContext;
import hu.bme.mit.sette.core.validator.ValidationException;
import lombok.Getter;
import lombok.NonNull;

/**
 * Stores settings for a runner project.
 *
 * @param <T>
 *            The type of the tool.
 */
public final class RunnerProjectSettings<T extends Tool> {
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
    private final T tool;

    /** The base directory of the runner project. */
    @Getter
    private final File baseDir;

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
            @NonNull Path outputDir, @NonNull T tool, @NonNull String tag) {
        Preconditions.checkArgument(!tag.trim().isEmpty(), "The tag must not be blank");
        Preconditions.checkArgument(!tag.contains("___"),
                "The tag must not contain the '___' substring");

        this.snippetProject = snippetProject;
        this.tool = tool;
        this.tag = tag;

        String projectName = String.format("%s___%s___%s", snippetProject.getName(),
                tool.getName(), tag).toLowerCase();
        this.baseDir = new File(outputDir.toFile(), projectName);
    }

    /**
     * Returns the name of the runner project.
     *
     * @return The name of the runner project.
     */
    public String getProjectName() {
        return this.baseDir.getName();
    }

    /**
     * Returns the snippet source directory.
     *
     * @return The snippet source directory.
     */
    public File getSnippetSourceDirectory() {
        return new File(this.baseDir, "snippet-src");
    }

    /**
     * Returns the snippet library directory.
     *
     * @return The snippet library directory.
     */
    public File getSnippetLibraryDirectory() {
        return new File(this.baseDir, "snippet-lib");
    }

    /**
     * Returns the binary directory.
     *
     * @return The binary directory.
     */
    public File getBinaryDirectory() {
        return new File(this.baseDir, RunnerProjectSettings.BINARY_DIRNAME);
    }

    /**
     * Returns the generated directory.
     *
     * @return The generated directory.
     */
    public File getGeneratedDirectory() {
        return new File(this.baseDir, RunnerProjectSettings.GENERATED_DIRNAME);
    }

    /**
     * Returns the runner output directory.
     *
     * @return The runner directory.
     */
    public File getRunnerOutputDirectory() {
        return new File(this.baseDir, RunnerProjectSettings.RUNNER_OUTPUT_DIRNAME);
    }

    /**
     * Returns the directory containing the tests.
     *
     * @return The directory containing the tests.
     */
    public File getTestDirectory() {
        return new File(this.baseDir, RunnerProjectSettings.TEST_DIRNAME);
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
            ValidationContext vc = new ValidationContext(this);

            // base directory
            PathValidator v1 = new PathValidator(this.baseDir.toPath());
            v1.type(PathType.DIRECTORY).readable(true).executable(true);
            vc.addValidator(v1);

            // snippet source directory
            PathValidator v2 = new PathValidator(this.getSnippetSourceDirectory().toPath());
            v2.type(PathType.DIRECTORY).readable(true).executable(true);
            vc.addValidator(v2);

            // snippet library directory
            if (this.getSnippetLibraryDirectory().exists()) {
                PathValidator v3 = new PathValidator(this.getSnippetLibraryDirectory().toPath())
                        .type(PathType.DIRECTORY).readable(true).executable(true);
                vc.addValidator(v3);
            }

            // generated directory
            if (this.getGeneratedDirectory().exists()) {
                PathValidator v4 = new PathValidator(this.getGeneratedDirectory().toPath())
                        .type(PathType.DIRECTORY).readable(true).executable(true);
                vc.addValidator(v4);
            }

            // runner output directory
            if (this.getRunnerOutputDirectory().exists()) {
                PathValidator v5 = new PathValidator(this.getRunnerOutputDirectory().toPath())
                        .type(PathType.DIRECTORY).readable(true).executable(true);
                vc.addValidator(v5);
            }

            // test directory
            if (this.getTestDirectory().exists()) {
                PathValidator v6 = new PathValidator(this.getTestDirectory().toPath())
                        .type(PathType.DIRECTORY).readable(true).executable(true);
                vc.addValidator(v6);
            }

            vc.validate();
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
            // base directory
            PathValidator v = new PathValidator(this.baseDir.toPath());
            v.type(PathType.NONEXISTENT);
            v.validate();
        } catch (ValidationException ex) {
            throw new SetteConfigurationException("The runner project already exists", ex);
        }
    }
}
