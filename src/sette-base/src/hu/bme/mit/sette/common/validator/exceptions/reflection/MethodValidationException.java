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

import java.lang.reflect.Method;

/**
 * Exception class for method validation errors.
 */
public final class MethodValidationException extends ValidationException {
    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -5763118155937025906L;

    /** The method. */
    private final Method method;

    /**
     * Instantiates a new method validation exception.
     *
     * @param message
     *            the message
     * @param method
     *            the method
     */
    public MethodValidationException(String message, Method method) {
        this(message, method, null);
    }

    /**
     * Instantiates a new method validation exception.
     *
     * @param message
     *            the message
     * @param method
     *            the method
     * @param cause
     *            the cause
     */
    public MethodValidationException(String message, Method method, Throwable cause) {
        super(message + String.format("\n(method: [%s])", method), cause);
        this.method = method;
    }

    /**
     * Gets the method.
     *
     * @return the method
     */
    public Method getMethod() {
        return method;
    }
}
