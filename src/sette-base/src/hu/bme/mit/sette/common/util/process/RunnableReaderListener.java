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
 * The listener interface for receiving {@link RunnableReader} events.
 *
 * @see RunnableReader
 */
interface RunnableReaderListener {
    /**
     * Called when characters were read by the reader.
     *
     * @param runnableReader
     *            the {@link RunnableReader} object
     * @param charactersRead
     *            the number of the read characters
     */
    default void onRead(RunnableReader runnableReader, int charactersRead) {
        // default: do nothing
    }

    /**
     * Called when {@link IOException} occurred.
     *
     * @param runnableReader
     *            the {@link RunnableReader} object
     * @param exception
     *            the exception
     */
    default void onIOException(RunnableReader runnableReader, IOException exception) {
        // default: do nothing
    }

    /**
     * Called when the reader has finished.
     *
     * @param runnableReader
     *            the {@link RunnableReader} object
     */
    default void onComplete(RunnableReader runnableReader) {
        // default: do nothing
    }
}
