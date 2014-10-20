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
package hu.bme.mit.sette.common.tasks;

import hu.bme.mit.sette.common.Tool;
import hu.bme.mit.sette.common.model.runner.RunnerProjectSettings;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;
import hu.bme.mit.sette.common.model.snippet.SnippetProjectSettings;

import java.io.File;

import org.apache.commons.lang3.Validate;

/**
 * Base class for SETTE tasks, i.e. phases of the whole workflow.
 *
 * @param <T>
 *            the type of the tool
 */
abstract class SetteTask<T extends Tool> {
    /** The snippet project. */
    private final SnippetProject snippetProject;

    /** The runner project settings. */
    private final RunnerProjectSettings<T> runnerProjectSettings;

    /**
     * Instantiates a new SETTE task.
     *
     * @param pSnippetProject
     *            the snippet project
     * @param outputDirectory
     *            the output directory
     * @param tool
     *            the tool
     */
    public SetteTask(final SnippetProject pSnippetProject,
            final File outputDirectory, final T tool) {
        Validate.notNull(pSnippetProject,
                "The snippet project must not be null");
        Validate.isTrue(
                pSnippetProject.getState().equals(
                        SnippetProject.State.PARSED),
                        "The snippet project must be parsed (state: [%s]) ",
                        pSnippetProject.getState().name());

        Validate.notNull(outputDirectory,
                "The output directory must not be null");
        Validate.notNull(tool, "The tool must not be null");

        this.snippetProject = pSnippetProject;
        this.runnerProjectSettings = new RunnerProjectSettings<>(
                pSnippetProject.getSettings(), outputDirectory, tool);
    }

    /**
     * Gets the snippet project.
     *
     * @return the snippet project
     */
    public final SnippetProject getSnippetProject() {
        return this.snippetProject;
    }

    /**
     * Gets the snippet project settings.
     *
     * @return the snippet project settings
     */
    public final SnippetProjectSettings getSnippetProjectSettings() {
        return this.snippetProject.getSettings();
    }

    /**
     * Gets the runner project settings.
     *
     * @return the runner project settings
     */
    public final RunnerProjectSettings<T> getRunnerProjectSettings() {
        return this.runnerProjectSettings;
    }

    /**
     * Gets the tool.
     *
     * @return the tool
     */
    public final T getTool() {
        return this.runnerProjectSettings.getTool();
    }
}
