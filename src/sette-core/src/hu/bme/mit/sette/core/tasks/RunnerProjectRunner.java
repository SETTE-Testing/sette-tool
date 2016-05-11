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
package hu.bme.mit.sette.core.tasks;

import java.io.PrintStream;
import java.util.regex.Pattern;

import hu.bme.mit.sette.core.SetteException;
import hu.bme.mit.sette.core.exceptions.RunnerProjectRunnerException;

public interface RunnerProjectRunner extends EvaluationTask {
    int getTimeoutInMs();

    void setTimeoutInMs(int timeoutInMs);

    Pattern getSnippetSelector();

    void setSnippetSelector(Pattern snippetSelector);

    /**
     * Runs the runner project.
     *
     * @param loggerStream
     *            the logger stream
     * @throws RunnerProjectRunnerException
     *             if running fails
     */
    void run(PrintStream loggerStream) throws RunnerProjectRunnerException;

    /**
     * Cleans up the processes, i.e. kills undesired and stuck processes.
     *
     * @throws SetteException
     *             if a SETTE problem occurred
     */
    void cleanUp() throws SetteException;
}
