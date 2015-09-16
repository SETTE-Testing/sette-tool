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
package hu.bme.mit.sette.common.model.runner;

import java.util.Arrays;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.Validate;

/**
 * Represents the type of a parameter.
 */
// TODO make it abstract class (ParameterType<T>), abstract method
// validateValue(),
// T getValue(), void setValue(T), String getValueAsString() etc..
public enum ParameterType {
    /** Java primitive byte. */
    BYTE("byte"),
    /** Java primitive short. */
    SHORT("short"),
    /** Java primitive int. */
    INT("int"),
    /** Java primitive long. */
    LONG("long"),
    /** Java primitive float. */
    FLOAT("float"),
    /** Java primitive double. */
    DOUBLE("double"),
    /** Java primitive boolean. */
    BOOLEAN("boolean"),
    /** Java primitive char. */
    CHAR("char"),
    /** Java expression (one statement). */
    EXPRESSION("expression");
    // TODO it is really needed after heap was added? heap+expression should be
    // enough
    // /** Factory (Java code). */
    // FACTORY("factory");

    /** The string representation. */
    private final String toString;

    /**
     * Instantiates a new parameter type.
     *
     * @param toString
     *            the string representation
     */
    private ParameterType(String toString) {
        this.toString = toString;
    }

    /**
     * Parses the given string into a {@link ParameterType}.
     *
     * @param string
     *            the string
     * @return the parameter type
     */
    public static ParameterType fromString(String string) {
        Validate.notBlank(string, "The string must not be blank");

        for (ParameterType pt : ParameterType.values()) {
            if (pt.toString.equalsIgnoreCase(string) || pt.name().equalsIgnoreCase(string)) {
                return pt;
            }
        }

        String message = String.format("Invalid string (string: [%s], valid strings: [%s]", string,
                Arrays.toString(ParameterType.values()));
        throw new IllegalArgumentException(message);
    }

    public static ParameterType primitiveFromJavaClass(Class<?> javaClass) {
        Validate.notNull(javaClass, "The Java class must not be null");
        Validate.isTrue(ClassUtils.isPrimitiveOrWrapper(javaClass),
                "The represented type is not primitive [javaClass: %s]", javaClass.getName());

        Class<?> primitiveClass;
        if (javaClass.isPrimitive()) {
            primitiveClass = javaClass;
        } else {
            primitiveClass = ClassUtils.wrapperToPrimitive(javaClass);
        }

        Validate.isTrue(primitiveClass != void.class,
                "the parameter type must not be void [javaClass: %s]", javaClass.getName());

        return fromString(primitiveClass.getCanonicalName());
    }

    @Override
    public String toString() {
        return toString;
    }
}
