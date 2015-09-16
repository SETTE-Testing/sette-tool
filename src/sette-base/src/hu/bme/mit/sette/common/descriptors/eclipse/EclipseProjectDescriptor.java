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

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Represents an Eclipse .project file with the Eclipse's built-in Java builder and Java nature.
 */
public final class EclipseProjectDescriptor {
    /** Name of the Java builder. */
    private static final String JAVA_BUILDER = "org.eclipse.jdt.core.javabuilder";

    /** Name of the Java nature. */
    private static final String JAVA_NATURE = "org.eclipse.jdt.core.javanature";

    /** The name of the project. */
    private String name;

    /** The comment for the project. */
    private String comment;

    /**
     * Creates an instance of the object.
     *
     * @param name
     *            The name of the project.
     */
    public EclipseProjectDescriptor(String name) {
        this(name, "");
    }

    /**
     * Creates an instance of the object.
     *
     * @param came
     *            The name of the project.
     * @param comment
     *            The comment for the project.
     */
    public EclipseProjectDescriptor(String came, String comment) {
        setName(came);
        setComment(comment);
    }

    /**
     * Returns the name of the project.
     *
     * @return The name of the project.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the project.
     *
     * @param name
     *            The name of the project.
     */
    public void setName(String name) {
        Validate.notBlank(name, "The name must not be be blank");
        this.name = name.trim();
    }

    /**
     * Returns the comment for the project.
     *
     * @return The comment for the project.
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the comment for the project.
     *
     * @param comment
     *            The comment for the project.
     */
    public void setComment(String comment) {
        this.comment = StringUtils.trimToEmpty(comment);
    }

    /**
     * Creates the XML document for the .project file.
     *
     * @return The XML document.
     * @throws ParserConfigurationException
     *             If a DocumentBuilder cannot be created which satisfies the configuration
     *             requested.
     */
    public Document createXmlDocument() throws ParserConfigurationException {
        // create document object
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        document.setXmlVersion("1.0");
        document.setXmlStandalone(true);

        // add root tag
        Element rootTag = document.createElement("projectDescription");
        document.appendChild(rootTag);

        // add name and comment tags
        rootTag.appendChild(document.createElement("name")).setTextContent(name);
        rootTag.appendChild(document.createElement("comment")).setTextContent(comment);

        // add the projects tag
        rootTag.appendChild(document.createElement("projects"));

        // add the Java builder
        Element buildSpecTag = document.createElement("buildSpec");
        rootTag.appendChild(buildSpecTag);

        Element buildCommandTag = document.createElement("buildCommand");
        buildSpecTag.appendChild(buildCommandTag);

        buildCommandTag.appendChild(document.createElement("name"))
                .setTextContent(EclipseProjectDescriptor.JAVA_BUILDER);
        buildCommandTag.appendChild(document.createElement("arguments"));

        // add the Java nature
        Element naturesTag = document.createElement("natures");
        rootTag.appendChild(naturesTag);

        naturesTag.appendChild(document.createElement("nature"))
                .setTextContent(EclipseProjectDescriptor.JAVA_NATURE);

        return document;
    }
}
