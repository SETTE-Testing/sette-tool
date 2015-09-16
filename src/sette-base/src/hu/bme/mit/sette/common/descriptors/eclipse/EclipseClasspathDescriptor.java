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

import hu.bme.mit.sette.common.descriptors.eclipse.EclipseClasspathEntry.Kind;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Represents an Eclipse .classpath file.
 */
public final class EclipseClasspathDescriptor {
    /** List containing the entries for the classpath. */
    private final List<EclipseClasspathEntry> classpathEntries;

    /** Creates an instance of the object. */
    public EclipseClasspathDescriptor() {
        classpathEntries = new ArrayList<>();
    }

    /**
     * Adds a classpath entry to the classpath descriptor.
     *
     * @param classpathEntry
     *            The classpath entry.
     */
    public void addEntry(EclipseClasspathEntry classpathEntry) {
        classpathEntries.add(classpathEntry);
    }

    /**
     * Adds a classpath entry to the classpath descriptor.
     *
     * @param kind
     *            The kind of the entry.
     * @param path
     *            The path for the entry.
     */
    public void addEntry(Kind kind, String path) {
        this.addEntry(new EclipseClasspathEntry(kind, path));
    }

    /**
     * Returns the list storing the classpath entries.
     *
     * @return The list storing the classpath entries.
     */
    public List<EclipseClasspathEntry> classpathEntries() {
        return classpathEntries;
    }

    /**
     * Creates the XML document for the .classpath file.
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
        Element rootTag = document.createElement("classpath");
        document.appendChild(rootTag);

        // add classpath entry tags
        for (EclipseClasspathEntry classpathEntry : classpathEntries) {
            Element tag = document.createElement("classpathentry");
            tag.setAttribute("kind", classpathEntry.getKind().getAttrValue());
            tag.setAttribute("path", classpathEntry.getPath());
            rootTag.appendChild(tag);
        }

        return document;
    }
}
