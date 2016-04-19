package hu.bme.mit.sette.core.tasks;

import java.io.PrintStream;
import java.util.regex.Pattern;

import hu.bme.mit.sette.core.SetteException;
import hu.bme.mit.sette.core.exceptions.RunnerProjectRunnerException;

public interface RunnerProjectRunner extends EvaluationTask {
    int getTimeoutInMs();

    void setTimeoutInMs(int timeoutInMs);

    Pattern getSnippetSelector();

    void setSnippetSelector(Pattern snippetSelector);

    /**
     * Runs the runner project.
     *
     * @param loggerStream
     *            the logger stream
     * @throws RunnerProjectRunnerException
     *             if running fails
     */
    void run(PrintStream loggerStream) throws RunnerProjectRunnerException;

    /**
     * Cleans up the processes, i.e. kills undesired and stuck processes.
     *
     * @throws SetteException
     *             if a SETTE problem occurred
     */
    void cleanUp() throws SetteException;
}
