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
package hu.bme.mit.sette.core.util.process;

/**
 * The listener interface for receiving {@link ProcessExecutor} events.
 *
 * @see ProcessExecutor
 */
public interface ProcessExecutorListener {
    /**
     * Called after the process has started.
     */
    default void onStart() {
        // do nothing by default
    }

    /**
     * Called when data was read from the standard output of the process from a separate thread (but
     * the method only recives one call at a time). Not called if the stdout is redirected to a
     * file.
     *
     * @param bytes
     *            The read bytes.
     */
    default void onStdoutRead(byte[] bytes) {
        // do nothing by default
    }

    /**
     * Called when data was read from the standard error output of the process from a separate
     * thread (but the method only recives one call at a time). Not called if the stderr is
     * redirected to a file.
     *
     * @param bytes
     *            The read bytes.
     */
    default void onStderrRead(byte[] bytes) {
        // do nothing by default
    }

    /**
     * Called when the process has finished.
     *
     * @param result
     *            The result of the execution (exit code, whether the process was destroyed and the
     *            elapsed time).
     */
    default void onComplete(ProcessExecutionResult result) {
        // do nothing by default
    }
}
