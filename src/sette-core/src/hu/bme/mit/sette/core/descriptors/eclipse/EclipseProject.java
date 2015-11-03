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
package hu.bme.mit.sette.core.descriptors.eclipse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.ParserConfigurationException;

import hu.bme.mit.sette.core.exceptions.XmlException;
import hu.bme.mit.sette.core.random.XmlUtils;
import hu.bme.mit.sette.core.validator.PathValidator;
import hu.bme.mit.sette.core.validator.ValidationException;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Represents an Eclipse project and its .project and .classpath files with the Eclipse's built-in
 * Java builder and Java nature.
 */
@RequiredArgsConstructor
public final class EclipseProject {
    /** The descriptor of the .project file. */
    @Getter
    @NonNull
    private final EclipseProjectDescriptor projectDescriptor;

    /** The descriptor of the .classpath file. */
    @Getter
    private final EclipseClasspathDescriptor classpathDescriptor;

    /**
     * Saves the project files to the specified directory. Please note that the existing files will
     * be overwritten.
     *
     * @param dir
     *            The directory. If it does not exist, it will be created.
     * @throws IOException
     *             If the directory cannot be created or the file already exists but is not a
     *             directory.
     * @throws XmlException
     *             If an XML related exception occurs.
     */
    public void save(@NonNull Path dir) throws IOException, XmlException {
        // create directory if not exists
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        try {
            PathValidator.forDirectory(dir, true, null, true).validate();
        } catch (ValidationException ex) {
            throw new IllegalArgumentException(ex);
        }

        // save .project file
        try {
            Path projectFile = dir.resolve(".project");
            XmlUtils.writeXml(projectDescriptor.createXmlDocument(), projectFile);
        } catch (ParserConfigurationException ex) {
            throw new XmlException("Cannot create the content of the .project file", ex);
        }

        // save .classpath file if classpath is specified
        if (classpathDescriptor != null) {
            try {
                Path classpathFile = dir.resolve(".classpath");
                XmlUtils.writeXml(classpathDescriptor.createXmlDocument(), classpathFile);
            } catch (ParserConfigurationException ex) {
                throw new XmlException("Cannot create the content of the .classpath file", ex);
            }
        }
    }
}
