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
// TODO z revise this file
package hu.bme.mit.sette.common.model.runner;

import hu.bme.mit.sette.common.Tool;

/**
 * Represents a runner project.
 *
 * @param <T>
 *            The type of the tool.
 */
@Deprecated
// TODO remove class (take features to RunnerProjectUtils static class
public abstract class RunnerProject<T extends Tool> {
    // public static enum State {
    // CREATED, PARSE_STARTED, PARSED, PARSE_ERROR
    // };
    //
    // private State state;
    // private final RunnerProjectSettings<T> settings;
    // private final SnippetProject snippetProject;
    //
    // public RunnerProject(RunnerProjectSettings<T> settings,
    // SnippetProject snippetProject) {
    // Validate.notNull(pSettings, "The settings must not be null");
    // Validate.notNull(pSnippetProject,
    // "The snippet project must not be null");
    // this.state = State.CREATED;
    // this.settings = settings;
    // this.snippetProject = snippetProject;
    // }
    //
    // public final void parse() throws SetteConfigurationException,
    // ValidatorException {
    // // validate preconditions
    // this.validateState(State.CREATED);
    // this.settings.validateExists();
    //
    // // start parsing
    // this.state = State.PARSE_STARTED;
    //
    // // etc...
    // }
    //
    // public final State getState() {
    // return state;
    // }
    //
    // public final RunnerProjectSettings<T> getSettings() {
    // return settings;
    // }
    //
    // public final SnippetProject getSnippetProject() {
    // return snippetProject;
    // }
    //
    // private void validateState(State required) {
    // Validate.validState(this.state.equals(required),
    // "Invalid state (state: [%s], required: [%s])",
    // this.state, required);
    // }

}
