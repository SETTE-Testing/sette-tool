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

import static com.google.common.base.Preconditions.checkArgument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hu.bme.mit.sette.core.model.runner.RunnerProject;
import hu.bme.mit.sette.core.model.runner.RunnerProjectSettings;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tool.Tool;
import lombok.Getter;
import lombok.NonNull;

/**
 * Base class for SETTE evaluation tasks, i.e. steps of the whole workflow.
 *
 * @param <T>
 *            the type of the tool
 */
public abstract class EvaluationTaskBase<T extends Tool> implements EvaluationTask {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final T tool;

    /** The runner project. */
    @Getter
    protected final RunnerProject runnerProject;

    /** The runner project settings. */
    @Getter
    private final RunnerProjectSettings runnerProjectSettings;

    /**
     * Instantiates a new SETTE task.
     *
     * @param runnerProject
     *            the runner project
     * @param tool
     *            the tool
     */
    public EvaluationTaskBase(@NonNull RunnerProject runnerProject, @NonNull T tool) {
        checkArgument(runnerProject.getToolName().equals(tool.getName()));

        log.info(
                "Instantiated {} with {} and {}", getClass().getSimpleName(), runnerProject, tool);
        this.runnerProject = runnerProject;
        this.tool = tool;
        this.runnerProjectSettings = new RunnerProjectSettings(runnerProject.getSnippetProject(),
                runnerProject.getBaseDir().getParent(), tool, runnerProject.getTag());
    }

    @Override
    public SnippetProject getSnippetProject() {
        return runnerProject.getSnippetProject();
    }

    @Override
    public Tool getTool() {
        return tool;
    }
}
