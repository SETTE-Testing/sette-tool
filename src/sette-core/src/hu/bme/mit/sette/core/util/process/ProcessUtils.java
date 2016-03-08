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
// NOTE revise this file
package hu.bme.mit.sette.core.util.process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.StandardSystemProperty;
import com.google.common.collect.Lists;

import lombok.NonNull;

/**
 * Utility class for process handling.
 */
public final class ProcessUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessUtils.class);

    private static final boolean IS_OS_WINDOWS;

    static {
        String os = StandardSystemProperty.OS_NAME.value();
        if (os == null) {
            LOG.warn("Cannot determine the OS because the {} property is null, "
                    + "assuming not Windows", StandardSystemProperty.OS_NAME);
            IS_OS_WINDOWS = false;
        } else {
            IS_OS_WINDOWS = os.toLowerCase().startsWith("win");
        }
    }

    /** Static class. */
    private ProcessUtils() {
        throw new UnsupportedOperationException("Static class");
    }

    /**
     * Terminates process given by its PID. It calls <code>kill -9 [PID]</code> so use carefully.
     * Please note that this method only calls the command and does not parses its output.
     *
     * @param pid
     *            the PID of the process
     * @throws InterruptedException
     *             if the current thread is interrupted by another thread while it is waiting
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static void terminateProcess(int pid) throws IOException {
        failIfWindows();

        List<String> command = Arrays.asList("kill", "-9", String.valueOf(pid));

        LOG.info("Terminating process {}: {}", pid, command);

        // example: kill -9 12345
        try {
            new ProcessBuilder(command).start().waitFor();
            LOG.info("Terminated process {}", pid);
        } catch (InterruptedException ex) {
            throw new IllegalStateException("The process execution was interrupted", ex);
        }
    }

    /**
     * Searches the running processes. It calls <code>ps aux</code> and performs partial string
     * search ({@link String#contains(CharSequence)} in the COMMAND column.
     *
     * @param searchExpression
     *            the search expression
     * @return the list of found process PIDs
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static List<Integer> searchProcesses(@NonNull String searchExpression)
            throws IOException {
        failIfWindows();
        Preconditions.checkArgument(!searchExpression.trim().isEmpty());

        /**
         * Command output example:
         * "USER        PID %CPU %MEM    VSZ   RSS TTY      STAT START   TIME COMMAND"
         * "sette     16070  0.0  0.0  22656  2660 pts/1    R+   00:23   0:00 ps aux"
         */
        final String command = "ps aux";
        final Pattern linePattern = Pattern
                .compile("\\S+\\s+(?<pid>\\S+)\\s+(\\S+\\s+){8}(?<command>.+)");
        LOG.debug("Search for process: {}", searchExpression);

        // execute command
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", command);
        ProcessExecutor executor = new ProcessExecutor(pb, 0);
        SimpleProcessExecutorListener listener = new SimpleProcessExecutorListener();

        ProcessExecutionResult result = executor.execute(listener);
        LOG.debug("Result: {}", result);

        // parse output
        String stdout = listener.getStdoutData().toString();
        String stderr = listener.getStderrData().toString();
        if (LOG.isTraceEnabled()) {
            LOG.debug("STDOUT\n" + stdout);
            LOG.debug("STDERR\n" + stderr);
        }

        // zero exit value is expected
        if (result.getExitValue() != 0) {
            throw new IOException("The command has exited with " + result.getExitValue());
        }

        // empty stderr is expected
        if (stderr.length() > 0) {
            throw new IOException("The command has produced unexpected error output");
        }

        // parse stdout lines
        List<Integer> pids = new ArrayList<>();
        Iterable<String> stdoutLinesIt = Splitter.on('\n').trimResults().omitEmptyStrings()
                .split(stdout);
        List<String> stdoutLines = Lists.newArrayList(stdoutLinesIt);

        // remove header line
        if (stdoutLines.size() > 0 && stdoutLines.get(0).startsWith("USER")) {
            stdoutLines.remove(0);
        }

        for (String line : stdoutLines) {
            Matcher m = linePattern.matcher(line);

            if (m.matches()) {
                if (m.group("command").contains(searchExpression)) {
                    String pid = m.group("pid");
                    LOG.debug("Found PID {} in line {}", pid, line);

                    try {
                        pids.add(Integer.parseInt(pid));
                    } catch (NumberFormatException ex) {
                        throw new IOException("The PID cannot be parsed as an integer: " + pid);
                    }
                }
            } else {
                throw new IOException("A line from stdout does not match the pattern: " + line);
            }
        }

        // sort and return
        pids.sort(null);
        LOG.debug("Found processes: {}", pids.toString());
        return pids;
    }

    /**
     * Searches the running processes and terminates the ones which were found. This method calls
     * {@link #terminateProcess(int)} for all the processes found by
     * {@link #searchProcesses(String)}.
     *
     * @param searchExpression
     *            the search expression
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static void searchAndTerminateProcesses(@NonNull String searchExpression)
            throws IOException {
        failIfWindows();

        List<Integer> pids = searchProcesses(searchExpression);
        LOG.info("Terminating processes: {}", pids);

        for (Integer pid : pids) {
            terminateProcess(pid);
        }

        LOG.info("Terminated processes: {}", pids);
    }

    /**
     * Throws an {@link UnsupportedOperationException} if the OS is Windows.
     */
    private static void failIfWindows() {
        if (IS_OS_WINDOWS) {
            throw new UnsupportedOperationException("Windows is not supported");
        }
    }
}
