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
package hu.bme.mit.sette.core.tool;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import hu.bme.mit.sette.common.snippets.JavaVersion;
import hu.bme.mit.sette.core.configuration.SetteToolConfiguration;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tasks.RunResultParser;
import hu.bme.mit.sette.core.tasks.RunnerProjectGenerator;
import hu.bme.mit.sette.core.tasks.RunnerProjectRunner;
import hu.bme.mit.sette.core.util.io.PathUtils;
import hu.bme.mit.sette.core.validator.PathValidator;
import hu.bme.mit.sette.core.validator.ValidationException;
import hu.bme.mit.sette.core.validator.Validator;
import lombok.Getter;
import lombok.NonNull;

/**
 * Represents a tool which can be evaluated by SETTE. When inheriting from this class make sure that
 * the constructor is overridden with the same argument list, since the class might be dynamically
 * instantiated by {@link #create(String, Path, String)}.
 */
public abstract class Tool implements Comparable<Tool> {
    /** The name of the tool. */
    @Getter
    private final String name;

    /** The version of the tool. */
    @Getter
    private final String version;

    /** The tool directory. */
    @Getter
    private final Path toolDir;

    /**
     * Instantiates a new tool and parses its version from the VERSION file.
     * 
     * @param name
     *            the name of the tool
     * @param toolDir
     *            the tool directory
     * @throws IOException
     *             if an I/O error occurs
     * @throws ValidationException
     *             if validation fails
     */
    public Tool(@NonNull String name, @NonNull Path toolDir)
            throws IOException, ValidationException {
        checkArgument(!name.trim().isEmpty());
        PathValidator.forDirectory(toolDir, true, null, true).validate();

        this.name = name;
        this.toolDir = toolDir;

        // parse version
        Path versionFile = toolDir.resolve("VERSION");

        if (PathUtils.exists(versionFile)) {
            PathValidator.forRegularFile(versionFile, true, null, null, null).validate();

            List<String> versionFileNonBlankLines = PathUtils.readAllLines(versionFile)
                    .stream()
                    .map(l -> l.trim())
                    .filter(l -> !l.isEmpty())
                    .collect(Collectors.toList());

            if (versionFileNonBlankLines.size() != 1) {
                Validator<Tool> v = Validator.of(this);
                v.addError("The VERSION file should only contain exactly line with the version: "
                        + versionFileNonBlankLines);
                v.validate();
            }

            this.version = versionFileNonBlankLines.get(0);
        } else {
            // unknown version
            this.version = "";
        }
    }

    /**
     * Instantiates the specified tool using reflection.
     * <p>
     * Note: reflection-related exceptions will be wrapped into a {@link RuntimeException} and
     * re-thrown.
     *
     * @param toolConfiguration
     *            the tool configuration
     * @return the created object
     * @throws IOException
     *             if an I/O error occurs
     * @throws ValidationException
     *             if validation fails
     */
    public static Tool create(@NonNull SetteToolConfiguration toolConfiguration)
            throws IOException, ValidationException {
        try {
            Class<?> toolClass = Class.forName(toolConfiguration.getClassName());
            Constructor<?> ctor = toolClass.getConstructor(String.class, Path.class);
            return (Tool) ctor.newInstance(toolConfiguration.getName(),
                    toolConfiguration.getToolDir());
        } catch (Exception ex) {
            if (ex instanceof InvocationTargetException) {
                // re-throw probable exceptions caught from the ctor call
                Throwable targetEx = ((InvocationTargetException) ex).getTargetException();

                if (targetEx instanceof ValidationException) {
                    throw (ValidationException) targetEx;
                } else if (targetEx instanceof IOException) {
                    throw (IOException) targetEx;
                }
            }

            // wrap other exceptions
            throw new RuntimeException("Cannot instatiate tool: " + toolConfiguration, ex);
        }
    }

    /**
     * Returns the type of the generated output of the tool
     * 
     * @return the type of the generated output of the tool
     */
    public abstract ToolOutputType getOutputType();

    /**
     * Returns the latest Java version which is supported by the tool.
     *
     * @return the latest Java version which is supported by the tool.
     */
    public abstract JavaVersion getSupportedJavaVersion();

    /**
     * Returns whether the tool supports the given Java version.
     *
     * @param javaVersion
     *            the Java version
     * @return true if supports, otherwise false
     */
    public final boolean supportsJavaVersion(@NonNull JavaVersion javaVersion) {
        return getSupportedJavaVersion().compareTo(javaVersion) >= 0;
    }

    /**
     * Creates a runner project generator.
     *
     * @param snippetProject
     *            the snippet project
     * @param outputDir
     *            the output directory
     * @return the runner project generator
     */
    public abstract RunnerProjectGenerator<?> createRunnerProjectGenerator(
            SnippetProject snippetProject, Path outputDir, String runnerProjectTag);

    /**
     * Creates a runner project runner.
     *
     * @param snippetProject
     *            the snippet project
     * @param outputDir
     *            the output directory
     * @return the runner project runner
     */
    public abstract RunnerProjectRunner<?> createRunnerProjectRunner(SnippetProject snippetProject,
            Path outputDir, String runnerProjectTag);

    /**
     * Creates a run result parser.
     *
     * @param snippetProject
     *            the snippet project
     * @param outputDir
     *            the output directory
     * @param runnerProjectTag
     *            the tag of the runner project
     * @return the run result parser
     */
    public abstract RunResultParser<?> createRunResultParser(SnippetProject snippetProject,
            Path outputDir, String runnerProjectTag);

    @Override
    public final int compareTo(@NonNull Tool o) {
        return name.compareToIgnoreCase(o.name);
    }

    @Override
    public String toString() {
        return String.format(
                "%s [name=%s, version=%s, dir=%s, outputType=%s, supportedJavaVersion=%s]",
                getClass().getName(), name, version, toolDir, getOutputType(),
                getSupportedJavaVersion());
    }
}
