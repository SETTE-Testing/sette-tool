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
package hu.bme.mit.sette.core.validator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Predicate;

import lombok.NonNull;

/**
 * Enum for path types.
 */
public enum PathType {
    /** Nonexistent path. */
    NONEXISTENT(path -> !Files.exists(path)),
    /** Directory. */
    DIRECTORY(Files::isDirectory),
    /** Regular file. */
    REGULAR_FILE(Files::isRegularFile);

    /** Predicate which decides whether the given path has the corresponding type. */
    private final transient Predicate<Path> predicate;

    /**
     * Creates a new enum value.
     * 
     * @param predicate
     *            predicate which decides whether the given path has the corresponding type
     */
    PathType(Predicate<Path> predicate) {
        this.predicate = predicate;
    }

    /**
     * Tests a path against the path type.
     * 
     * @param path
     *            a path
     * @return <code>true</code> if the path has the type of the enum value
     */
    public boolean test(@NonNull Path path) {
        return predicate.test(path);
    }

    /**
     * Determines the type of a path
     * 
     * @param path
     *            the path
     * @return the type of the path
     */
    public static PathType forPath(@NonNull Path path) {
        return Arrays.stream(PathType.values()).filter(t -> t.test(path)).findAny().get();
    }
}
