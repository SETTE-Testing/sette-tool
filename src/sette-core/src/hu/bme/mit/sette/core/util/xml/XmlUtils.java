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
// NOTE revise this file
package hu.bme.mit.sette.core.util.xml;

import java.nio.file.Path;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;
import org.w3c.dom.Document;

import hu.bme.mit.sette.core.util.io.PathUtils;
import lombok.NonNull;

/**
 * Contains static helper methods for XML file manipulation.
 */
public final class XmlUtils {
    private static Serializer xmlSerializer;

    /** Static class. */
    private XmlUtils() {
        throw new UnsupportedOperationException("Static class");
    }

    static {
        xmlSerializer = new Persister(new AnnotationStrategy(),
                new Format("<?xml version=\"1.0\" encoding= \"UTF-8\" ?>"));
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
            throw new XmlException("XML transformation has failed", ex);
        }
    }

    /**
     * Validates and serialises the object to an XML file.
     * 
     * @param object
     *            the object to serialise
     * @param file
     *            the target file (missing parent directories will be created)
     * @throws XmlException
     *             If an XML related exception occurs.
     */
    public static void serializeToXml(@NonNull XmlElement object, @NonNull Path file)
            throws XmlException {
        try {
            object.validate();

            PathUtils.createDir(file.getParent());
            PathUtils.deleteIfExists(file);

            xmlSerializer.write(object, file.toFile());
        } catch (Exception ex) {
            throw new XmlException("XML serialization has failed", ex);
        }
    }

    /**
     * Deserialises and validates an object from an XML file.
     * 
     * @param cls
     *            the class of the object
     * @param file
     *            the source file
     * @return the deserialised object
     * @throws XmlException
     *             If an XML related exception occurs.
     */
    public static <T extends XmlElement> T deserializeFromXml(@NonNull Class<T> cls,
            @NonNull Path file) throws XmlException {
        try {
            T object = xmlSerializer.read(cls, file.toFile());
            object.validate();
            return object;
        } catch (Exception ex) {
            throw new XmlException("XML deserialization has failed", ex);
        }
    }
}
