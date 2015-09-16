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
package hu.bme.mit.sette.common.validator.exceptions;

import hu.bme.mit.sette.common.exceptions.SetteException;

/**
 * Base class for validation exceptions.
 */
public abstract class ValidationException extends SetteException {
    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 2262806066760596612L;

    /**
     * Instantiates a new validation exception.
     *
     * @param message
     *            the message
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * Instantiates a new validation exception.
     *
     * @param message
     *            the message
     * @param cause
     *            the cause
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
