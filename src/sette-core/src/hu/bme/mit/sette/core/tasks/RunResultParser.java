package hu.bme.mit.sette.core.tasks;

public interface RunResultParser extends EvaluationTask {
    void parse() throws Exception;
}
