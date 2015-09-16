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

import org.apache.commons.lang3.Validate;

/**
 * Represents a classpath entry for an Eclipse .classpath file.
 */
public final class EclipseClasspathEntry {
    /**
     * Enumeration containing the possible kinds of classpath entries.
     */
    public enum Kind {
        /** The entry is a source directory which should be considered during build. */
        SOURCE("src"),

        /** The entry is a container which should be used during build (e.g. JRE_CONTAINER). */
        CONTAINER("con"),

        /** The entry is a library which should be included in the build path. */
        LIBRARY("lib"),

        /** The entry is a build output directory. */
        OUTPUT("output");

        /** The value of the XML attribute. */
        private final String attrValue;

        /**
         * Initialises the instance.
         *
         * @param attrValue
         *            The value of the XML attribute.
         */
        private Kind(String attrValue) {
            this.attrValue = attrValue;
        }

        /**
         * Returns the value of the XML attribute.
         *
         * @return The value of the XML attribute.
         */
        public String getAttrValue() {
            return attrValue;
        }
    }

    /** Path for the JRE container. */
    public static final String JRE_CONTAINER_PATH;

    /** Entry object for the JRE container. */
    public static final EclipseClasspathEntry JRE_CONTAINER;

    static {
        JRE_CONTAINER_PATH = "org.eclipse.jdt.launching.JRE_CONTAINER";
        JRE_CONTAINER = new EclipseClasspathEntry(Kind.CONTAINER,
                EclipseClasspathEntry.JRE_CONTAINER_PATH);
    }

    /** The kind of the entry. */
    private final Kind kind;

    /** The path for the entry. */
    private final String path;

    /**
     * Creates an instance of the object.
     *
     * @param kind
     *            The kind of the entry.
     * @param path
     *            The path for the entry.
     */
    public EclipseClasspathEntry(Kind kind, String path) {
        Validate.notNull(kind, "The kind should not be null");
        Validate.notBlank(path, "The path should not be blank");
        this.kind = kind;
        this.path = path.trim();
    }

    /**
     * Returns the kind of the entry.
     *
     * @return The kind of the entry.
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Returns the path for the entry.
     *
     * @return The path for the entry.
     */
    public String getPath() {
        return path;
    }
}
