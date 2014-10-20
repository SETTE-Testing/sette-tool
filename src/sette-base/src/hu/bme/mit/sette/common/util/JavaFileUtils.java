/*
 * SETTE - Symbolic Execution based Test Tool Evaluator
 *
 * SETTE is a tool to help the evaluation and comparison of symbolic execution
 * based test input generator tools.
 *
 * Budapest University of Technology and Economics (BME)
 *
 * Authors: Lajos Cseppentő <lajos.cseppento@inf.mit.bme.hu>, Zoltán Micskei
 * <micskeiz@mit.bme.hu>
 *
 * Copyright 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package hu.bme.mit.sette.common.util;

import java.io.File;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Contains static helper methods for Java file manipulation.
 */
public final class JavaFileUtils {
    /** Static class. */
    private JavaFileUtils() {
        throw new UnsupportedOperationException("Static class");
    }

    /** File extension separator character. */
    public static final char FILE_EXTENSION_SEPARATOR = '.';
    /** Java package separator character. */
    public static final char PACKAGE_SEPARATOR = '.';

    /** Extension for Java source files. */
    public static final String JAVA_SOURCE_EXTENSION = "java";
    /** Extension for Java class files. */
    public static final String JAVA_CLASS_EXTENSION = "class";
    /** Extension for Java JAR files. */
    public static final String JAVA_JAR_EXTENSION = "jar";

    /**
     * Converts a filename to a Java package name by transliterating the file
     * separator characters to the package separator character.
     *
     * @param filename
     *            The filename (e.g. hu/bme/mit/sette)
     * @return The package name (e.g. hu.bme.mit.sette)
     */
    public static String filenameToPackageName(final String filename) {
        return StringUtils.replaceChars(filename, File.separatorChar,
                JavaFileUtils.PACKAGE_SEPARATOR);
    }

    /**
     * Converts a Java package name to filename by transliterating the package
     * separator characters to the file separator charater.
     *
     * @param packageName
     *            The package name (e.g. hu.bme.mit.sette)
     * @return The filename (e.g. hu/bme/mit/sette)
     */
    public static String packageNameToFilename(final String packageName) {
        return StringUtils.replaceChars(packageName,
                JavaFileUtils.PACKAGE_SEPARATOR, File.separatorChar);
    }

    /**
     * Converts a filename to a Java class name by transliterating the file
     * separator characters to the package separator character.
     *
     * @param filename
     *            The filename (e.g. hu/bme/mit/sette/MyClass.java)
     * @return The class name (e.g. hu.bme.mit.sette.MyClass)
     */
    public static String filenameToClassName(final String filename) {
        return StringUtils.replaceChars(
                FilenameUtils.removeExtension(filename),
                File.separatorChar, JavaFileUtils.PACKAGE_SEPARATOR);
    }

    /**
     * Converts a Java class name to source filename by transliterating the
     * package separator characters to the file separator charater and adding
     * the extension.
     *
     * @param className
     *            The class name (e.g. hu.bme.mit.sette.MyClass)
     * @return The filename (e.g. hu/bme/mit/sette/MyClass.java)
     */
    public static String classNameToSourceFilename(
            final String className) {
        return JavaFileUtils.packageNameToFilename(className)
                + JavaFileUtils.FILE_EXTENSION_SEPARATOR
                + JavaFileUtils.JAVA_SOURCE_EXTENSION;
    }

    /**
     * Converts a Java class name to class filename by transliterating the
     * package separator characters to the file separator charater and adding
     * the extension.
     *
     * @param className
     *            The class name (e.g. hu.bme.mit.sette.MyClass)
     * @return The filename (e.g. hu/bme/mit/sette/MyClass.class)
     */
    public static String classNameToClassFilename(final String className) {
        Validate.notBlank(className, "Class name must not be blank");

        return JavaFileUtils.packageNameToFilename(className)
                + JavaFileUtils.FILE_EXTENSION_SEPARATOR
                + JavaFileUtils.JAVA_CLASS_EXTENSION;
    }
}
