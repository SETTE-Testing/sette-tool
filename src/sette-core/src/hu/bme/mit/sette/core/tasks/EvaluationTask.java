package hu.bme.mit.sette.core.tasks;

import hu.bme.mit.sette.core.model.runner.RunnerProjectSettings;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;

public interface EvaluationTask {
    SnippetProject getSnippetProject();

    RunnerProjectSettings getRunnerProjectSettings();
}
