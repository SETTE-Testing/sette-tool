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

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.NonNull;

/**
 * This class is able to execute a {@link Process} while the caller can specify a timeout for the
 * process run. After the timeout is elapsed, the process will be killed forcibly.
 * <p>
 * Please note that this class does not provide any possibility to write to the standard input of
 * the process.
 * 
 */
public final class ProcessExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessUtils.class);

    /** The builder used to create the process. */
    private final ProcessBuilder processBuilder;

    /** The timeout in ms (zero means no timeout). */
    private final int timeoutInMs;

    /**
     * Creates a new {@link Process} using the specified {@link ProcessBuilder} and timeout.
     * 
     * @param processBuilder
     *            The builder to use to create the process.
     * @param timeoutInMs
     *            The timeout for the process in milliseconds. 0 means unlimited timeout. Must not
     *            be negative.
     */
    public ProcessExecutor(@NonNull ProcessBuilder processBuilder, int timeoutInMs) {
        checkArgument(timeoutInMs >= 0);

        this.processBuilder = processBuilder;
        this.timeoutInMs = timeoutInMs;

        LOG.debug("Created for {} with command {} with {}ms timeout", processBuilder,
                processBuilder.command(), timeoutInMs);
    }

    /**
     * Executes the process. This method blocks the caller thread until the process has finished or
     * destroyed.
     * 
     * @param listener
     *            A listener to notify on the following events: start, stdoutRead, stderrRead,
     *            complete.
     * @return The result of the execution (exit code, whether the process was destroyed and the
     *         elapsed time).
     * @throws IOException
     *             If an I/O error occurs.
     */
    public ProcessExecutionResult execute(@NonNull ProcessExecutorListener listener)
            throws IOException {
        try {
            LOG.debug("execute() called");
            LOG.debug("  command: {}", processBuilder.command());
            LOG.debug("  directory: {}", processBuilder.directory());
            LOG.debug("  environment: {}", processBuilder.environment());
            LOG.debug("  redirect: {} (input) {} (output) {} (error)",
                    processBuilder.redirectInput(), processBuilder.redirectOutput(),
                    processBuilder.redirectError());

            // start process
            long start = System.currentTimeMillis();
            Process process = processBuilder.start();

            LOG.debug("notifying listener start()");
            listener.onStart();
            LOG.debug("notified listener start()");

            // handler stdout and stderr
            InputStream stdout = processBuilder.redirectOutput() == Redirect.PIPE
                    ? process.getInputStream() : null;
            InputStream stderr = processBuilder.redirectError() == Redirect.PIPE
                    ? process.getErrorStream() : null;

            InputStreamGobbler stdoutGobbler = new InputStreamGobbler(stdout) {
                @Override
                protected void dataRead(byte[] buffer, int bytesRead) {
                    LOG.trace("Read {} bytes from stdout", bytesRead);
                    listener.onStdoutRead(Arrays.copyOf(buffer, bytesRead));
                }
            };
            InputStreamGobbler stderrGobbler = new InputStreamGobbler(stderr) {
                @Override
                protected void dataRead(byte[] buffer, int bytesRead) {
                    LOG.trace("Read {} bytes from stderr", bytesRead);
                    listener.onStderrRead(Arrays.copyOf(buffer, bytesRead));
                }
            };

            LOG.debug("Starting stream gobblers");
            stdoutGobbler.start();
            stderrGobbler.start();

            boolean finishedInTime;
            if (timeoutInMs > 0) {
                // wait until timeout
                finishedInTime = process.waitFor(timeoutInMs, TimeUnit.MILLISECONDS);
            } else {
                // no timeout
                process.waitFor();
                finishedInTime = true;
            }
            LOG.debug("Process finished in time: {}", finishedInTime);

            // terminate process if needed, stop threads
            int exitValue;
            if (finishedInTime) {
                // shutdown readers
                LOG.debug("Waiting for gobbler threads to finish");
                stdoutGobbler.join();
                stderrGobbler.join();

                exitValue = process.exitValue();
            } else {
                // timeout, try to stop threads
                LOG.debug("Interrupting gobbler threads");
                stdoutGobbler.interrupt();
                stderrGobbler.interrupt();

                // give a short time for the threads to stop before destroying the process
                Thread.sleep(200);

                // destroy the process
                LOG.debug("Destroying process forcibly");
                exitValue = process.destroyForcibly().waitFor();
            }

            // notify listener and return
            long elapsedTime = System.currentTimeMillis() - start;
            ProcessExecutionResult result = new ProcessExecutionResult(exitValue, !finishedInTime,
                    elapsedTime);
            listener.onComplete(result);

            LOG.debug("execute() result: {}", result);
            return result;
        } catch (InterruptedException ex) {
            throw new IllegalStateException("The process execution was interrupted", ex);
        }
    }

    /**
     * This thread reads data from the given {@link InputStream} on calls the
     * {@link #dataRead(byte[], int)} method (implemented by a subclass) when any data is read.
     * Please note that the thread does not close the stream.
     */
    private abstract static class InputStreamGobbler extends Thread {
        /** the input stream, if <code>null</code> the class will not read anything. */
        private final InputStream inputStream;

        /**
         * Instantiates a new {@link InputStreamGobbler}.
         *
         * @param inputStream
         *            the input stream, if <code>null</code> the class will not read anything
         */
        public InputStreamGobbler(InputStream inputStream) {
            this.inputStream = inputStream;
            LOG.trace("Gobbler thread created for {}", inputStream);
        }

        @Override
        public synchronized void start() {
            // do not even start a thread if the input stream is null
            if (inputStream != null) {
                LOG.trace("Start gobbler thread for {}", inputStream);
                super.start();
            } else {
                LOG.trace("Do not start thread for {}", inputStream);
            }
        }

        @Override
        public void run() {
            LOG.trace("Gobbler run() called for {}", inputStream);

            // needed because the run() method may be called directly
            if (inputStream == null) {
                LOG.trace("Do not run gobbler for {}", inputStream);
                return;
            }

            try {
                // read until the process is running or the stream is empty
                byte[] buffer = new byte[8192];

                while (!Thread.currentThread().isInterrupted()) {
                    int bytesRead = inputStream.read(buffer);

                    if (bytesRead > 0) {
                        dataRead(buffer, bytesRead);
                    } else if (bytesRead < 0) {
                        break;
                    }
                }

                LOG.trace("Stopping gobbler for {}", inputStream);
            } catch (IOException ex) {
                throw new RuntimeException("IOException from gobbler thread for " + inputStream,
                        ex);
            }
        }

        /**
         * Called when data is read from the input stream. The read bytes start at the beginning of
         * the buffer. The implementation should not alter the contents of the buffer.
         * 
         * @param buffer
         *            Reference to the buffer used for reading.
         * @param bytesRead
         *            The number of bytes read.
         */
        protected abstract void dataRead(byte[] buffer, int bytesRead);
    }
}
