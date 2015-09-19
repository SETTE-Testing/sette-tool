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
package hu.bme.mit.sette.common.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.lang3.Validate;

import hu.bme.mit.sette.common.Tool;
import hu.bme.mit.sette.common.exceptions.ConfigurationException;
import hu.bme.mit.sette.common.exceptions.RunnerProjectRunnerException;
import hu.bme.mit.sette.common.exceptions.SetteException;
import hu.bme.mit.sette.common.model.runner.RunnerProjectUtils;
import hu.bme.mit.sette.common.model.snippet.Snippet;
import hu.bme.mit.sette.common.model.snippet.SnippetContainer;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;
import hu.bme.mit.sette.common.util.JavaFileUtils;
import hu.bme.mit.sette.common.util.process.ProcessRunner;
import hu.bme.mit.sette.common.util.process.ProcessRunnerListener;

/**
 * A SETTE task which provides base for runner project running. The phases are the following:
 * validation, preparation, running.
 *
 * @param <T>
 *            the type of the tool
 */
public abstract class RunnerProjectRunner<T extends Tool> extends SetteTask<T> {
    /** The poll interval for {@link ProcessRunner} objects. */
    public static final int POLL_INTERVAL = 100;

    /** The default timeout for called processes. */
    private static final int DEFAULT_TIMEOUT = 30000;

    /** The timeout in ms for the called processes. */
    private int timeoutInMs;

    /**
     * Instantiates a new runner project runner.
     *
     * @param snippetProject
     *            the snippet project
     * @param outputDirectory
     *            the output directory
     * @param tool
     *            the tool
     */
    public RunnerProjectRunner(SnippetProject snippetProject, File outputDirectory, T tool,
            String runnerProjectTag) {
        super(snippetProject, outputDirectory, tool, runnerProjectTag);
        this.timeoutInMs = RunnerProjectRunner.DEFAULT_TIMEOUT;
    }

    /**
     * Gets the timeout for the called processes.
     *
     * @return the timeout for the called processes
     */
    public final int getTimeoutInMs() {
        return this.timeoutInMs;
    }

    /**
     * Sets the timeout for the called processes.
     *
     * @param timeoutInMs
     *            the new timeout for the called processes
     */
    public final void setTimeoutInMs(int timeoutInMs) {
        this.timeoutInMs = timeoutInMs;
    }

    /**
     * Runs the runner project.
     *
     * @param loggerStream
     *            the logger stream
     * @throws RunnerProjectRunnerException
     *             if running has failed
     */
    public final void run(PrintStream loggerStream) throws RunnerProjectRunnerException {
        String phase = null;
        PrintStream runnerLogger = null;

        try {
            log.info("== Cleaning up");
            cleanUp();

            // validate preconditions
            phase = "validate (do)";
            log.info("== Phase: {}", phase);
            validate();

            phase = "validate (after)";
            log.info("== Phase: {}", phase);
            afterValidate();

            // prepare
            phase = "prepare (do)";
            log.info("== Phase: {}", phase);
            prepare();

            phase = "prepare (after)";
            log.info("== Phase: {}", phase);
            afterPrepare();

            // create logger
            File runnerLogFile = RunnerProjectUtils.getRunnerLogFile(getRunnerProjectSettings());

            if (loggerStream != null) {
                loggerStream.println("Log file: " + runnerLogFile.getCanonicalPath());
                runnerLogger = new PrintStream(
                        new TeeOutputStream(new FileOutputStream(runnerLogFile), loggerStream),
                        true);
            } else {
                runnerLogger = new PrintStream(new FileOutputStream(runnerLogFile), true);
            }

            // run all
            phase = "run all (do)";
            log.info("== Phase: {}", phase);
            runAll(runnerLogger);

            phase = "run all (after)";
            log.info("== Phase: {}", phase);
            afterRunAll();

            log.info("== Cleaning up");
            cleanUp();

            phase = "complete";
            log.info("== Phase: {}", phase);
        } catch (Exception ex) {
            String message = String.format(
                    "The runner project run has failed\n(phase: [%s])\n(tool: [%s])", phase,
                    getTool().getFullName());
            throw new RunnerProjectRunnerException(message, this, ex);
        } finally {
            IOUtils.closeQuietly(runnerLogger);
        }
    }

    /**
     * Validates both the snippet and runner project settings.
     *
     * @throws ConfigurationException
     *             if a SETTE configuration problem occurred
     */
    private void validate() throws ConfigurationException {
        Validate.isTrue(getSnippetProject().getState().equals(SnippetProject.State.PARSED),
                "The snippet project must be parsed (state: [%s]) ",
                getSnippetProject().getState().name());

        // TODO currently snippet project validation can fail even if it is
        // valid getSnippetProjectSettings().validateExists();
        getRunnerProjectSettings().validateExists();
    }

