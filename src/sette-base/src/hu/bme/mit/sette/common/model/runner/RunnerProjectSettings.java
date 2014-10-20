/*
 * SETTE - Symbolic Execution based Test Tool Evaluator
 *
 * SETTE is a tool to help the evaluation and comparison of symbolic execution
 * based test input generator tools.
 *
 * Budapest University of Technology and Economics (BME)
 *
 * Authors: Lajos Cseppentő <lajos.cseppento@inf.mit.bme.hu>, Zoltán Micskei
 * <micskeiz@mit.bme.hu>
 *
 * Copyright 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package hu.bme.mit.sette.common.model.runner;

import hu.bme.mit.sette.common.Tool;
import hu.bme.mit.sette.common.exceptions.SetteConfigurationException;
import hu.bme.mit.sette.common.model.snippet.SnippetProjectSettings;
import hu.bme.mit.sette.common.validator.FileType;
import hu.bme.mit.sette.common.validator.FileValidator;
import hu.bme.mit.sette.common.validator.GeneralValidator;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;

import java.io.File;

import org.apache.commons.lang3.Validate;

/**
 * Stores settings for a runner project.
 *
 * @param <T>
 *            The type of the tool.
 */
public final class RunnerProjectSettings<T extends Tool> {
    /**
     * Name of the directory containing the compiled files of the runner
     * project.
     */
    public static final String BINARY_DIRNAME = "build";
    /** Name of the directory containing the generated files by the runner. */
    public static final String GENERATED_DIRNAME = "gen";
    /** Name of the directory containing the runner's output. */
    public static final String RUNNER_OUTPUT_DIRNAME = "runner-out";
    /** Name of the directory containing the tests. */
    public static final String TESTS_DIRNAME = "tests";

    /** The settings of the snippet project. */
    private final SnippetProjectSettings snippetProjectSettings;
    /** The tool. */
    private final T tool;
    /** The base directory. */
    private final File baseDirectory;

    /**
     * Creates an instance of the object.
     *
     * @param pSnippetProjectSettings
     *            The settings of the snippet project.
     * @param pBaseDirectory
     *            the base directory
     * @param pTool
     *            The tool.
     */
    public RunnerProjectSettings(
            final SnippetProjectSettings pSnippetProjectSettings,
            final File pBaseDirectory, final T pTool) {
        Validate.notNull(pSnippetProjectSettings,
                "Snippet project settings must not be null");
        Validate.notNull(pBaseDirectory,
                "The base directory must not be null");
        Validate.notNull(pTool, "The tool must not be null");

        this.snippetProjectSettings = pSnippetProjectSettings;
        this.tool = pTool;

        String projectName = pSnippetProjectSettings.getProjectName()
                + '-' + pTool.getName().toLowerCase();
        this.baseDirectory = new File(pBaseDirectory, projectName);
    }

    /**
     * Returns the settings of the snippet project.
     *
     * @return The settings of the snippet project.
     */
    public SnippetProjectSettings getSnippetProjectSettings() {
        return this.snippetProjectSettings;
    }

    /**
     * Returns the tool.
     *
     * @return The tool.
     */
    public T getTool() {
        return this.tool;
    }

    /**
     * Returns the name of the runner project.
     *
     * @return The name of the runner project.
     */
    public String getProjectName() {
        return this.baseDirectory.getName();
    }

    /**
     * Returns the base directory.
     *
     * @return The base directory.
     */
    public File getBaseDirectory() {
        return this.baseDirectory;
    }

    /**
     * Returns the snippet source directory.
     *
     * @return The snippet source directory.
     */
    public File getSnippetSourceDirectory() {
        return new File(this.baseDirectory,
                snippetProjectSettings.getSnippetSourceDirectoryPath());
    }

    /**
     * Returns the snippet library directory.
     *
     * @return The snippet library directory.
     */
    public File getSnippetLibraryDirectory() {
        return new File(this.baseDirectory,
                snippetProjectSettings.getLibraryDirectoryPath());
    }

    /**
     * Returns the binary directory.
     *
     * @return The binary directory.
     */
    public File getBinaryDirectory() {
        return new File(this.baseDirectory,
                RunnerProjectSettings.BINARY_DIRNAME);
    }

    /**
     * Returns the generated directory.
     *
     * @return The generated directory.
     */
    public File getGeneratedDirectory() {
        return new File(this.baseDirectory,
                RunnerProjectSettings.GENERATED_DIRNAME);
    }

    /**
     * Returns the runner output directory.
     *
     * @return The runner directory.
     */
    public File getRunnerOutputDirectory() {
        return new File(this.baseDirectory,
                RunnerProjectSettings.RUNNER_OUTPUT_DIRNAME);
    }

    /**
     * Returns the tests directory.
     *
     * @return The tests directory.
     */
    public File getTestsDirectory() {
        return new File(this.baseDirectory,
                RunnerProjectSettings.TESTS_DIRNAME);
    }

    /**
     * Validates whether the runner project exists. This method does not check
     * whether the underlying snippet project exists.
     *
     * @throws SetteConfigurationException
     *             If the runner project does not exist or it has other file
     *             problems.
     */
    public void validateExists() throws SetteConfigurationException {
        try {
            GeneralValidator validator = new GeneralValidator(this);

            // base directory
            FileValidator v1 = new FileValidator(this.baseDirectory);
            v1.type(FileType.DIRECTORY).readable(true).executable(true);
            validator.addChildIfInvalid(v1);

            // snippet source directory
            FileValidator v2 = new FileValidator(
                    this.getSnippetSourceDirectory());
            v2.type(FileType.DIRECTORY).readable(true).executable(true);
            validator.addChildIfInvalid(v2);

            // snippet library directory
            if (this.getSnippetLibraryDirectory().exists()) {
                FileValidator v3 = new FileValidator(
                        this.getSnippetLibraryDirectory())
                .type(FileType.DIRECTORY).readable(true)
                .executable(true);
                validator.addChildIfInvalid(v3);
            }

            // generated directory
            if (this.getGeneratedDirectory().exists()) {
                FileValidator v4 = new FileValidator(
                        this.getGeneratedDirectory())
                .type(FileType.DIRECTORY).readable(true)
                .executable(true);
                validator.addChildIfInvalid(v4);
            }

            // runner output directory
            if (this.getRunnerOutputDirectory().exists()) {
                FileValidator v5 = new FileValidator(
                        this.getRunnerOutputDirectory())
                .type(FileType.DIRECTORY).readable(true)
                .executable(true);
                validator.addChildIfInvalid(v5);
            }

            // tests directory
            if (this.getTestsDirectory().exists()) {
                FileValidator v6 = new FileValidator(
                        this.getTestsDirectory())
                .type(FileType.DIRECTORY).readable(true)
                .executable(true);
                validator.addChildIfInvalid(v6);
            }

            validator.validate();
        } catch (ValidatorException e) {
            throw new SetteConfigurationException(
                    "The runner project or a part of it "
                            + "does not exists or is not readable", e);
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
            FileValidator v = new FileValidator(this.baseDirectory);
            v.type(FileType.NONEXISTENT);
            v.validate();
        } catch (ValidatorException e) {
            throw new SetteConfigurationException(
                    "The runner project already exists", e);
        }
    }
}
