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
package hu.bme.mit.sette.core.descriptors.eclipse;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Represents an Eclipse .project file with the Eclipse's built-in Java builder and Java nature.
 */
@RequiredArgsConstructor
public final class EclipseProjectDescriptor {
    /** Name of the Java builder. */
    private static final String JAVA_BUILDER = "org.eclipse.jdt.core.javabuilder";

    /** Name of the Java nature. */
    private static final String JAVA_NATURE = "org.eclipse.jdt.core.javanature";

    /** The name of the project. */
    @Getter
    @NonNull
    private final String name;

    @Getter
    /** The comment for the project. */
    private final String comment;

    /**
     * Creates the XML document for the .project file.
     *
     * @return The XML document.
     * @throws ParserConfigurationException
     *             If the {@link DocumentBuilder} cannot be created which satisfies the
     *             configuration requested.
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
