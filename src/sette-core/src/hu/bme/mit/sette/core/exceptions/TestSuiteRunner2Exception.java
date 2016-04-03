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
import hu.bme.mit.sette.core.tasks.testsuiterunner2.TestSuiteRunner2;

/**
 * Exception class for exceptions in connection with a test suite runner.
 */
public final class TestSuiteRunner2Exception extends SetteException {
    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -3524104888740157450L;

    /** The test suite runner. */
    private final TestSuiteRunner2 testSuiteRunner2;

    /**
     * Instantiates a new test suite runner exception.
     *
     * @param message
     *            the message
     * @param runner
     *            the test suite runner
     */
    public TestSuiteRunner2Exception(String message, TestSuiteRunner2 runner) {
        this(message, runner, null);
    }

    /**
     * Instantiates a new test suite runner exception.
     *
     * @param message
     *            the message
     * @param runner
     *            the test suite runner
     * @param cause
     *            the cause
     */
    public TestSuiteRunner2Exception(String message, TestSuiteRunner2 runner, Throwable cause) {
        super(message, cause);
        testSuiteRunner2 = runner;
    }

    /**
     * Gets the test suite runner.
     *
     * @return the test suite runner
     */
    public TestSuiteRunner2 getTestSuiteRunner2() {
        return testSuiteRunner2;
    }
}