    /**
     * Prepares the running of the runner project, i.e. make everything ready for the execution.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private void prepare() throws IOException {
        // delete previous outputs
        if (getRunnerProjectSettings().getRunnerOutputDirectory().exists()) {
            FileUtils.forceDelete(getRunnerProjectSettings().getRunnerOutputDirectory());
        }

        // create output directory
        FileUtils.forceMkdir(getRunnerProjectSettings().getRunnerOutputDirectory());
    }

    /**
     * Runs the tool on all the snippets.
     *
     * @param runnerLoggerOut
     *            the {@link PrintStream} of the logger
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private void runAll(PrintStream runnerLoggerOut) throws IOException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // foreach containers
        for (SnippetContainer container : getSnippetProject().getModel().getContainers()) {
            // skip container with higher java version than supported
            if (container.getRequiredJavaVersion()
                    .compareTo(getTool().getSupportedJavaVersion()) > 0) {
                // TODO error/warning handling
                runnerLoggerOut.println("Skipping container: " + container.getJavaClass().getName()
                        + " (required Java version: " + container.getRequiredJavaVersion() + ")");
                System.err.println("Skipping container: " + container.getJavaClass().getName()
                        + " (required Java version: " + container.getRequiredJavaVersion() + ")");
                continue;
            }

            // foreach snippets
            for (Snippet snippet : container.getSnippets().values()) {
                String filenameBase = getFilenameBase(snippet);

                File infoFile = RunnerProjectUtils.getSnippetInfoFile(getRunnerProjectSettings(),
                        snippet);
                File outputFile = RunnerProjectUtils
                        .getSnippetOutputFile(getRunnerProjectSettings(), snippet);
                File errorFile = RunnerProjectUtils.getSnippetErrorFile(getRunnerProjectSettings(),
                        snippet);

                try {
                    String timestamp = dateFormat.format(new Date());
                    runnerLoggerOut
                            .println("[" + timestamp + "] Running for snippet: " + filenameBase);
                    this.runOne(snippet, infoFile, outputFile, errorFile);
                    this.cleanUp();
                } catch (Exception ex) {
                    runnerLoggerOut.println("Exception: " + ex.getMessage());
                    runnerLoggerOut.println("==========");
                    ex.printStackTrace(runnerLoggerOut);
                    runnerLoggerOut.println("==========");
                }
            }
        }
    }

    /**
     * This method is called after validation but before preparation.
     *
     * @throws ConfigurationException
     *             if a SETTE configuration problem occurred
     */
    protected void afterValidate() throws ConfigurationException {
        // to be implemented by the subclass
    }

    /**
     * This method is called after preparation but before writing.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    protected void afterPrepare() throws IOException {
        // to be implemented by the subclass
    }

    /**
     * This method is called after running.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws SetteException
     *             if a SETTE problem occurred
     */
    protected void afterRunAll() throws IOException, SetteException {
        // to be implemented by the subclass
    }

    /**
     * Runs the tool on one snippet.
     *
     * @param snippet
     *            the snippet
     * @param infoFile
     *            the info file for the snippet
     * @param outputFile
     *            the output file for the snippet
     * @param errorFile
     *            the error file for the snippet
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws SetteException
     *             if a SETTE problem occurred
     */
    protected abstract void runOne(Snippet snippet, File infoFile, File outputFile, File errorFile)
            throws IOException, SetteException;

    /**
     * Cleans up the processes, i.e. kills undesired and stuck processes.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws SetteException
     *             if a SETTE problem occurred
     */
    public abstract void cleanUp() throws IOException, SetteException;

    protected static final String getFilenameBase(Snippet snippet) {
        return JavaFileUtils.packageNameToFilename(snippet.getContainer().getJavaClass().getName())
                + "_" + snippet.getMethod().getName();
    }

    protected static final class OutputWriter implements ProcessRunnerListener {
        private long startTime = -1;
        private long finishTime = -1;
        private final String command;
        private final File infoFile;
        private final File outputFile;
        private final File errorFile;

        public OutputWriter(String command, File infoFile, File putputFile, File errorFile) {
            this.command = command;
            this.infoFile = infoFile;
            this.outputFile = putputFile;
            this.errorFile = errorFile;
        }

        public long getElapsedTimeInMs() {
            if (this.startTime > 0 && this.finishTime > 0) {
                return this.finishTime - this.startTime;
            } else {
                return -1;
            }
        }

        @Override
        public void onTick(ProcessRunner processRunner, long elapsedTimeInMs) {
            if (this.startTime < 0) {
                this.startTime = System.currentTimeMillis();
            }
        }

        @Override
        public void onIOException(ProcessRunner processRunner, IOException ex) {
            System.err.println("  IOException occured");
            System.err.println(ex);
        }

        @Override
        public void onComplete(ProcessRunner processRunner) {
            finishTime = System.currentTimeMillis();

            // save info
            StringBuffer infoData = new StringBuffer();

            infoData.append("Command: ").append(command).append('\n');
            infoData.append("Exit value: ").append(processRunner.getExitValue()).append('\n');

            infoData.append("Destroyed: ");
            if (processRunner.wasDestroyed()) {
                infoData.append("yes");
            } else {
                infoData.append("no");
            }
            infoData.append('\n');

            infoData.append("Elapsed time: ").append(getElapsedTimeInMs()).append(" ms\n");
            saveToFile(infoFile, infoData);

            // save stdout
            saveToFile(outputFile, processRunner.getStdout());

            // save stderr
            saveToFile(errorFile, processRunner.getStderr());
        }

        private static void saveToFile(File file, StringBuffer data) {
            if (data.length() <= 0) {
                return;
            }

            // TODO handle errors and exceptions
            file.getParentFile().mkdirs();

            FileWriter fw = null;
            try {
                fw = new FileWriter(file);

                // TODO enhance comment
                // don't use toString() because if data is big, you will be out
                // of memory!
                // this is bad: pw.print(data.toString())
                // https://forums.oracle.com/message/9015943 -> doit5 (this is
                // the best way regarding both speed and memory)

                // TODO externize buffer size
                // TODO other tasks here
                int bufferSize = 8192;
                for (int start = 0; start < data.length(); start += bufferSize) {
                    int end = start + bufferSize;

                    if (end > data.length()) {
                        fw.write(data.substring(start)); // to end of stream
                    } else {
                        fw.write(data.substring(start, end));
                    }
                }

                fw.flush();
            } catch (Exception ex) {
                // TODO syserr is kinda antipattern
                System.err.println("  Exception when saving to file (" + file + ")");
                ex.printStackTrace();
            } finally {
                IOUtils.closeQuietly(fw);
            }
        }
    }
}
