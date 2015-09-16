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
package hu.bme.mit.sette.common.descriptors.eclipse;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;

import hu.bme.mit.sette.common.util.XmlUtils;

/**
 * Represents an Eclipse project and its .project and .classpath files with the Eclipse's built-in
 * Java builder and Java nature.
 */
public final class EclipseProject {
    /** The descriptor of the .project file. */
    private EclipseProjectDescriptor projectDescriptor;

    /** The descriptor of the .classpath file. */
    private EclipseClasspathDescriptor classpathDescriptor;

    /**
     * Creates an instance of the object.
     *
     * @param name
     *            The name of the project.
     */
    public EclipseProject(String name) {
        setProjectDescriptor(new EclipseProjectDescriptor(name));
    }

    /**
     * Creates an instance of the object.
     *
     * @param name
     *            The name of the project.
     * @param comment
     *            The comment for the project.
     */
    public EclipseProject(String name, String comment) {
        setProjectDescriptor(new EclipseProjectDescriptor(name, comment));
    }

    /**
     * Creates an instance of the object.
     *
     * @param projectDescriptor
     *            The descriptor of the .project file.
     */
    public EclipseProject(EclipseProjectDescriptor projectDescriptor) {
        setProjectDescriptor(projectDescriptor);
    }

    /**
     * Creates an instance of the object.
     *
     * @param projectDescriptor
     *            The descriptor of the .project file.
     * @param classpathDescriptor
     *            The descriptor of the .classpath file.
     */
    public EclipseProject(EclipseProjectDescriptor projectDescriptor,
            EclipseClasspathDescriptor classpathDescriptor) {
        setProjectDescriptor(projectDescriptor);
        setClasspathDescriptor(classpathDescriptor);
    }

    /**
     * Returns the descriptor of the .project file.
     *
     * @return The descriptor of the .project file.
     */
    public EclipseProjectDescriptor getProjectDescriptor() {
        return projectDescriptor;
    }

    /**
     * Sets the descriptor of the .project file.
     *
     * @param projectDescriptor
     *            The descriptor of the .project file.
     */
    public void setProjectDescriptor(EclipseProjectDescriptor projectDescriptor) {
        Validate.notNull(projectDescriptor, "The project descriptor should not be null");
        this.projectDescriptor = projectDescriptor;
    }

    /**
     * Returns the descriptor of the .classpath file.
     *
     * @return The descriptor of the .classpath file.
     */
    public EclipseClasspathDescriptor getClasspathDescriptor() {
        return classpathDescriptor;
    }

    /**
     * Sets the descriptor of the .classpath file.
     *
     * @param classpathDescriptor
     *            The descriptor of the .classpath file.
     */
    public void setClasspathDescriptor(EclipseClasspathDescriptor classpathDescriptor) {
        this.classpathDescriptor = classpathDescriptor;
    }

    /**
     * Saves the project files to the specified directory. Please note that the existing files will
     * be overwritten.
     *
     * @param directory
     *            The directory. If it does not exist, it will be created.
     * @throws IOException
     *             If the directory cannot be created or the file already exists but is not a
     *             directory.
     * @throws ParserConfigurationException
     *             If a DocumentBuilder cannot be created which satisfies the configuration
     *             requested or when it is not possible to create a Transformer instance.
     * @throws TransformerException
     *             If an unrecoverable error occurs during the course of the transformation.
     */
    public void save(File directory)
            throws IOException, ParserConfigurationException, TransformerException {
        Validate.notNull(directory, "The directory must not be null");

        // create directory if not exists
        if (!directory.exists()) {
            FileUtils.forceMkdir(directory);
        }

        // save .project file
        File projectFile = new File(directory, ".project");
        XmlUtils.writeXml(projectDescriptor.createXmlDocument(), projectFile);

        // save .classpath file if classpath is specified
        if (classpathDescriptor != null) {
            File classpathFile = new File(directory, ".classpath");
            XmlUtils.writeXml(classpathDescriptor.createXmlDocument(), classpathFile);
        }
    }
}
