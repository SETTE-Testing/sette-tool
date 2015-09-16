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
// TODO z revise this file
package hu.bme.mit.sette.common.util;

import java.io.File;
import java.io.OutputStream;
import java.io.Writer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.Validate;
import org.w3c.dom.Document;

/**
 * Contains static helper methods for XML file manipulation.
 */
public final class XmlUtils {
    /** Static class. */
    private XmlUtils() {
        throw new UnsupportedOperationException("Static class");
    }

    /**
     * Writes the specified XML document to the specified file.
     *
     * @param document
     *            The XML document.
     * @param file
     *            The output file.
     * @throws ParserConfigurationException
     *             When it is not possible to create a Transformer instance.
     * @throws TransformerException
     *             If an unrecoverable error occurs during the course of the transformation.
     */
    public static void writeXml(Document document, File file)
            throws ParserConfigurationException, TransformerException {
        Validate.notNull(document, "The document must not be null");
        Validate.notNull(file, "The file must not be null");

        XmlUtils.transformXml(document, new StreamResult(file));
    }

    /**
     * Writes the specified XML document to the specified output stream.
     *
     * @param document
     *            The XML document.
     * @param outputStream
     *            The output stream.
     * @throws ParserConfigurationException
     *             When it is not possible to create a Transformer instance.
     * @throws TransformerException
     *             If an unrecoverable error occurs during the course of the transformation.
     */
    public static void writeXml(Document document, OutputStream outputStream)
            throws ParserConfigurationException, TransformerException {
        Validate.notNull(document, "The document must not be null");
        Validate.notNull(outputStream, "The outputStream must not be null");

        XmlUtils.transformXml(document, new StreamResult(outputStream));
    }

    /**
     * Writes the specified XML document with the specified writer.
     *
     * @param document
     *            The XML document.
     * @param writer
     *            The writer.
     * @throws ParserConfigurationException
     *             When it is not possible to create a Transformer instance.
     * @throws TransformerException
     *             If an unrecoverable error occurs during the course of the transformation.
     */
    public static void writeXml(Document document, Writer writer)
            throws ParserConfigurationException, TransformerException {
        Validate.notNull(document, "The document must not be null");
        Validate.notNull(writer, "The writer must not be null");

        XmlUtils.transformXml(document, new StreamResult(writer));
    }

    /**
     * Transforms the specified XML document to the specified result.
     *
     * @param document
     *            The XML document.
     * @param result
     *            The result.
     * @throws TransformerException
     *             If an unrecoverable error occurs during the course of the transformation.
     */
    private static void transformXml(Document document, Result result) throws TransformerException {
        Validate.notNull(document, "The document must not be null");
        Validate.notNull(result, "The result must not be null");

        // write XML to stream
        Transformer transformer = TransformerFactory.newInstance().newTransformer();

        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        transformer.transform(new DOMSource(document), result);
    }
}
