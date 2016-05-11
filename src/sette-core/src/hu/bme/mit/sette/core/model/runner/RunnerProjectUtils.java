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

import org.apache.commons.lang3.Validate;

import com.google.common.base.Preconditions;

import hu.bme.mit.sette.core.model.snippet.Snippet;
import lombok.NonNull;

/**
 * Helper class for runner projects.
 */
@Deprecated
public final class RunnerProjectUtils {
    /** The relative path of the runner log file. */
    public static final String RUNNER_LOG_FILE = "runner.log";
    /** The extension for info files. */
    public static final String INFO_EXTENSION = "info";
    /** The extension for output files. */
    public static final String OUTPUT_EXTENSION = "out";
    /** The extension for error files. */
    public static final String ERROR_EXTENSOIN = "err";
    /** The extension for generated inputs XML files. */
    public static final String INPUTS_EXTENSION = "inputs.xml";
    /** The extension for result XML files. */
    public static final String RESULT_EXTENSION = "result.xml";
    /** The extension for coverage XML files. */
    public static final String COVERAGE_EXTENSION = "coverage.xml";

    /** Static class. */
    private RunnerProjectUtils() {
        throw new UnsupportedOperationException("Static class");
    }

    /**
     * Gets the runner log file.
     *
     * @param settings
     *            the settings of the runner project.
     * @return the runner log file
     */
    public static Path getRunnerLogFile(RunnerProjectSettings settings) {
        Validate.notNull(settings, "The settings must not be null");

        return settings.getRunnerOutputDir().resolve(RUNNER_LOG_FILE);
    }

    /**
     * Gets the base filename for the output files for a snippet.
     *
     * @param snippet
     *            the snippet (e.g. hu.bme.mit.sette.MyContainer.MySnippet)
     * @return the base filename for the output files (e.g. hu/bme/mit/sette/MyContainer_MySnippet)
     */
    public static String getSnippetBaseFilename(Snippet snippet) {
        Validate.notNull(snippet, "The snippet must not be null");

        return snippet.getContainer().getJavaClass().getName().replace('.', '/') + "_"
                + snippet.getMethod().getName();
    }

    /**
     * Gets a runner project file for the snippet.
     *
     * @param settings
     *            the settings of the runner project.
     * @param snippet
     *            the snippet (e.g. hu.bme.mit.sette.MyContainer.MySnippet)
     * @param extension
     *            the extension of the file (e.g. myExt)
     * @return the output file for the snippet (e.g.
     *         RUNNER_OUTPUT_DIR/hu/bme/mit/sette/MyContainer_MySnippet.myExt).
     */
    private static Path getSnippetFile(@NonNull RunnerProjectSettings settings,
            @NonNull Snippet snippet,
            @NonNull String extension) {
        Preconditions.checkArgument(!extension.isEmpty());

        String relativePath = getSnippetBaseFilename(snippet) + '.' + extension;

        return settings.getRunnerOutputDir().resolve(relativePath);
    }

    /**
     * Gets the info file for the snippet.
     *
     * @param settings
     *            the settings of the runner project.
     * @param snippet
     *            the snippet (e.g. hu.bme.mit.sette.MyContainer.MySnippet)
     * @return the output file for the snippet (e.g.
     *         RUNNER_OUTPUT_DIR/hu/bme/mit/sette/MyContainer_MySnippet.info).
     */
    public static Path getSnippetInfoFile(RunnerProjectSettings settings, Snippet snippet) {
        Validate.notNull(settings, "The settings must not be null");
        Validate.notNull(snippet, "The snippet must not be null");

        return getSnippetFile(settings, snippet, INFO_EXTENSION);
    }

