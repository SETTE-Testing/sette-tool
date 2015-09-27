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

import lombok.Getter;

/**
 * This listener class collects the output from stdout and stderr into {@link StringBuffer} objects.
 */
public class SimpleProcessExecutorListener implements ProcessExecutorListener {
    /** The data read from stdout. */
    @Getter
    private final StringBuilder stdoutData = new StringBuilder();

    /** The data read from stderr. */
    @Getter
    private final StringBuilder stderrData = new StringBuilder();

    @Override
    public void onStdoutRead(byte[] bytes) {
        stdoutData.append(new String(bytes));
    }

    @Override
    public void onStderrRead(byte[] bytes) {
        stderrData.append(new String(bytes));
    }
}