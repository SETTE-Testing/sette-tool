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

import java.io.PrintStream;

/**
 * Simple command-line application to test process-related classes. See {@link #printHelp()} for
 * more info.
 */
public final class ProcessTestApplication {
    private static final PrintStream out = System.out;
    private static final PrintStream err = System.err;

    /**
     * Entry point for the application.
     * 
     * @param args
     *            Arguments for the application.
     * @throws InterruptedException
     *             If any thread has interrupted the current thread.
     */
    public static void main(String[] args) throws InterruptedException {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            printHelp();
            return;
        }

        int stdoutMax, stderrMax;
        try {
            stdoutMax = Integer.parseInt(args[0]);
            stderrMax = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            printHelp();
            return;
        }

        for (int i = 0; i < Math.max(stdoutMax, stderrMax); i++) {
            if (i < stdoutMax) {
                out.println(i);
            }
            Thread.sleep(50);

            if (i < stderrMax) {
                err.println(i);
            }
            Thread.sleep(50);
        }

        System.exit(stdoutMax + stderrMax);
    }

    /**
     * Prints the help message to STDERR.
     */
    public static void printHelp() {
        err.println("Produces messages to stdout/sdterr starting from 0 with some delay");
        err.println("The exit code is <stdoutMax>+<stderrMax>");
        err.println();
        err.println("Usage: ");
        err.println("  java <main-class> <stdoutMax> <stderrMax>");
        err.println("  <main-class> " + ProcessTestApplication.class.getName());
        err.println("  <stdoutMax> number of messages to write to stdout (<0 means 0)");
        err.println("  <stderrMax> number of messages to write to stderr (<0 means 0)");
    }
}