    /**
     * Gets the output file for the snippet.
     *
     * @param settings
     *            the settings of the runner project.
     * @param snippet
     *            the snippet (e.g. hu.bme.mit.sette.MyContainer.MySnippet)
     * @return the output file for the snippet (e.g.
     *         RUNNER_OUTPUT_DIR/hu/bme/mit/sette/MyContainer_MySnippet.out).
     */
    public static Path getSnippetOutputFile(RunnerProjectSettings settings, Snippet snippet) {
        Validate.notNull(settings, "The settings must not be null");
        Validate.notNull(snippet, "The snippet must not be null");

        return getSnippetFile(settings, snippet, OUTPUT_EXTENSION);
    }

    /**
     * Gets the error file for the snippet.
     *
     * @param settings
     *            the settings of the runner project.
     * @param snippet
     *            the snippet (e.g. hu.bme.mit.sette.MyContainer.MySnippet)
     * @return the output file for the snippet (e.g.
     *         RUNNER_OUTPUT_DIR/hu/bme/mit/sette/MyContainer_MySnippet.err).
     */
    public static Path getSnippetErrorFile(RunnerProjectSettings settings, Snippet snippet) {
        Validate.notNull(settings, "The settings must not be null");
        Validate.notNull(snippet, "The snippet must not be null");

        return getSnippetFile(settings, snippet, ERROR_EXTENSOIN);
    }

    /**
     * Gets the input file for the snippet.
     *
     * @param settings
     *            the settings of the runner project.
     * @param snippet
     *            the snippet (e.g. hu.bme.mit.sette.MyContainer.MySnippet)
     * @return the output file for the snippet (e.g. RUNNER_OUTPUT_DIR/hu/bme/mit
     *         /sette/MyContainer_MySnippet.inputs.xml).
     */
    public static Path getSnippetInputsFile(RunnerProjectSettings settings, Snippet snippet) {
        Validate.notNull(settings, "The settings must not be null");
        Validate.notNull(snippet, "The snippet must not be null");

        return getSnippetFile(settings, snippet, INPUTS_EXTENSION);
    }

    /**
     * Gets the result file for the snippet.
     *
     * @param settings
     *            the settings of the runner project.
     * @param snippet
     *            the snippet (e.g. hu.bme.mit.sette.MyContainer.MySnippet)
     * @return the output file for the snippet (e.g. RUNNER_OUTPUT_DIR/hu/bme/mit
     *         /sette/MyContainer_MySnippet.result.xml).
     */
    public static Path getSnippetResultFile(RunnerProjectSettings settings, Snippet snippet) {
        Validate.notNull(settings, "The settings must not be null");
        Validate.notNull(snippet, "The snippet must not be null");

        return getSnippetFile(settings, snippet, RESULT_EXTENSION);
    }

    /**
     * Gets the coverage file for the snippet.
     *
     * @param settings
     *            the settings of the runner project.
     * @param snippet
     *            the snippet (e.g. hu.bme.mit.sette.MyContainer.MySnippet)
     * @return the output file for the snippet (e.g. RUNNER_OUTPUT_DIR/hu/bme/mit
     *         /sette/MyContainer_MySnippet.coverage.xml).
     */
    public static Path getSnippetCoverageFile(RunnerProjectSettings settings, Snippet snippet) {
        Validate.notNull(settings, "The settings must not be null");
        Validate.notNull(snippet, "The snippet must not be null");

        return getSnippetFile(settings, snippet, COVERAGE_EXTENSION);
    }

    /**
     * Gets the HTML file for the snippet.
     *
     * @param settings
     *            the settings of the runner project.
     * @param snippet
     *            the snippet (e.g. hu.bme.mit.sette.MyContainer.MySnippet)
     * @return the output file for the snippet (e.g. RUNNER_OUTPUT_DIR/hu/bme/mit
     *         /sette/MyContainer_MySnippet.coverage.xml).
     */
    public static Path getSnippetHtmlFile(RunnerProjectSettings settings, Snippet snippet) {
        Validate.notNull(settings, "The settings must not be null");
        Validate.notNull(snippet, "The snippet must not be null");

        // TODO extract HTML as constant and rethink location!
        return getSnippetFile(settings, snippet, "html");
    }
}
