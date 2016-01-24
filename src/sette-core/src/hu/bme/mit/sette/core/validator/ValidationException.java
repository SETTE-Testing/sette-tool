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
package hu.bme.mit.sette.core.validator;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import hu.bme.mit.sette.core.SetteException;
import lombok.NonNull;

/**
 * Exception class for validation failures. The exception message will be usually multi-line,
 * containing all the errors for the validator or for the validation context.
 */
public final class ValidationException extends SetteException {
    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -17608370218683000L;

    /**
     * Instantiates a new validation exception.
     *
     * @param validator
     *            the validator (must not be <code>null</code>)
     * @throws IllegalArgumentException
     *             if the validator contains no errors
     */
    public ValidationException(Validator<?> validator) {
        super(String.join("\n", createMessage(validator)));
    }

    /**
     * 
     * Instantiates a new validation exception.
     *
     * @param validationContext
     *            the validation (must not be <code>null</code>)
     * @throws IllegalArgumentException
     *             if the validation context contains no errors
     */
    public ValidationException(ValidationContext validationContext) {
        super(String.join("\n", createMessage(validationContext)));
    }

    /**
     * Creates an exception message for a validator.
     * 
     * @param validator
     *            the validator
     * @return list of the lines of the exception message
     * @throws IllegalArgumentException
     *             if the validator contains no errors
     */
    private static List<String> createMessage(@NonNull Validator<?> validator) {
        checkArgument(!validator.isValid(),
                "Cannot create exception for validator with no errors");

        List<String> msg = new LinkedList<>();
        msg.add(String.valueOf(validator.getSubject()));
        validator.getErrors().stream().forEach(error -> {
            Splitter.on('\n').splitToList(error.getMessage()).forEach(line -> {
                msg.add("    " + line);
            });
        });

        return msg;
    }

    /**
     * Creates an exception message for a validation context.
     * 
     * @param validationContext
     *            the validation context
     * @return list of the lines of the exception message
     * @throws IllegalArgumentException
     *             if the validation context contains no errors
     */
    private static List<String> createMessage(@NonNull ValidationContext validationContext) {
        checkArgument(!validationContext.isValid(),
                "Cannot create exception for validation context with no errors");

        List<String> msg = new LinkedList<>();
        msg.add(String.valueOf(validationContext.getContext()));

        validationContext.getValidators()
                .stream()
                .filter(v -> !v.isValid())
                .forEach(v -> {
                    List<String> vMsg = createMessage(v);

                    // indent other lines
                    vMsg.stream().forEach(error -> msg.add("    " + error));
                });

        msg.add(Strings.repeat("=", 40));

        return msg;
    }
}
