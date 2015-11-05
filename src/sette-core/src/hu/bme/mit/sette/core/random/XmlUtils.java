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
// NOTE revise this file
package hu.bme.mit.sette.core.random;

import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Path;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import hu.bme.mit.sette.core.exceptions.XmlException;
import lombok.NonNull;

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
     * @throws XmlException
     *             If an XML related exception occurs.
     */
    public static void writeXml(@NonNull Document document, @NonNull Path file)
            throws XmlException {
        XmlUtils.transformXml(document, new StreamResult(file.toFile()));
    }

    /**
     * Writes the specified XML document to the specified output stream.
     *
     * @param document
     *            The XML document.
     * @param outputStream
     *            The output stream.
     * @throws XmlException
     *             If an XML related exception occurs.
     */
    public static void writeXml(@NonNull Document document, @NonNull OutputStream outputStream)
            throws XmlException {
        XmlUtils.transformXml(document, new StreamResult(outputStream));
    }

    /**
     * Writes the specified XML document with the specified writer.
     *
     * @param document
     *            The XML document.
     * @param writer
     *            The writer.
     * @throws XmlException
     *             If an XML related exception occurs.
     */
    public static void writeXml(@NonNull Document document, @NonNull Writer writer)
            throws XmlException {
        XmlUtils.transformXml(document, new StreamResult(writer));
    }

    /**
     * Transforms the specified XML document to the specified result.
     *
     * @param document
     *            The XML document.
     * @param result
     *            The result.
     * @throws XmlException
     *             If an XML related exception occurs.
     */
    private static void transformXml(@NonNull Document document, @NonNull Result result)
            throws XmlException {
        // write XML to stream
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();

            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            transformer.transform(new DOMSource(document), result);
        } catch (TransformerException ex) {
            throw new XmlException("Transformation failed", ex);
        }
    }
}
