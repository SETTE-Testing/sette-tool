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
package hu.bme.mit.sette.common.util.reflection;

import java.util.Comparator;

/**
 * Helper class for reflection.
 */
public final class ReflectionUtils {
    /** Static class. */
    private ReflectionUtils() {
        throw new UnsupportedOperationException("Static class");
    }

    static {
        CLASS_COMPARATOR = new Comparator<Class<?>>() {
            @Override
            public int compare(final Class<?> o1, final Class<?> o2) {
                if (o1 == null) {
                    return -1;
                } else if (o2 == null) {
                    return 1;
                } else {
                    return o1.getName().compareTo(o2.getName());
                }
            }
        };
    }

    /** A {@link Comparator} for {@link Class}es. */
    public static final Comparator<Class<?>> CLASS_COMPARATOR;

    // TODO decide whether it is needed
    // /**
    // * Loads a Java class.
    // *
    // * @param packageName
    // * the package name
    // * @param className
    // * the class name
    // * @return the class
    // * @throws ClassNotFoundException
    // * if the class cannot be located
    // */
    // public static Class<?> loadClass(final String packageName,
    // final String className) throws ClassNotFoundException {
    // Validate.notBlank(packageName, "Package name must not be blank");
    // Validate.notBlank(className, "Class name must not be blank");
    //
    // return Class.forName(packageName
    // + JavaFileUtils.PACKAGE_SEPARATOR + className);
    // }
    //
    // /**
    // * Loads a Java class.
    // *
    // * @param className
    // * the class name
    // * @return the class
    // * @throws ClassNotFoundException
    // * if the class cannot be located
    // */
    // public static Class<?> loadClass(final String className)
    // throws ClassNotFoundException {
    // Validate.notBlank(className, "Class name must not be blank");
    // return Class.forName(className);
    // }
}
