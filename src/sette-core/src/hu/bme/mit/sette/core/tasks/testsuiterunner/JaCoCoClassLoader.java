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
package hu.bme.mit.sette.core.tasks.testsuiterunner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.commons.lang3.Validate;
import org.jacoco.core.instr.Instrumenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hu.bme.mit.sette.core.util.io.PathUtils;
import lombok.NonNull;

/**
 * A class loader which loads and instruments classes for JaCoCo.
 */
public final class JaCoCoClassLoader extends ClassLoader {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Path[] binaryDirectories;
    private final Instrumenter instrumenter;

    public JaCoCoClassLoader(@NonNull Path[] binaryDirectories, @NonNull Instrumenter instrumenter,
            @NonNull ClassLoader parent) {
        super(parent);

        Validate.notEmpty(binaryDirectories,
                "The array of binary directories must not be empty or null");
        Validate.noNullElements(binaryDirectories,
                "The array of binary directories must not contain null elements");
        Validate.notNull(instrumenter, "The instrumenter must not be null");

        this.binaryDirectories = Arrays.copyOf(binaryDirectories, binaryDirectories.length);
        this.instrumenter = instrumenter;

        log.debug("JaCoCoClassLoader has been created");
    }

    @Override
    protected Class<?> loadClass(@NonNull String className, boolean resolve)
            throws ClassNotFoundException {
        Class<?> javaClass = findLoadedClass(className);

        if (javaClass != null) {
            // class was already loaded (and instrumented if bytecode was found)
            log.debug("{}: the class was already loaded", className);
            return javaClass;
        }

        // first try to load from one of the binary directories and
        // instrument the class
        byte[] bytes = readBytes(className);

        if (bytes != null) {
            log.debug("{}: instrumenting and defining class", className);

            // instrument
            byte[] instrumentedBytes;
            try {
                instrumentedBytes = instrumenter.instrument(bytes, className);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
            log.debug("{}: instrumented class", className);

            // define class
            Class<?> cls = defineClass(className, instrumentedBytes, 0,
                    instrumentedBytes.length);
            log.debug("{}: defined class", className);
            return cls;
        } else {
            // was not found, try to load with the parent, but it will
            // not be instrumented
            log.debug("{}: calling super.loadClass() (corresponding file was not found)",
                    className);
            return super.loadClass(className, resolve);
        }
    }

    public Class<?> tryLoadClass(String name) {
        try {
            return loadClass(name);
        } catch (ClassNotFoundException ex) {
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
    public Path findBinaryFile(String className) {
        Validate.notBlank(className, "The class name must not be blank");

        // iterate binary directories in order
        for (Path dir : binaryDirectories) {
            Path file = dir.resolve(className.replace('.', '/') + ".class");

            if (PathUtils.exists(file)) {
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
     */
    public byte[] readBytes(String className) {
        Validate.notBlank(className, "The class name must not be blank");

        Path file = findBinaryFile(className);

        if (file != null) {
            return PathUtils.readAllBytes(file);
        } else {
            return null;
        }
    }
}
