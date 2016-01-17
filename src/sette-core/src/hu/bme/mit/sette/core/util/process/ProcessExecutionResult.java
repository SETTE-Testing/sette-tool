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
package hu.bme.mit.sette.core.util.process;

import lombok.Data;
import lombok.Getter;

/**
 * The instances of this class store the result of a process execution.
 */
@Data
public final class ProcessExecutionResult {
    /** The exit value/code of the execution. */
    @Getter
    private final int exitValue;

    /**
     * <code>true</code> if the process was destroyed because of timeout, otherwise
     * <code>false</code>
     */
    @Getter
    private final boolean destroyed;

    /** The elapsed time in milliseconds. */
    @Getter
    private final long elapsedTimeInMs;

    @Override
    public String toString() {
        return "ProcessExecutionResult [exitValue=" + exitValue + ", destroyed=" + destroyed
                + ", elapsedTimeInMs=" + elapsedTimeInMs + "]";
    }
}
