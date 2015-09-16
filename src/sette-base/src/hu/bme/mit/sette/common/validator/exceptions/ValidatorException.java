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
import hu.bme.mit.sette.common.validator.AbstractValidator;

/**
 * Exception class for the validators.
 */
public final class ValidatorException extends SetteException {
    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 3996094745780644700L;

    /** The validator. */
    private final AbstractValidator<?> validator;

    /**
     * Instantiates a new validator exception.
     *
     * @param validator
     *            the validator
     */
    public ValidatorException(AbstractValidator<?> validator) {
        super(String.format("%d exception(s) occured.", validator.getExceptionCount()));
        this.validator = validator;
    }

    /**
     * Gets the full message.
     *
     * @return the full message
     */
    public String getFullMessage() {
        StringBuilder sb = new StringBuilder();

        for (AbstractValidator<?> v : validator.getAllValidators()) {
            if (!v.getExceptions().isEmpty()) {
                sb.append("Subject:   [").append(v.getSubject()).append("]\n");
                sb.append("Validator: [").append(v).append("]\n");
                for (ValidationException e : v.getExceptions()) {
                    sb.append("  ").append(e.getMessage()).append('\n');
                }
            }
        }

        return sb.toString();
    }

    /**
     * Gets the validator.
     *
     * @return the validator
     */
    public AbstractValidator<?> getValidator() {
        return validator;
    }
}
