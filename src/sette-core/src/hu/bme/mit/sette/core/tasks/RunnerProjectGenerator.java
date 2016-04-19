package hu.bme.mit.sette.core.tasks;

import hu.bme.mit.sette.core.exceptions.RunnerProjectGeneratorException;

public interface RunnerProjectGenerator extends EvaluationTask {
    /**
     * Generates the runner project.
     *
     * @throws RunnerProjectGeneratorException
     *             if generation fails
     */
    void generate() throws RunnerProjectGeneratorException;

}
