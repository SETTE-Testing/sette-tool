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
package hu.bme.mit.sette.application;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.joining;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

import hu.bme.mit.sette.core.configuration.SetteConfiguration;
import hu.bme.mit.sette.core.configuration.SetteToolConfiguration;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tasks.CsvBatchGenerator;
import hu.bme.mit.sette.core.tasks.CsvGenerator;
import hu.bme.mit.sette.core.tasks.TestSuiteGenerator;
import hu.bme.mit.sette.core.tasks.TestSuiteRunner;
import hu.bme.mit.sette.core.tool.Tool;
import hu.bme.mit.sette.runnerprojectbrowser.RunnerProjectBrowser;
import hu.bme.mit.sette.snippetbrowser.SnippetBrowser;
import javafx.application.Application;

public final class SetteApplication {
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private final BufferedReader input;
    private final PrintStream output;
    private final PrintStream errorOutput;
    private final Path configurationFile;

    public SetteApplication(BufferedReader input, PrintStream output, PrintStream errorOutput,
            Path configurationFile) {
        this.input = input;
        this.output = output;
        this.errorOutput = errorOutput;
        this.configurationFile = configurationFile;
    }

    public void execute(String... args) {
        try {
            //
            // Parse configuration
            //
            SetteConfiguration configuration = SetteConfiguration.parse(configurationFile);

            //
            // Determine what to do in this execution
            //

            // Parse arguments
            ArgumentParser argParser = new ArgumentParser(configuration, errorOutput);
            if (!argParser.parse(args)) {
                LOG.debug("Exiting (invalid cmd-line arguments or --help was specified");
                return;
            }

            Path snippetProjectDir;
            if (argParser.getSnippetProjectDir() != null) {
                snippetProjectDir = configuration.getBaseDir()
                        .resolve(argParser.getSnippetProjectDir());
            } else {
                snippetProjectDir = null;
            }
            ApplicationTask applicationTask = argParser.getApplicationTask();
            SetteToolConfiguration toolConfiguration = argParser.getToolConfiguration();
            String runnerProjectTag = argParser.getRunnerProjectTag();

            int runnerTimeoutInMs = argParser.getRunnerTimeoutInMs();
            BackupPolicy backupPolicy = argParser.getBackupPolicy();

            // Determine the snippet project if needed
            if ((applicationTask == null || applicationTask.requiresSnippetProject())
                    && snippetProjectDir == null) {
                snippetProjectDir = selectSnippetProjectDir(configuration.getSnippetProjectDirs());
            }

            // Determine the task if needed (and exit if specified)
            if (applicationTask == null) {
                applicationTask = selectApplicationTask();
            }

            if (applicationTask == ApplicationTask.EXIT) {
                LOG.debug("Exiting for user request");
                return;
            }

            // Determine the tool if needed
            Tool tool = null;
            if (applicationTask.requiresTool()) {
                if (toolConfiguration == null) {
                    toolConfiguration = selectToolConfiguration(
                            configuration.getToolConfigurations());
                }
                tool = Tool.create(toolConfiguration);
            } else {
                if (toolConfiguration != null) {
                    String msg = String.format("The tool is specified (%s) but for the %s task "
                            + "it is not required, thus it will not affect the current execution",
                            toolConfiguration.getName(), applicationTask);
                    output.println(msg);
                    LOG.debug(msg);
                    toolConfiguration = null;
                }
            }

            // Determine the runner project tag if needed
            if (applicationTask.requiresRunnerProjectTag()) {
                if (runnerProjectTag == null) {
                    output.print("Enter a runner project tag: ");
                    runnerProjectTag = readLineOrExitIfEOF().trim();
                }
            } else {
                if (runnerProjectTag != null) {
                    String msg = String.format("The runner project tag is specified (%s) but for "
                            + "the %s task it is not required, thus it will not affect the "
                            + "current execution", runnerProjectTag, applicationTask);
                    output.println(msg);
                    LOG.debug(msg);
                    runnerProjectTag = null;
                }
            }

            // Print settings
            output.println("Snippet project: " + snippetProjectDir);
            output.println("Task: " + applicationTask);
            output.println("Tool: " + tool);
            output.println("Runner project tag: " + runnerProjectTag);
            output.println("Snippet selector: " + argParser.getSnippetSelector());
            output.println(String.format("Runner timeout: %d ms", runnerTimeoutInMs));
            output.println("Backup policy: " + backupPolicy);

            //
            // Execute the specified task
            //
            final SnippetProject snippetProject;
            final ExecutionContext context;
            if (applicationTask.requiresSnippetProject()) {
                snippetProject = SnippetProject.parse(snippetProjectDir);
                context = new ExecutionContext(input, output, errorOutput,
                        snippetProject, tool, runnerProjectTag, runnerTimeoutInMs,
                        argParser.getSnippetSelector(), backupPolicy, configuration.getOutputDir());
            } else {
                snippetProject = null;
                context = null;
            }

            switch (applicationTask) {
                case EXIT:
                    return;

                case GENERATOR:
                    new GeneratorUI().execute(context);
                    break;

                case RUNNER:
                    new RunnerUI().execute(context);
                    break;

                case PARSER:
                    new ParserUI().execute(context);
                    break;

                case TEST_GENERATOR:
                    // NOTE now the generator skips the test suite generation and only generates the
                    // ant
                    // build file
                    // if (tool.getOutputType() == ToolOutputType.INPUT_VALUES) {
                    new TestSuiteGenerator(snippetProject, configuration.getOutputDir(), tool,
                            runnerProjectTag).generate();
                    // } else {
                    // out.println("This tool has already generated a test suite");
                    // }
                    break;

                case TEST_RUNNER:
                    new TestSuiteRunner(snippetProject, configuration.getOutputDir(), tool,
                            runnerProjectTag).analyze();
                    break;

                case SNIPPET_BROWSER:
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                SnippetBrowser frame = new SnippetBrowser(snippetProject);
                                frame.setVisible(true);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                    break;

                case EXPORT_CSV:
                    new CsvGenerator(snippetProject, configuration.getOutputDir(), tool,
                            runnerProjectTag).generate();
                    break;

                case EXPORT_CSV_BATCH:
                    // FIXME runnerProjectTag is a list of tags separated by ','
                    String toolNames = configuration.getToolConfigurations()
                            .stream()
                            .map(tc -> tc.getName())
                            .collect(joining(","));
                    new CsvBatchGenerator(snippetProject, configuration.getOutputDir(),
                            toolNames, runnerProjectTag).generateAll();
                    break;

                case RUNNER_PROJECT_BROWSER:
                    Application.launch(RunnerProjectBrowser.class);
                    break;

                default:
                    throw new UnsupportedOperationException("Unknown task: " + applicationTask);
            }
        } catch (Exception ex) {
            errorOutput.println("Exception: " + ex.getMessage());
            LOG.error("Exception", ex);
            throw new RuntimeException(ex);
        }
    }

