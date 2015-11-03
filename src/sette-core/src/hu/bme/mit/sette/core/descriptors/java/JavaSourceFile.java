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
package hu.bme.mit.sette.core.descriptors.java;

  import java.io.File;
import java.io.IOException;

import hu.bme.mit.sette.core.configuration.SetteConfigurationException;
import hu.bme.mit.sette.core.validator.PathType;
import hu.bme.mit.sette.core.validator.PathValidator;
import hu.bme.mit.sette.core.validator.ValidationContext;
import hu.bme.mit.sette.core.validator.ValidationException;
import lombok.NonNull;

/**
 * Represents a Java source file and stores the type contained by it.
 */
// XXX remove class if possible
@Deprecated
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
    private JavaSourceFile(@NonNull File file, @NonNull Class<?> javaClass) {
        this.file = file;
        this.javaClass = javaClass;
    }

    /**
     * Creates an instance of the {@link JavaSourceFile} object by using the specified source and
     * binary directories and the specified source file. Please note that this method will fail if
     * the Java class cannot be loaded using the given class loader.
     *
     * @param sourceDir
     *            the source directory (e.g. /path/to/project/src)
     * @param sourceFile
     *            the source file (e.g. /path/to/project/src/hu/bme/mit/sette/MyClass.java)
     * @param classLoader
     *            the class loader to be used to load the class
     * @return A {@link JavaSourceFile} object representing the Java file.
     * @throws SetteConfigurationException
     *             If an error occurred, e.g. not enough permissions to access the directory or the
     *             file, cannot retrieve canonical file names, file is in the specified directory or
     *             cannot load the Java class contained by the file.
     */
    public static JavaSourceFile fromFile(@NonNull File sourceDir, @NonNull File sourceFile,
            @NonNull ClassLoader classLoader) throws SetteConfigurationException {
        try {
            //
            // validate permissions
            //
            ValidationContext vc = new ValidationContext(JavaSourceFile.class);

            PathValidator srcDirValidator = new PathValidator(sourceDir.toPath())
                    .type(PathType.DIRECTORY).readable(true).executable(true);
            vc.addValidator(srcDirValidator);

            PathValidator srcFileValidator = new PathValidator(sourceFile.toPath())
                    .type(PathType.REGULAR_FILE).readable(true).extension("java");
            vc.addValidator(srcFileValidator);

            vc.validate();

            //
            // get canonical file objects and absolute paths
            //
            sourceDir = sourceDir.getCanonicalFile();
            sourceFile = sourceFile.getCanonicalFile();

            // like "/path/to/project/src"
            // TODO normalize, nio Path
            String sourceDirPath = sourceDir.getCanonicalFile().getAbsolutePath();
            // like "/path/to/project/src/pkg/path/here/MyClass.java"
            // TODO normalize, nio Path
            String sourceFilePath = sourceFile.getCanonicalFile().getAbsolutePath();

            //
            // check whether the file is under the specified directory
            //
            // FIXME it was FilenameUtils.directoryContains(sourceDirectoryPath, sourceFilePath)
            if (sourceFilePath.startsWith(sourceDirPath)) {
                // get relative path and class name
                // like "pkg/path/here/MyClass.java"
                String classPath = sourceFilePath.substring(sourceDirPath.length() + 1);

                // like "pkg.path.here.MyClass"
                String className = classPath.replace('/', '.');

                // load class
                Class<?> javaClass = classLoader.loadClass(className);

                // create object and return with it
                return new JavaSourceFile(sourceFile, javaClass);
            } else {
                String message = String.format(
                        "The source file is not in the source directory\n"
                                + "(sourceDir: [%s])\n(sourceFile: [%s])",
                        sourceDir, sourceFile);
                throw new SetteConfigurationException(message);
            }
        } catch (ValidationException ex) {
            throw new SetteConfigurationException("A validation exception occurred", ex);
        } catch (IOException ex) {
            throw new SetteConfigurationException("An IO exception occurred", ex);
        } catch (ClassNotFoundException ex) {
            throw new SetteConfigurationException("The Java class could not have been loaded", ex);
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
}
