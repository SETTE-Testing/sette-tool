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
package hu.bme.mit.sette.common.validator.exceptions.reflection;

import hu.bme.mit.sette.common.validator.exceptions.ValidationException;

/**
 * Exception class for Java class validation errors.
 */
public final class ClassValidationException extends ValidationException {
    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -6894977336562822740L;

    /** The Java class. */
    private final Class<?> javaClass;

    /**
     * Instantiates a new class validation exception.
     *
     * @param message
     *            the message
     * @param javaClass
     *            the java class
     */
    public ClassValidationException(String message, Class<?> javaClass) {
        this(message, javaClass, null);
    }

    /**
     * Instantiates a new class validation exception.
     *
     * @param message
     *            the message
     * @param javaClass
     *            the java class
     * @param cause
     *            the cause
     */
    public ClassValidationException(String message, Class<?> javaClass, Throwable cause) {
        super(message + String.format("\n(javaClass: [%s])", javaClass), cause);
        this.javaClass = javaClass;
    }

    /**
     * Gets the Java class.
     *
     * @return the Java class
     */
    public Class<?> getJavaClass() {
        return javaClass;
    }
}
