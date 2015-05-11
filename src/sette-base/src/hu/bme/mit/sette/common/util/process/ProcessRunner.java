/*
 * SETTE - Symbolic Execution based Test Tool Evaluator
 *
 * SETTE is a tool to help the evaluation and comparison of symbolic execution
 * based test input generator tools.
 *
 * Budapest University of Technology and Economics (BME)
 *
 * Authors: Lajos Cseppentő <lajos.cseppento@inf.mit.bme.hu>, Zoltán Micskei
 * <micskeiz@mit.bme.hu>
 *
 * Copyright 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package hu.bme.mit.sette.common.util.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class is able to run an external {@link Process} while the caller can
 * specify a timeout for the process run. After the timeout is elapsed, the
 */
public final class ProcessRunner {
    /** The default poll interval for {@link ProcessRunner} objects. */
    public static final int DEFAULT_POLL_INTERVAL = 200;

    /**
     * The amount of time to wait after stopping the readers before destroying
     * the process.
     */
    private static final int WAIT_BEFORE_DESTROY = 100;

    /** The timeout in ms (zero means no timeout). */
    private int timeoutInMs = 0;

    /** The poll interval in ms. */
    private int pollIntervalInMs = DEFAULT_POLL_INTERVAL;

    /** The reader buffer size in bytes. */
    private int readerBufferSize = RunnableReader.DEFAULT_BUFFER_SIZE;

    /** The command. */
    private String[] command = null;

    /** The environment variables. */
    private String[] environmentVariables = null;

    /** The working directory. */
    private File workingDirectory = null;

    /** The exit value. */
    private Integer exitValue = null;

    /** Whether the process was destroyed. */
    private Boolean wasDestroyed = null;

    /** The data read from stdout. */
    private StringBuffer stdout = null;

    /** The data read from stderr. */
    private StringBuffer stderr = null;

    /** The listeners. */
    private final List<ProcessRunnerListener> listeners = new ArrayList<>();

