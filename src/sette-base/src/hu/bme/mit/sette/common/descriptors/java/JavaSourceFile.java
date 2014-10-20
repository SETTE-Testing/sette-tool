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
package hu.bme.mit.sette.common.descriptors.java;

import hu.bme.mit.sette.common.exceptions.SetteConfigurationException;
import hu.bme.mit.sette.common.util.JavaFileUtils;
import hu.bme.mit.sette.common.validator.FileType;
import hu.bme.mit.sette.common.validator.FileValidator;
import hu.bme.mit.sette.common.validator.GeneralValidator;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.Validate;

/**
 * Represents a Java source file with the type contained by it.
 */
public final class JavaSourceFile {
    /** The file object. */
    private final File file;
    /** The Java class contained by the file. */
    private final Class<?> javaClass;

    /**
     * Creates an instance of the object. This constructor only performs null
     * checks. Please use the factory method {{@link #fromFile(File, File)}.
     *
     * @param pFile
     *            The (source) file.
     * @param pJavaClass
     *            The Java class.
     */
    private JavaSourceFile(final File pFile, final Class<?> pJavaClass) {
        Validate.notNull(pFile, "The file must not be null");
        Validate.notNull(pJavaClass, "The Java class must not be null");

        file = pFile;
        javaClass = pJavaClass;
    }

    /**
     * Creates an instance of the {@link JavaSourceFile} object by using the
     * specified source and binary directories and the specified source file.
     *
     * @param pSourceDirectory
     *            the source directory (e.g. /path/to/project/src)
     * @param pSourceFile
     *            the source file (e.g.
     *            /path/to/project/src/hu/bme/mit/sette/MyClass.java)
     * @param classLoader
     *            the class loader to be used to load the class
     * @return A {@link JavaSourceFile} object representing the Java file.
     * @throws SetteConfigurationException
     *             If an error occurred, e.g. not enough permissions to access
     *             the directory or the file, cannot retrieve canonical file
     *             names, file is in the specified directory or cannot load the
     *             Java class contained by the file.
     */
    public static JavaSourceFile fromFile(final File pSourceDirectory,
            final File pSourceFile, final ClassLoader classLoader)
                    throws SetteConfigurationException {
        Validate.notNull(pSourceDirectory,
                "The source directory must not be null");
        Validate.notNull(pSourceFile,
                "The source file must not be null");
        Validate.notNull(classLoader,
                "The class loader must not be null");

        try {
            // validate permissions
            GeneralValidator v = new GeneralValidator(
                    JavaSourceFile.class);

            FileValidator v1 = new FileValidator(pSourceDirectory);
            v1.type(FileType.DIRECTORY).readable(true).executable(true);
            v.addChildIfInvalid(v1);

            FileValidator v2 = new FileValidator(pSourceFile);
            v2.type(FileType.REGULAR_FILE).readable(true);
            v2.extension(JavaFileUtils.JAVA_SOURCE_EXTENSION);
            v.addChildIfInvalid(v2);

            v.validate();

            // get canonical file objects
            File sourceDirectory = pSourceDirectory.getCanonicalFile();
            File sourceFile = pSourceFile.getCanonicalFile();

            // get paths
            // like "/path/to/project/src"
            String sourceDirectoryPath = FilenameUtils
                    .normalizeNoEndSeparator(sourceDirectory
                            .getAbsolutePath());
            // like "/path/to/project/src/pkg/path/here/MyClass.java"
            String sourceFilePath = FilenameUtils
                    .normalizeNoEndSeparator(sourceFile
                            .getAbsolutePath());

            // check whether the file is under the specified directory
            if (FilenameUtils.directoryContains(sourceDirectoryPath,
                    sourceFilePath)) {
                // get relative path and class name
                // like "pkg/path/here/MyClass.java"
                String classPath = sourceFilePath
                        .substring(sourceDirectoryPath.length() + 1);

                // like "pkg.path.here.MyClass"
                String className = JavaFileUtils
                        .filenameToClassName(classPath);

                // load class
                Class<?> javaClass = classLoader.loadClass(className);

                // create object and return with it
                return new JavaSourceFile(sourceFile, javaClass);
            } else {
                String message = String
                        .format("The source file is not in the "
                                + "source directory\n"
                                + "(sourceDirectory: [%s])\n(sourceFile: [%s])",
                                sourceDirectory, sourceFile);
                throw new SetteConfigurationException(message);
            }
        } catch (ValidatorException e) {
            throw new SetteConfigurationException(
                    "A validation exception occurred", e);
        } catch (IOException e) {
            throw new SetteConfigurationException(
                    "An IO exception occurred", e);
        } catch (ClassNotFoundException e) {
            throw new SetteConfigurationException(
                    "The Java class could not have been loaded", e);
        }
    }

    /**
     * Returns the file.
     *
     * @return The file.
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns the Java class.
     *
     * @return The Java class.
     */
    public Class<?> getJavaClass() {
        return javaClass;
    }

    /**
     * Returns the package for the Java class.
     *
     * @return The package for the Java class.
     */
    public Package getJavaPackage() {
        return javaClass.getPackage();
    }
}
