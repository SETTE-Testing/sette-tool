/*
 * SETTE - Symbolic Execution based Test Tool Evaluator
 *
 * SETTE is a tool to help the evaluation and comparison of symbolic execution
 * based test input generator tools.
 *
 * Budapest University of Technology and Economics (BME)
 *
 * Authors: Lajos Cseppentő <lajos.cseppento@inf.mit.bme.hu>, Zoltán Micskei
 * <micskeiz@mit.bme.hu>
 *
 * Copyright 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
     * @param pName
     *            the name of the tool
     * @param pFullName
     *            the full name of the tool
     * @param pVersion
     *            the version of the tool
     */
    public Tool(final String pName, final String pFullName,
            final String pVersion) {
        Validate.notBlank(pName, "The name must not be blank");

        name = pName;

        if (StringUtils.isBlank(pFullName)) {
            fullName = pName;
        } else {
            fullName = pFullName;
        }

        if (StringUtils.isBlank(pVersion)) {
            version = "unknown version";
        } else {
            version = pVersion;
        }

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
    public final boolean supportsJavaVersion(
            final JavaVersion javaVersion) {
        Validate.notNull(javaVersion,
                "The Java version must not be null");

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
            SnippetProject snippetProject, File outputDirectory);

    /**
     * Creates a runner project runner.
     *
     * @param snippetProject
     *            the snippet project
     * @param outputDirectory
     *            the output directory
     * @return the runner project runner
     */
    public abstract RunnerProjectRunner<?> createRunnerProjectRunner(
            SnippetProject snippetProject, File outputDirectory);

    /**
     * Creates a run result parser.
     *
     * @param snippetProject
     *            the snippet project
     * @param outputDirectory
     *            the output directory
     * @return the run result parser
     */
    public abstract RunResultParser<?> createRunResultParser(
            SnippetProject snippetProject, File outputDirectory);

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public final int compareTo(final Tool o) {
        if (o == null) {
            return 1;
        } else if (equals(o)) {
            return 0;
        } else {
            return name.compareToIgnoreCase(o.getName());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public final int hashCode() {
        return name.toLowerCase().hashCode();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public final boolean equals(final Object o) {
        if (o instanceof Tool) {
            return o.getClass().equals(this.getClass());
        } else {
            return false;
        }
    }
}
