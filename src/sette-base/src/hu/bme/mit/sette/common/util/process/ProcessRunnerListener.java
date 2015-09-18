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
package hu.bme.mit.sette.common.util.process;

import java.io.IOException;

/**
 * The listener interface for receiving {@link ProcessRunner} events.
 *
 * @see ProcessRunner
 */
public interface ProcessRunnerListener {
    /**
     * Called when characters were read from the process' stdout.
     *
     * @param processRunner
     *            the {@link ProcessRunner} object
     * @param charactersRead
     *            the number of the read characters
     */
    default void onStdoutRead(ProcessRunner processRunner, int charactersRead) {
        // default: do nothing, read data can be received from the ProcessRunner on completion
    }

    /**
     * Called when characters were read from the process' stderr.
     *
     * @param processRunner
     *            the {@link ProcessRunner} object
     * @param charactersRead
     *            the number of the read characters
     */
    default void onStderrRead(ProcessRunner processRunner, int charactersRead) {
        // default: do nothing, read data can be received from the ProcessRunner on completion
    }

    /**
     * Called when {@link IOException} occurred.
     *
     * @param processRunner
     *            the {@link ProcessRunner} object
     * @param exception
     *            the exception
     */
    void onIOException(ProcessRunner processRunner, IOException exception);

    /**
     * Called when a poll interval has elapsed in the process runner.
     *
     * @param processRunner
     *            the {@link ProcessRunner} object
     * @param elapsedTimeInMs
     *            the elapsed time in ms
     */
    void onTick(ProcessRunner processRunner, long elapsedTimeInMs);

    /**
     * Called when the reader has finished.
     *
     * @param processRunner
     *            the {@link ProcessRunner} object
     */
    void onComplete(ProcessRunner processRunner);
}
