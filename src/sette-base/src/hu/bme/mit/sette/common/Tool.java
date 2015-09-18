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
package hu.bme.mit.sette.common;

import hu.bme.mit.sette.common.model.snippet.SnippetProject;
import hu.bme.mit.sette.common.snippets.JavaVersion;
import hu.bme.mit.sette.common.tasks.RunResultParser;
import hu.bme.mit.sette.common.tasks.RunnerProjectGenerator;
import hu.bme.mit.sette.common.tasks.RunnerProjectRunner;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Represents a tool which should be evaluated by SETTE.
 */
public abstract class Tool implements Comparable<Tool> {
    /** The name of the tool. */
    private final String name;

    /** The full name of the tool. */
    private final String fullName;

    /** The version of the tool. */
    private final String version;

    /**
     * Instantiates a new tool.
     *
     * @param name
     *            the name of the tool
     * @param fullName
     *            the full name of the tool
     * @param version
     *            the version of the tool
     */
    public Tool(String name, String fullName, String version) {
        Validate.notBlank(name, "The name must not be blank");

        this.name = name;
        this.fullName = StringUtils.isBlank(fullName) ? name : fullName;
        this.version = StringUtils.isBlank(version) ? "unknown version" : version;
    }

    /**
     * Adds the tool to the {@link ToolRegister}.
     */
    public void register() {
        ToolRegister.register(this);
    }

    /**
     * Gets the name of the tool.
     *
     * @return the name of the tool
     */
    public final String getName() {
        return name;
    }

    /**
     * Gets the full name of the tool.
     *
     * @return the full name of the tool
     */
    public final String getFullName() {
        return fullName;
    }

    /**
     * Gets the version of the tool.
     *
     * @return the version of the tool
     */
    public String getVersion() {
        return version;
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
    public final boolean supportsJavaVersion(JavaVersion javaVersion) {
        Validate.notNull(javaVersion, "The Java version must not be null");

        return getSupportedJavaVersion().compareTo(javaVersion) >= 0;
    }

    /**
     * Creates a runner project generator.
     *
     * @param snippetProject
     *            the snippet project
     * @param outputDirectory
     *            the output directory
     * @return the runner project generator
     */
    public abstract RunnerProjectGenerator<?> createRunnerProjectGenerator(
            SnippetProject snippetProject, File outputDirectory, String runnerProjectTag);

    /**
     * Creates a runner project runner.
     *
     * @param snippetProject
     *            the snippet project
     * @param outputDirectory
     *            the output directory
     * @return the runner project runner
     */
    public abstract RunnerProjectRunner<?> createRunnerProjectRunner(SnippetProject snippetProject,
            File outputDirectory, String runnerProjectTag);

    /**
     * Creates a run result parser.
     *
     * @param snippetProject
     *            the snippet project
     * @param outputDirectory
     *            the output directory
     * @param runnerProjectTag
     *            the tag of the runner project
     * @return the run result parser
     */
    public abstract RunResultParser<?> createRunResultParser(SnippetProject snippetProject,
            File outputDirectory, String runnerProjectTag);

    @Override
    public final int compareTo(Tool o) {
        if (o == null) {
            return 1;
        } else if (equals(o)) {
            return 0;
        } else {
            return name.compareToIgnoreCase(o.getName());
        }
    }

    @Override
    public final int hashCode() {
        return name.toLowerCase().hashCode();
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof Tool) {
            return o.getClass().equals(this.getClass());
        } else {
            return false;
        }
    }
}
