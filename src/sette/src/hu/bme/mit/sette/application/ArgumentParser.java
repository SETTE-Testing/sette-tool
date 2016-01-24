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
package hu.bme.mit.sette.application;

import java.io.PrintStream;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerRegistry;
import org.kohsuke.args4j.ParserProperties;

import hu.bme.mit.sette.core.configuration.SetteConfiguration;
import hu.bme.mit.sette.core.configuration.SetteToolConfiguration;
import lombok.Getter;
import lombok.NonNull;

// NOT SPECIFIED -> ASK
// INVALID SPECIFIED -> FAIL
public class ArgumentParser {
    /** The SETTE configuration to use while parsing the arguments. */
    private final SetteConfiguration configuration;

    /** The {@link PrintStream} to use for writing error messages. */
    private final PrintStream errorOutput;

    @Getter
    @Option(name = "--snippet-project-dir", metaVar = "[PROJECT_NAME]",
            usage = "The path to the snippet-project (relative to the base-directory) to use - "
                    + "if missing, then the user will be asked to select one from the projects "
                    + "specified in the configuration")
    private String snippetProjectDir = null;

    @Getter
    @Option(name = "--task", usage = "The task to execute")
    private ApplicationTask applicationTask = null;

    @Getter
    @Option(name = "--tool", usage = "The tool to use")
    private SetteToolConfiguration toolConfiguration = null;

    @Getter
    @Option(name = "--runner-project-tag", metaVar = "[TAG]",
            usage = "The tag of the desired runner project")
    private String runnerProjectTag = null;

    @Getter
    @Option(name = "--runner-timeout", handler = TimeInMsOptionHandler.class,
            usage = "Timeout for execution of a tool on one snippet - if missing, then the value "
                    + "specified in the configuration will be used")
    private int runnerTimeoutInMs;

    @Getter
    @Option(name = "--backup", usage = "Set the backup policy for runner projects "
            + "(used when the runner project already exists before generation)")
    private BackupPolicy backupPolicy = BackupPolicy.ASK;

    @Option(name = "--help", usage = "Prints the help message", help = true, hidden = true)
    private boolean help = false;

    /**
     * Instantiates a new SETTE argument parser.
     *
     * @param configuration
     *            the SETTE configuration to use while parsing the arguments
     * @param errorOutput
     *            the {@link PrintStream} to use for writing error messages
     */
    public ArgumentParser(@NonNull SetteConfiguration configuration,
            @NonNull PrintStream errorOutput) {
        this.configuration = configuration;
        this.errorOutput = errorOutput;
        this.runnerTimeoutInMs = configuration.getRunnerTimeoutInMs();
    }

    /**
     * Parses and checks the program arguments. If the arguments are invalid or the user requested
     * the help message, this method returns <code>false</code> and writes the message for the user.
     * If the parsing is successful, this method does not print anything to the output.
     * 
     * @param args
     *            the arguments to parse
     * @return <code>true</code> if the arguments were successfully parsed, <code>false</code> if
     *         arguments are invalid or the user requested the help message (<code>false</code>
     *         generally means that SETTE should stop)
     */
    public boolean parse(@NonNull String... args) {
        // register handler for tool and set the current configuration for it
        OptionHandlerRegistry.getRegistry().registerHandler(SetteToolConfiguration.class,
                ToolOptionHandler.class);
        ToolOptionHandler.configuration = configuration;

        // parse args with preset properties
        ParserProperties parserProps = ParserProperties.defaults()
                .withShowDefaults(true)
                .withUsageWidth(80);
        CmdLineParser parser = new CmdLineParser(this, parserProps);

        try {
            parser.parseArgument(args);

            if (help) {
                printHelp(parser);
                return false;
            } else {
                return true;
            }
        } catch (CmdLineException ex) {
            errorOutput.println(ex.getMessage());
            printHelp(parser);
            return false;
        }
    }

    private void printHelp(CmdLineParser parser) {
        errorOutput.println("Usage:");
        parser.printUsage(errorOutput);
        errorOutput.println();
    }
}
