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

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.ImmutableList;

import lombok.NonNull;

/**
 * Represents an Eclipse .classpath file.
 */
public final class EclipseClasspathDescriptor {
    /** List containing the entries of the classpath. */
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
    public void addEntry(@NonNull EclipseClasspathEntry classpathEntry) {
        classpathEntries.add(classpathEntry);
    }

    /**
     * Adds a classpath entry to the classpath descriptor.
     * 
     * @param kind
     *            The path of the entry.
     * @param path
     *            The path of the entry.
     */
    public void addEntry(EclipseClasspathEntryKind kind, String path) {
        addEntry(new EclipseClasspathEntry(kind, path));
    }

    public ImmutableList<EclipseClasspathEntry> getClasspathEntries() {
        return ImmutableList.copyOf(classpathEntries);
    }

    /**
     * Creates the XML document for the .classpath file.
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
        Element rootTag = document.createElement("classpath");
        document.appendChild(rootTag);

        // add classpath entry tags
        for (EclipseClasspathEntry classpathEntry : classpathEntries) {
            Element tag = document.createElement("classpathentry");
            tag.setAttribute("kind", classpathEntry.getKind().getAttrValue());
            tag.setAttribute("path", classpathEntry.getPath());
            rootTag.appendChild(tag);
        }

        document.normalizeDocument();
        return document;
    }
}
