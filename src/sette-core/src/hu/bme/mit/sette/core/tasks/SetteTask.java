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
package hu.bme.mit.sette.core.tasks;

import java.io.File;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hu.bme.mit.sette.core.model.runner.RunnerProjectSettings;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tool.Tool;
import lombok.Getter;
import lombok.NonNull;

/**
 * Base class for SETTE tasks, i.e. phases of the whole workflow.
 *
 * @param <T>
 *            the type of the tool
 */
abstract class SetteTask<T extends Tool> {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /** The snippet project. */
    @Getter
    private final SnippetProject snippetProject;

    /** The runner project settings. */
    @Getter
    private final RunnerProjectSettings<T> runnerProjectSettings;

    /**
     * Instantiates a new SETTE task.
     *
     * @param snippetProject
     *            the snippet project
     * @param outputDirectory
     *            the output directory
     * @param tool
     *            the tool
     * @param runnerProjectTag
     *            tag for the runner project
     */
    public SetteTask(@NonNull SnippetProject snippetProject, @NonNull File outputDirectory,
            @NonNull T tool,
            @NonNull String runnerProjectTag) {
        this.snippetProject = snippetProject;
        this.runnerProjectSettings = new RunnerProjectSettings<>(snippetProject,
                outputDirectory, tool, runnerProjectTag);
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
