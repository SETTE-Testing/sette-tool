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
package hu.bme.mit.sette.core.util.process

import groovy.transform.CompileStatic
import hu.bme.mit.sette.core.util.io.PathUtils

import java.nio.file.Files
import java.nio.file.Path

import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout

/**
 * Integration tests for {@link ProcessExecutor}.
 */
@CompileStatic
class ProcessExecutorTest {
    // the tests must finish end even if the class is buggy and execute() never returns
    @Rule
    public Timeout globalTimeout = new Timeout(5000)

    @Test(expected = IllegalArgumentException)
    void testConstructor_invalidTimeout() {
        new ProcessExecutor(new ProcessBuilder(), -1)
    }

    @Test
    void testExecute_doesNotDestroyProcessIfNoTimeoutIsSet() {
        ProcessExecutor executor = createProcessExecutor(3, 5, 0)
        SimpleProcessExecutorListener listener = new SimpleProcessExecutorListener()
        ProcessExecutionResult result = executor.execute(listener)

        assert result.exitValue == 8
        assert !result.destroyed
        assert result.elapsedTimeInMs >= 500

        assert listener.stdoutData.tokenize('\n').size() == 3
        assert listener.stderrData.tokenize('\n').size() == 5
    }

    @Test
    void testExecute_doesNotDestroyProcessBeforeTimeout() {
        ProcessExecutor executor = createProcessExecutor(3, 5, 1000)
        SimpleProcessExecutorListener listener = new SimpleProcessExecutorListener()
        ProcessExecutionResult result = executor.execute(listener)

        assert result.exitValue == 8
        assert !result.destroyed
        assert result.elapsedTimeInMs >= 500

        assert listener.stdoutData.tokenize('\n').size() == 3
        assert listener.stderrData.tokenize('\n').size() == 5
    }

    @Test
    void testExecute_destroysProcessIfTimeoutIsReached() {
        ProcessExecutor executor = createProcessExecutor(50, 50, 200)
        SimpleProcessExecutorListener listener = new SimpleProcessExecutorListener()
        ProcessExecutionResult result = executor.execute(listener)

        assert result.destroyed
        assert result.elapsedTimeInMs >= 200
        // hopefully it will be enough
        assert result.elapsedTimeInMs <= 2500

        assert listener.stdoutData.tokenize('\n').size() in 1..3
        assert listener.stderrData.tokenize('\n').size() in 1..3
    }

    @Test
    void testExecute_handlesIORedirectionToFile() {
        Path outFile = Files.createTempFile('ProcessExecutorTest', '.tmp')
        Path errFile = Files.createTempFile('ProcessExecutorTest', '.tmp')

        Runtime.addShutdownHook {
            Files.deleteIfExists(outFile)
            Files.deleteIfExists(errFile)
        }

        ProcessExecutor executor = createProcessExecutor(1, 2, 0)
        executor.processBuilder.redirectOutput(outFile.toFile())
        executor.processBuilder.redirectError(errFile.toFile())
        SimpleProcessExecutorListener listener = new SimpleProcessExecutorListener()
        ProcessExecutionResult result = executor.execute(listener)

        assert result.exitValue == 3
        assert !result.destroyed
        assert result.elapsedTimeInMs >= 150

        assert listener.stdoutData.length() == 0
        assert listener.stderrData.length() == 0
        assert PathUtils.readAllLines(outFile).size() == 1
        assert PathUtils.readAllLines(errFile).size() == 2
    }

    @Test
    void testExecute_notifiesListenerInProperOrder() {
        List<String> events = []

        ProcessExecutor executor = createProcessExecutor(1, 2, 0)
        ProcessExecutorListener listener = new ProcessExecutorListener() {
                    @Override
                    void onStart() {
                        events << 'start'
                    }

                    @Override
                    void onStdoutRead(byte[] bytes) {
                        events << 'read'
                    }

                    @Override
                    void onStderrRead(byte[] bytes) {
                        events << 'read'
                    }

                    @Override
                    void onComplete(ProcessExecutionResult result) {
                        events << 'complete'
                    }
                }

        ProcessExecutionResult result = executor.execute(listener)

        assert result.exitValue == 3
        assert !result.destroyed
        assert result.elapsedTimeInMs >= 150

        assert events[0] == 'start'
        assert events[-1] == 'complete'
        assert events[1..-2] == ['read']* (events.size()-2)
    }

    private ProcessExecutor createProcessExecutor(int stdoutMax, int stdinMax, int timeoutInMs) {
        List<String> command = [
            'java',
            '-cp',
            // the same classpath will work
            System.getProperty('java.class.path'),
            ProcessTestApplication.name,
            String.valueOf(stdoutMax),
            String.valueOf(stdinMax)
        ]
        return new ProcessExecutor(new ProcessBuilder(command), timeoutInMs)
    }
}
