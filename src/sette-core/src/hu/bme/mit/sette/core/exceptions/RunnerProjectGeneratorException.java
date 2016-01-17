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
import hu.bme.mit.sette.core.tasks.RunnerProjectGenerator;

/**
 * Exception class for exceptions in connection with a runner project generator.
 */
public final class RunnerProjectGeneratorException extends SetteException {
    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 421499340908943263L;

    /** The runner project generator. */
    private final RunnerProjectGenerator<?> runnerProjectGenerator;

    /**
     * Instantiates a new runner project generator exception.
     *
     * @param message
     *            the message
     * @param generator
     *            the runner project generator
     */
    public RunnerProjectGeneratorException(String message, RunnerProjectGenerator<?> generator) {
        this(message, generator, null);
    }

    /**
     * Instantiates a new runner project generator exception.
     *
     * @param message
     *            the message
     * @param generator
     *            the runner project generator
     * @param cause
     *            the cause
     */
    public RunnerProjectGeneratorException(String message, RunnerProjectGenerator<?> generator,
            Throwable cause) {
        super(message, cause);
        runnerProjectGenerator = generator;
    }

    /**
     * Gets the runner project generator.
     *
     * @return the runner project generator
     */
    public RunnerProjectGenerator<?> getRunnerProjectGenerator() {
        return runnerProjectGenerator;
    }
}
