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
package hu.bme.mit.sette.core.exceptions;

import hu.bme.mit.sette.core.SetteException;
import hu.bme.mit.sette.core.tasks.RunnerProjectRunner;

/**
 * Exception class for exceptions in connection with a runner project runner.
 */
public final class RunnerProjectRunnerException extends SetteException {
    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 3488315983289061614L;

    /** The runner project runner. */
    private final RunnerProjectRunner<?> runnerProjectRunner;

    /**
     * Instantiates a new runner project runner exception.
     *
     * @param message
     *            the message
     * @param runner
     *            the runner project runner
     */
    public RunnerProjectRunnerException(String message, RunnerProjectRunner<?> runner) {
        this(message, runner, null);
    }

    /**
     * Instantiates a new runner project runner exception.
     *
     * @param message
     *            the message
     * @param runner
     *            the runner project runner
     * @param cause
     *            the cause
     */
    public RunnerProjectRunnerException(String message, RunnerProjectRunner<?> runner,
            Throwable cause) {
        super(message, cause);
        runnerProjectRunner = runner;
    }

    /**
     * Gets the runner project runner.
     *
     * @return the runner project runner
     */
    public RunnerProjectRunner<?> getRunnerProjectRunner() {
        return runnerProjectRunner;
    }
}