    private Path selectSnippetProjectDir(Collection<Path> snippetProjectDirs) throws IOException {
        // automatically select if only one is present
        Path[] items = snippetProjectDirs.toArray(new Path[0]);
        if (items.length > 1) {
            return selectItemFromArray("Please select a snippet project:", items, 1,
                    p -> p.toString());
        } else {
            Path dir = items[0];
            output.println("Using the only available snippet project: " + dir);
            return dir;
        }
    }

    private ApplicationTask selectApplicationTask() throws IOException {
        return selectItemFromArray("Please select a task:", ApplicationTask.values(), 0,
                t -> t.toString());
    }

    private SetteToolConfiguration selectToolConfiguration(
            Collection<SetteToolConfiguration> toolConfigurations) throws IOException {
        SetteToolConfiguration[] items = toolConfigurations.toArray(new SetteToolConfiguration[0]);
        return selectItemFromArray("Please select a tool:", items, 1, tc -> tc.getName());
    }

    /**
     * Asks the user to select an item from an array. This method will:
     * 
     * <ol>
     * <li>print the specified prompt and</li>
     * <li>list of available values (both ordinal and string)</li>
     * <li>asks the user to select (they can choose either the number or the text)</li>
     * <li>if the selection is valid, print the selected item and return with it</li>
     * <li>if the selection is invalid, notify the user and go back to step 1</li>
     * </ol>
     * Example:
     * 
     * <pre>
     * <code>
     * Please select:
     *   [1] First
     *   [2] Second
     * Selection: <strong>3</strong>
     * Invalid selection: 3
     * Please select:
     *   [1] First
     *   [2] Second
     * Selection: <strong>second</strong>
     * Selected: Second
     * </code>
     * </pre>
     * 
     * @param prompt
     *            the message to write for the user before listing the available items
     * @param items
     *            the array of available items
     * @param firstIndex
     *            the first index to use
     * @param toString
     *            a function mapping each item to a string (this string will be displayed during
     *            selection and this will be used to determine the selection, which is
     *            case-insensitive)
     * @return The selected value.
     * @throws IOException
     *             if an I/O exception occurs
     */
    private <T> T selectItemFromArray(String prompt, T[] items, int firstIndex,
            Function<T, String> toString)
                    throws IOException {
        checkArgument(items.length > 0);
        T selected = null;

        while (selected == null) {
            output.println(prompt);

            for (int i = 0; i < items.length; i++) {
                output.println(
                        String.format("  [%d] %s", firstIndex + i, toString.apply(items[i])));
            }

            output.print("Selection: ");
            String line = readLineOrExitIfEOF();

            try {
                // search by index
                int idx = Integer.parseInt(line) - firstIndex;

                if (0 <= idx && idx < items.length) {
                    selected = items[idx];
                }
            } catch (NumberFormatException ex) {
                // search by name
                for (T value : items) {
                    if (toString.apply(value).equalsIgnoreCase(line)) {
                        selected = value;
                    }
                }
            }

            if (selected == null) {
                output.println("Invalid selection: " + line);
            }
        }

        output.println("Selected: " + selected);
        return selected;
    }

    /**
     * Reads a line from {@link #input} and if it is <code>null</code> (EOF), then stops the
     * application.
     * 
     * @return The read line
     * @throws IOException
     *             if an I/O error occurs
     */
    private String readLineOrExitIfEOF() throws IOException {
        String line = input.readLine();

        if (line == null) {
            String msg = "EOF detected, exiting";
            errorOutput.println(msg);
            LOG.debug(msg);
            throw new EOFException();
        } else {
            return line.trim();
        }
    }
}
