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
package hu.bme.mit.sette.common.descriptors.java;

import hu.bme.mit.sette.common.exceptions.ConfigurationException;
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
 * Represents a Java source file and stores the type contained by it.
 */
public final class JavaSourceFile {
    /** The file object. */
    private final File file;

    /** The Java class contained by the file. */
    private final Class<?> javaClass;

    /**
     * Creates an instance of the object. This constructor only performs null checks. Please use the
     * factory method {{@link #fromFile(File, File)}.
     *
     * @param file
     *            The (source) file.
     * @param javaClass
     *            The Java class.
     */
    private JavaSourceFile(File file, Class<?> javaClass) {
        Validate.notNull(file, "The file must not be null");
        Validate.notNull(javaClass, "The Java class must not be null");

        this.file = file;
        this.javaClass = javaClass;
    }

    /**
     * Creates an instance of the {@link JavaSourceFile} object by using the specified source and
     * binary directories and the specified source file. Please note that this method will fail if
     * the Java class cannot be loaded using the given class loader.
     *
     * @param sourceDirectory
     *            the source directory (e.g. /path/to/project/src)
     * @param sourceFile
     *            the source file (e.g. /path/to/project/src/hu/bme/mit/sette/MyClass.java)
     * @param classLoader
     *            the class loader to be used to load the class
     * @return A {@link JavaSourceFile} object representing the Java file.
     * @throws ConfigurationException
     *             If an error occurred, e.g. not enough permissions to access the directory or the
     *             file, cannot retrieve canonical file names, file is in the specified directory or
     *             cannot load the Java class contained by the file.
     */
    public static JavaSourceFile fromFile(File sourceDirectory, File sourceFile,
            ClassLoader classLoader) throws ConfigurationException {
        Validate.notNull(sourceDirectory, "The source directory must not be null");
        Validate.notNull(sourceFile, "The source file must not be null");
        Validate.notNull(classLoader, "The class loader must not be null");

        try {
            //
            // validate permissions
            //
            GeneralValidator validator = new GeneralValidator(JavaSourceFile.class);

            FileValidator srcDirValidator = new FileValidator(sourceDirectory)
                    .type(FileType.DIRECTORY).readable(true).executable(true);
            validator.addChildIfInvalid(srcDirValidator);

            FileValidator srcFileValidator = new FileValidator(sourceFile)
                    .type(FileType.REGULAR_FILE).readable(true)
                    .extension(JavaFileUtils.JAVA_SOURCE_EXTENSION);
            validator.addChildIfInvalid(srcFileValidator);

            validator.validate();

            //
            // get canonical file objects and absolute paths
            //
            sourceDirectory = sourceDirectory.getCanonicalFile();
            sourceFile = sourceFile.getCanonicalFile();

            // like "/path/to/project/src"
            String sourceDirectoryPath = FilenameUtils
                    .normalizeNoEndSeparator(sourceDirectory.getAbsolutePath());
            // like "/path/to/project/src/pkg/path/here/MyClass.java"
            String sourceFilePath = FilenameUtils
                    .normalizeNoEndSeparator(sourceFile.getAbsolutePath());

            //
            // check whether the file is under the specified directory
            //
            if (FilenameUtils.directoryContains(sourceDirectoryPath, sourceFilePath)) {
                // get relative path and class name
                // like "pkg/path/here/MyClass.java"
                String classPath = sourceFilePath.substring(sourceDirectoryPath.length() + 1);

                // like "pkg.path.here.MyClass"
                String className = JavaFileUtils.filenameToClassName(classPath);

                // load class
                Class<?> javaClass = classLoader.loadClass(className);

                // create object and return with it
                return new JavaSourceFile(sourceFile, javaClass);
            } else {
                String message = String.format(
                        "The source file is not in the source directory\n"
                                + "(sourceDirectory: [%s])\n(sourceFile: [%s])",
                        sourceDirectory, sourceFile);
                throw new ConfigurationException(message);
            }
        } catch (ValidatorException ex) {
            throw new ConfigurationException("A validation exception occurred", ex);
        } catch (IOException ex) {
            throw new ConfigurationException("An IO exception occurred", ex);
        } catch (ClassNotFoundException ex) {
            throw new ConfigurationException("The Java class could not have been loaded", ex);
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
