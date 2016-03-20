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
package hu.bme.mit.sette.application;

/**
 * Tasks (commands) for the SETTE.
 */
public enum ApplicationTask {
    EXIT(false, false, false),
    GENERATOR(true, true, true),
    RUNNER(true, true, true),
    PARSER(true, true, true),
    TEST_GENERATOR(true, true, true),
    TEST_RUNNER(true, true, true),
    SNIPPET_BROWSER(true, false, false),
    EXPORT_CSV(true, true, true),
    EXPORT_CSV_BATCH(true, false, false),
    RUNNER_PROJECT_BROWSER(false, false, false);

    private final boolean requiresSnippetProject;
    private final boolean requiresTool;
    private final boolean requiresRunnerProjectTag;

    private ApplicationTask(boolean requiresSnippetProject, boolean requiresTool,
            boolean requiresRunnerProjectTag) {
        this.requiresSnippetProject = requiresSnippetProject;
        this.requiresTool = requiresTool;
        this.requiresRunnerProjectTag = requiresRunnerProjectTag;
    }

    public boolean requiresSnippetProject() {
        return requiresSnippetProject;
    }

    public boolean requiresTool() {
        return requiresTool;
    }

    public boolean requiresRunnerProjectTag() {
        return requiresRunnerProjectTag;
    }

    @Override
    public String toString() {
        return name().replace('_', '-').toLowerCase();
    }
}
