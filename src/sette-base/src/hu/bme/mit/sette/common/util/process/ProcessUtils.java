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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

/**
 * Helper class for process handling.
 */
public final class ProcessUtils {
    /**
     * The minimal ASCII code in the search expression of the {@link #searchProcess(String)}
     * method..
     */
    private static final int SEARCH_EXPR_MIN = 0x20;
    /**
     * The maximal ASCII code in the search expression of the {@link #searchProcess(String)}
     * method..
     */
    private static final int SEARCH_EXPR_ASCII_MAX = 0x7E;
    /**
     * The disallowed characters in the search expression of the {@link #searchProcess(String)}
     * method.
     */
    private static final char[] SEARCH_EXPR_DISALLOWED_CHARS = new char[] { '\\', '"', '`' };
    /**
     * Pattern for the output received by the {@link #searchProcess(String)} method.
     */
    private static final Pattern SEARCH_PROCESS_PATTERN = Pattern
            .compile("^\\s*\\d+\\s+(\\d+)(.*)$");

    /** Static class. */
    private ProcessUtils() {
        throw new UnsupportedOperationException("Static class");
    }

    /**
     * Terminates process given by its PID. It calls <code>kill -9 [PID]</code> so use carefully.
     *
     * @param pid
     *            the PID of the process
     * @throws InterruptedException
     *             if the current thread is interrupted by another thread while it is waiting
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static void terminateProcess(int pid) throws InterruptedException, IOException {
        // example: kill -9 12345
        String command = String.format("kill -9 %d", pid);
        Runtime.getRuntime().exec(command).waitFor();
    }

    /**
     * Searches the running processes. It calls <code>ps asx | grep "[searchExpression]"</code>
     *
     * @param searchExpression
     *            the search expression
     * @return the list
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static List<Integer> searchProcess(String searchExpression) throws IOException {
        // validate search expression
        Validate.notBlank(searchExpression, "The search expression must not be blank");

        for (char ch : ProcessUtils.SEARCH_EXPR_DISALLOWED_CHARS) {
            Validate.isTrue(searchExpression.indexOf(ch) < 0,
                    "The search expression must not contain "
                            + "the '%c' character (searchExpression: [%s])",
                    ch, searchExpression);
        }

        for (int i = 0; i < searchExpression.length(); i++) {
            char ch = searchExpression.charAt(i);
            Validate.isTrue(SEARCH_EXPR_MIN <= ch && ch <= SEARCH_EXPR_ASCII_MAX,
                    "The ASCII codes of the characters of the search expression "
                            + "must be between 0x%02X and 0x%02X (searchExpression: [%s])",
                    SEARCH_EXPR_MIN, SEARCH_EXPR_ASCII_MAX, searchExpression);
        }

        // open process

        // command: /bin/bash -c "ps asx | grep \"MyProc\""
        // example: 1000 12345 ... where 12345 is the PID
        String command = String.format("ps asx | grep \"%s\"", searchExpression);

        Process p = Runtime.getRuntime().exec(new String[] { "/bin/bash", "-c", command });

        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));

        List<Integer> pids = new ArrayList<>();

        // read pids
        while (true) {
            String line = br.readLine();

            if (br.readLine() == null) {
                break;
            }

            Matcher m = ProcessUtils.SEARCH_PROCESS_PATTERN.matcher(line);

            if (m.matches() && !m.group(2).contains(command)) {
                pids.add(Integer.parseInt(m.group(1)));
            }
        }

        return pids;
    }
}
