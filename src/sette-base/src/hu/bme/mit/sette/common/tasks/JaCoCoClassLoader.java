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
package hu.bme.mit.sette.common.tasks;

import hu.bme.mit.sette.common.util.JavaFileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;
import org.jacoco.core.instr.Instrumenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class loader which loads and instruments classes for JaCoCo.
 */
public final class JaCoCoClassLoader extends ClassLoader {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final File[] binaryDirectories;
    private final Instrumenter instrumenter;

    public JaCoCoClassLoader(File[] binaryDirectories, Instrumenter instrumenter,
            ClassLoader parent) {
        super(parent);

        Validate.notEmpty(binaryDirectories,
                "The array of binary directories must not be empty or null");
        Validate.noNullElements(binaryDirectories,
                "The array of binary directories must not contain null elements");
        Validate.notNull(instrumenter, "The instrumenter must not be null");

        this.binaryDirectories = Arrays.copyOf(binaryDirectories, binaryDirectories.length);
        this.instrumenter = instrumenter;

        logger.debug("JaCoCoClassLoader has been created");
    }

    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        Validate.notBlank(className, "The class name must not be blank");

        Class<?> javaClass = findLoadedClass(className);

        if (javaClass != null) {
            // class was already loaded (and instrumented if bytecode was
            // found)
            logger.debug("{}: the class was already loaded", className);
            return javaClass;
        }

        try {
            // first try to load from one of the binary directories and
            // instrument the class
            byte[] bytes = readBytes(className);

            if (bytes != null) {
                logger.debug("{}: instrumenting and defining class", className);

                // instrument
                byte[] instrumentedBytes = instrumenter.instrument(bytes, className);

                // define class
                return defineClass(className, instrumentedBytes, 0, instrumentedBytes.length);
            } else {
                // was not found, try to load with the parent, but it will
                // not be instrumented
                logger.debug("{}: calling super.loadClass() (corresponding file was not found)",
                        className);
                return super.loadClass(className, resolve);
            }
        } catch (IOException e) {
            logger.error(className + ": An IOException was thrown", e);
            // TODO some better handling
            throw new RuntimeException(e);
        }
    }

    public Class<?> tryLoadClass(String name) {
        try {
            return loadClass(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Finds the corresponding binary file for the specified class.
     *
     * @param className
     *            the name of the class
     * @return the binary file or null if it was not found
     */
    public File findBinaryFile(String className) {
        Validate.notBlank(className, "The class name must not be blank");

        // iterate binary directories in order
        for (File dir : binaryDirectories) {
            File file = new File(dir, JavaFileUtils.classNameToClassFilename(className));

            if (file.exists()) {
                // found
                return file;
            }
        }

        // not found
        return null;
    }

    /**
     * Gets the bytes of the corresponding binary file for the specified class.
     *
     * @param className
     *            the name of the class
     * @return the bytes in the corresponding binary file or null if the file was not found
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public byte[] readBytes(String className) throws IOException {
        Validate.notBlank(className, "The class name must not be blank");

        File file = findBinaryFile(className);

        if (file != null) {
            return FileUtils.readFileToByteArray(file);
        } else {
            return null;
        }
    }
}