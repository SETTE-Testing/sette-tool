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
package hu.bme.mit.sette.common.validator.exceptions;

/**
 * Exception class for validation errors when the argument is null but it must not be.
 */
public final class NullValidationException extends ValidationException {
    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1645818843033308696L;

    /** The subject. */
    private final Object subject;

    /**
     * Instantiates a new null validation exception.
     *
     * @param subject
     *            the subject
     */
    public NullValidationException(Object subject) {
        this(subject, null);
    }

    /**
     * Instantiates a new null validation exception.
     *
     * @param subject
     *            the subject
     * @param cause
     *            the cause
     */
    public NullValidationException(Object subject, Throwable cause) {
        this(String.format("The subject must be null\n(subject: [%s])", subject), subject, cause);
    }

    /**
     * Instantiates a new null validation exception.
     *
     * @param message
     *            the message
     * @param subject
     *            the subject
     */
    public NullValidationException(String message, Object subject) {
        this(message, subject, null);
    }

    /**
     * Instantiates a new null validation exception.
     *
     * @param message
     *            the message
     * @param subject
     *            the subject
     * @param cause
     *            the cause
     */
    public NullValidationException(String message, Object subject, Throwable cause) {
        super(message, cause);
        this.subject = subject;
    }

    /**
     * Gets the subject.
     *
     * @return the subject
     */
    public Object getSubject() {
        return subject;
    }
}
