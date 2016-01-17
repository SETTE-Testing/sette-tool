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

  import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;

import hu.bme.mit.sette.core.util.process.ProcessExecutionResult;
import hu.bme.mit.sette.core.util.process.ProcessExecutor;
import hu.bme.mit.sette.core.util.process.SimpleProcessExecutorListener;

public class AntExecutor {
    public static void executeAnt(File dir, String buildFile) {
        List<String> command = new ArrayList<>();

        if (SystemUtils.IS_OS_WINDOWS) {
            command.add("cmd.exe");
            command.add("/c");
        } else {
            command.add("/bin/bash");
            command.add("-c");
        }

        if (buildFile != null) {
            command.add("ant -buildfile " + buildFile);
        } else {
            command.add("ant");
        }

        ProcessBuilder pb = new ProcessBuilder(command).directory(dir);
        ProcessExecutor pr = new ProcessExecutor(pb, 0);
        SimpleProcessExecutorListener listener = new SimpleProcessExecutorListener();
        ProcessExecutionResult result;
        try {
            result = pr.execute(listener);
        } catch (Exception ex) {
            // FIXME
            throw new RuntimeException(ex);
        }

        System.out.println("Ant build result: " + result);

        if (listener.getStdoutData().length() > 0) {
            System.out.println("Ant build output:");
            System.out.println("========================================");
            System.out.println(listener.getStdoutData().toString());
            System.out.println("========================================");
        }

        if (listener.getStderrData().length() > 0) {
            System.out.println("Ant build error output:");
            System.out.println("========================================");
            System.out.println(listener.getStderrData().toString());
            System.out.println("========================================");
            System.out.println("Terminating");
        }

        if (listener.getStderrData().length() > 0) {
            // TODO enchance error handling
            throw new RuntimeException("ant build has failed");
        }
    }
}
