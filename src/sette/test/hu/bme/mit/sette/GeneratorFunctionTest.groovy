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
package hu.bme.mit.sette

import groovy.transform.TypeChecked
import hu.bme.mit.sette.application.SetteApplication

import java.nio.file.Paths
import java.util.concurrent.TimeUnit

import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.rules.Timeout
import org.junit.runner.Description

@TypeChecked
class GeneratorFunctionTest {
    @Rule
    public Timeout globalTimeout = Timeout.builder().withLookingForStuckThread(true).withTimeout(30, TimeUnit.SECONDS).build()

    @Rule
    public TestRule watchman = new TestWatcher() {
        @Override
        protected void failed(Throwable ex, Description description) {
            println ' TEST FAILURE '.center(80, '=')
            println "  ${description.className}.${description.methodName}()"

            println ' Exception stacktrace '.center(80, '-')
            ex.printStackTrace(System.out)

            println ' INPUT (remaining) '.center(80, '-')
            input.data.each { println it }

            println ' OUTPUT '.center(80, '-')
            output.lines.each  { println it }

            println ' ERROR OUTPUT '.center(80, '-')
            errorOutput.lines.each  { println it }

            println '='.multiply(80)
        }
    }

    TestBufferedReader input
    TestPrintStream output, errorOutput
    SetteApplication app

    @Before
    void setUp() {
        input = new TestBufferedReader()
        output = new TestPrintStream()
        errorOutput = new TestPrintStream()
        app = new SetteApplication(input, output, errorOutput, Paths.get('sette.config.json'))
    }

    @Test
    @Ignore // FIXME
    public final void testAA() {
        app.execute('--snippet-project-dir', 'sette-snippets/java/sette-snippets', '--backup', 'skip',
                '--runner-project-tag', 'test-auto', '--runner-timeout', '5s', '--task', 'generator', '--tool', 'catg')
        assert !errorOutput.lines.isEmpty()
    }
}
