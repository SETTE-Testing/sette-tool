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
package hu.bme.mit.sette.core.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import hu.bme.mit.sette.core.SetteException;
import hu.bme.mit.sette.core.configuration.SetteConfigurationException;
import hu.bme.mit.sette.core.exceptions.RunnerProjectRunnerException;
import hu.bme.mit.sette.core.model.runner.RunnerProjectUtils;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetContainer;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.random.SplitterOutputStream;
import hu.bme.mit.sette.core.tool.Tool;
import hu.bme.mit.sette.core.util.io.PathUtils;
import hu.bme.mit.sette.core.util.process.ProcessExecutionResult;
import hu.bme.mit.sette.core.util.process.ProcessExecutor;
import hu.bme.mit.sette.core.util.process.ProcessExecutorListener;
import lombok.Getter;
import lombok.Setter;

/**
 * A SETTE task which provides base for runner project running. The phases are the following:
 * validation, preparation, running.
 *
 * @param <T>
 *            the type of the tool
 */
public abstract class RunnerProjectRunner<T extends Tool> extends EvaluationTask<T> {
    /** The poll interval for {@link ProcessExecutor} objects. */
    public static final int POLL_INTERVAL = 100;

    /** The default timeout for called processes. */
    private static final int DEFAULT_TIMEOUT = 30000;

    /** The timeout in ms for the called processes. */
    private int timeoutInMs;

    @Getter
    @Setter
    private Pattern snippetSelector = null;

    /**
     * Instantiates a new runner project runner.
     *
     * @param snippetProject
     *            the snippet project
     * @param outputDir
     *            the output directory
     * @param tool
     *            the tool
     */
    public RunnerProjectRunner(SnippetProject snippetProject, Path outputDir, T tool,
            String runnerProjectTag) {
        super(snippetProject, outputDir, tool, runnerProjectTag);
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
     *             if running fails
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
                        new SplitterOutputStream(new FileOutputStream(runnerLogFile), loggerStream),
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
                    "The runner project run has failed%n(phase: [%s])%n(tool: [%s])", phase,
                    getTool().getName());
            throw new RunnerProjectRunnerException(message, ex);
        } finally {
            // TODO try to eliminate with a try-resources block
            if (runnerLogger != null) {
                runnerLogger.close();
            }
        }
    }

    /**
     * Validates both the snippet and runner project settings.
     *
     * @throws SetteConfigurationException
     *             if a SETTE configuration problem occurred
     */
    private void validate() throws SetteConfigurationException {
        // TODO currently snippet project validation can fail even if it is
        // valid getSnippetProjectSettings().validateExists();
        getRunnerProjectSettings().validateExists();
    }

    /**
     * Prepares the running of the runner project, i.e. make everything ready for the execution.
     */
    private void prepare() {
        // delete previous outputs
        if (getRunnerProjectSettings().getRunnerOutputDirectory().exists()) {
            Path dir = getRunnerProjectSettings().getRunnerOutputDirectory().toPath();
            PathUtils.delete(dir);
        }

        // create output directory
        PathUtils.createDir(getRunnerProjectSettings().getRunnerOutputDirectory().toPath());
    }

    @FunctionalInterface
    public static interface CheckedFunction<T> {
        void apply(T t);
    }

    /**
     * Runs the tool on all the snippets.
     *
     * @param runnerLoggerOut
     *            the {@link PrintStream} of the logger
     */
    private void runAll(PrintStream runnerLoggerOut) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // foreach containers
        for (SnippetContainer container : getSnippetProject().getSnippetContainers()) {
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
                // FIXME duplicated in TestSuiteRunner -> replace loop with proper iterator
                if (snippetSelector != null
                        && !snippetSelector.matcher(snippet.getId()).matches()) {
                    String msg = String.format("Skipping %s (--snippet-selector)", snippet.getId());
                    runnerLoggerOut.println(msg);
                    log.info(msg);
                    continue;
                }

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
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    /**
     * This method is called after validation but before preparation.
     *
     * @throws SetteConfigurationException
     *             if a SETTE configuration problem occurred
     */
    protected void afterValidate() throws SetteConfigurationException {
        // to be implemented by the subclass
    }

    /**
     * This method is called after preparation but before writing.
     */
    protected void afterPrepare() {
        // to be implemented by the subclass
    }

    /**
     * This method is called after running.
     *
     * @throws SetteException
     *             if a SETTE problem occurred
     */
    protected void afterRunAll() throws SetteException {
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
     * @throws SetteException
     *             if a SETTE problem occurred
     */
    protected abstract void runOne(Snippet snippet, File infoFile, File outputFile, File errorFile)
            throws SetteException;

    /**
     * Cleans up the processes, i.e. kills undesired and stuck processes.
     *
     * @throws SetteException
     *             if a SETTE problem occurred
     */
    public abstract void cleanUp() throws SetteException;

    protected static final String getFilenameBase(Snippet snippet) {
        return snippet.getContainer().getJavaClass().getName().replace('.', '/') + "_"
                + snippet.getMethod().getName();
    }

    protected final void executeToolProcess(List<String> command, File infoFile, File outputFile,
            File errorFile) {
        infoFile.getParentFile().mkdirs();

        File workingDirectory = getRunnerProjectSettings().getBaseDir();

        ProcessBuilder pb = new ProcessBuilder(command).directory(workingDirectory);
        pb.redirectOutput(outputFile);
        pb.redirectError(errorFile);

        try {
            ProcessExecutor pe = new ProcessExecutor(pb,
                    shouldKillAfterTimeout() ? getTimeoutInMs() : 0);
            pe.execute(new ProcessExecutorListener() {
                @Override
                public void onComplete(ProcessExecutionResult result) {
                    // save info
                    StringBuffer infoData = new StringBuffer();

                    infoData.append("Command: ").append(command).append('\n');
                    infoData.append("Exit value: ").append(result.getExitValue()).append('\n');

                    infoData.append("Destroyed: ");
                    if (result.isDestroyed()) {
                        infoData.append("yes");
                    } else {
                        infoData.append("no");
                    }
                    infoData.append('\n');

                    infoData.append("Elapsed time: ").append(result.getElapsedTimeInMs())
                            .append(" ms\n");

                    PathUtils.write(infoFile.toPath(), infoData.toString().getBytes());
                }
            });
        } catch (Exception ex) {
            // FIXME TODO fixme
            throw new RuntimeException(ex);
        }
    }

    public abstract boolean shouldKillAfterTimeout();
}