    /**
     * Executes the process. This method blocks the caller thread until the
     * process has finished or destroyed.
     */
    public void execute() {
        // init
        exitValue = null;
        wasDestroyed = null;
        stdout = null;
        stderr = null;

        StringBuffer stdoutData = new StringBuffer();
        StringBuffer stderrData = new StringBuffer();

        // start process
        long start = System.currentTimeMillis();
        Process process;
        try {
            process = Runtime.getRuntime().exec(command,
                    environmentVariables, workingDirectory);
        } catch (IOException e) {
            for (ProcessRunnerListener listener : listeners) {
                listener.onIOException(this, e);
            }
            return;
        }

        // create readers and start threads
        // use buffered reader to buffer the streams
        RunnableReader stdoutReader = new RunnableReader(
                new BufferedReader(new InputStreamReader(
                        process.getInputStream())), stdoutData,
                readerBufferSize);
        stdoutReader.addListener(new StdoutReaderListener());

        RunnableReader stderrReader = new RunnableReader(
                new BufferedReader(new InputStreamReader(
                        process.getErrorStream())), stderrData,
                readerBufferSize);
        stderrReader.addListener(new StderrReaderListener());

        Thread stdoutReaderThread = new Thread(stdoutReader);
        Thread stderrReaderThread = new Thread(stderrReader);

        stdoutReaderThread.start();
        stderrReaderThread.start();

        // start waiting for result
        int lExitValue = -1;
        boolean isDestroyed = false;

        while (true) {
            long elapsed = System.currentTimeMillis() - start;

            // a poll interval has elapsed
            for (ProcessRunnerListener listener : listeners) {
                listener.onTick(this, elapsed);
            }

            try {
                // process finished (or throws exception)
                lExitValue = process.exitValue();
                isDestroyed = false;

                try {
                    stdoutReaderThread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                try {
                    stderrReaderThread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                break;
            } catch (IllegalThreadStateException e) {
                // process is still running
                if (timeoutInMs > 0 && elapsed > timeoutInMs) {
                    // timeout, try to stop readers
                    stdoutReader.initiateStop();
                    stderrReader.initiateStop();

                    try {
                        // giving a short time for the readers to stop before
                        // destroying the process
                        Thread.sleep(WAIT_BEFORE_DESTROY);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }

                    // destroy the process
                    process.destroy();
                    isDestroyed = true;

                    try {
                        lExitValue = process.waitFor();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }

                    break;
                } else {
                    // wait
                    try {
                        Thread.sleep(pollIntervalInMs);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        exitValue = lExitValue;
        wasDestroyed = isDestroyed;
        stdout = stdoutData;
        stderr = stderrData;

        for (ProcessRunnerListener listener : listeners) {
            listener.onComplete(this);
        }
    }

    /**
     * Gets the timeout in ms.
     *
     * @return the timeout in ms (zero means no timeout)
     */
    public int getTimeoutInMs() {
        return timeoutInMs;
    }

    /**
     * Sets the timeout in ms.
     *
     * @param pTimeoutInMs
     *            the new timeout in ms (zero means no timeout)
     */
    public void setTimeoutInMs(final int pTimeoutInMs) {
        timeoutInMs = pTimeoutInMs;
    }

    /**
     * Gets the poll interval in ms.
     *
     * @return the poll interval in ms
     */
    public int getPollIntervalInMs() {
        return pollIntervalInMs;
    }

    /**
     * Sets the poll interval in ms.
     *
     * @param pPollIntervalInMs
     *            the new poll interval in ms
     */
    public void setPollIntervalInMs(final int pPollIntervalInMs) {
        pollIntervalInMs = pPollIntervalInMs;
    }

    /**
     * Gets the reader buffer size in bytes.
     *
     * @return the reader buffer size in bytes
     */
    public int getReaderBufferSize() {
        return readerBufferSize;
    }

    /**
     * Sets the reader buffer size in bytes.
     *
     * @param pReaderBufferSize
     *            the new reader buffer size in bytes
     */
    public void setReaderBufferSize(final int pReaderBufferSize) {
        readerBufferSize = pReaderBufferSize;
    }

    /**
     * Gets the command.
     *
     * @return the command
     */
    public String[] getCommand() {
        return command;
    }

    /**
     * Sets the command.
     *
     * @param pCommand
     *            the new command
     */
    public void setCommand(final String[] pCommand) {
        command = pCommand;
    }

    /**
     * Sets the command.
     *
     * @param pCommand
     *            the new command
     */
    public void setCommand(final Collection<String> pCommand) {
        command = pCommand.toArray(new String[pCommand.size()]);
    }

    /**
     * Gets the environment variables.
     *
     * @return the environment variables
     */
    public String[] getEnvironmentVariables() {
        return environmentVariables;
    }

    /**
     * Sets the environment variables.
     *
     * @param pEnvironmentVariables
     *            the new environment variables
     */
    public void setEnvironmentVariables(
            final String[] pEnvironmentVariables) {
        environmentVariables = pEnvironmentVariables;
    }

    /**
     * Gets the working directory.
     *
     * @return the working directory
     */
    public File getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * Sets the working directory.
     *
     * @param pWorkingDirectory
     *            the new working directory
     */
    public void setWorkingDirectory(final File pWorkingDirectory) {
        workingDirectory = pWorkingDirectory;
    }

    /**
     * Gets the exit value.
     *
     * @return the exit value
     */
    public Integer getExitValue() {
        return exitValue;
    }

    /**
     * Returns whether the process was destroyed.
     *
     * @return whether the process was destroyed
     */
    public Boolean wasDestroyed() {
        return wasDestroyed;
    }

    /**
     * Gets the data read from stdout.
     *
     * @return the data read from stdout
     */
    public StringBuffer getStdout() {
        return stdout;
    }

    /**
     * Gets the data read from stderr.
     *
     * @return the data read from stderr
     */
    public StringBuffer getStderr() {
        return stderr;
    }

    /**
     * Adds a listener.
     *
     * @param listener
     *            the listener
     */
    public void addListener(final ProcessRunnerListener listener) {
        if (listener == null) {
            return;
        } else {
            listeners.add(listener);
        }
    }

    /**
     * Removes a listener.
     *
     * @param listener
     *            the listener
     */
    public void removeListener(final ProcessRunnerListener listener) {
        listeners.remove(listener);
    }

    /**
     * Base class for {@link StdoutReaderListener} and
     * {@link StderrReaderListener} classes.
     */
    private abstract class BaseRunnableReaderListener implements
            RunnableReaderListener {
        /*
         * (non-Javadoc)
         * 
         * @see hu.bme.mit.sette.common.util.process.RunnableReaderListener
         * #onIOException(hu.bme.mit.sette.common.util.process.RunnableReader,
         * java.io.IOException)
         */
        @Override
        public void onIOException(final RunnableReader readerThread,
                final IOException exception) {
            for (ProcessRunnerListener listener : listeners) {
                listener.onIOException(ProcessRunner.this, exception);
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see hu.bme.mit.sette.common.util.process.RunnableReaderListener
         * #onComplete(hu.bme.mit.sette.common.util.process.RunnableReader)
         */
        @Override
        public void onComplete(final RunnableReader readerThread) {
        }
    }

    /**
     * This class propagates stdout {@link RunnableReader} events to the
     * listeners of the {@link ProcessRunner}.
     */
    private final class StdoutReaderListener extends
            BaseRunnableReaderListener {
        /*
         * (non-Javadoc)
         * 
         * @see
         * hu.bme.mit.sette.common.util.process.RunnableReaderListener#onRead
         * (hu.bme.mit.sette.common.util.process.RunnableReader, int)
         */
        @Override
        public void onRead(final RunnableReader readerThread,
                final int charactersRead) {
            for (ProcessRunnerListener listener : listeners) {
                listener.onStdoutRead(ProcessRunner.this,
                        charactersRead);
            }
        }
    }

    /**
     * This class propagates stderr {@link RunnableReader} events to the
     * listeners of the {@link ProcessRunner}.
     */
    private final class StderrReaderListener extends
            BaseRunnableReaderListener {
        /*
         * (non-Javadoc)
         * 
         * @see
         * hu.bme.mit.sette.common.util.process.RunnableReaderListener#onRead
         * (hu.bme.mit.sette.common.util.process.RunnableReader, int)
         */
        @Override
        public void onRead(final RunnableReader readerThread,
                final int charactersRead) {
            for (ProcessRunnerListener listener : listeners) {
                listener.onStderrRead(ProcessRunner.this,
                        charactersRead);
            }
        }
    }
}
